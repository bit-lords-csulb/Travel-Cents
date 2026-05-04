from firebase_functions import https_fn
from firebase_admin import initialize_app
import difflib
import os
import re
import requests
import json
import unicodedata
from groq import Groq
from pinecone import Pinecone
from dotenv import load_dotenv

# Initialize Firebase and load environment variables
initialize_app()
load_dotenv()

CITY_ALIASES = {
    "london": "London",
    "london uk": "London",
    "london united kingdom": "London",
    "london england": "London",
    "greater london": "London",
    "lhr": "London",
    "lgw": "London",
    "paris": "Paris",
    "paris france": "Paris",
    "cdg": "Paris",
    "ory": "Paris",
    "amsterdam": "Amsterdam",
    "amsterdam netherlands": "Amsterdam",
    "amsterdam holland": "Amsterdam",
    "ams": "Amsterdam",
    "new york": "New York",
    "new york city": "New York",
    "new york usa": "New York",
    "new york united states": "New York",
    "new york ny": "New York",
    "nyc": "New York",
    "jfk": "New York",
    "lga": "New York",
    "ewr": "New York",
    "miami": "Miami",
    "miami usa": "Miami",
    "miami united states": "Miami",
    "miami fl": "Miami",
    "miami florida": "Miami",
    "mia": "Miami",
    "cairo": "Cairo",
    "cairo egypt": "Cairo",
    "cai": "Cairo",
    "sydney": "Sydney",
    "sydney australia": "Sydney",
    "syd": "Sydney",
    "tokyo": "Tokyo",
    "tokyo japan": "Tokyo",
    "tokyo jp": "Tokyo",
    "tyo": "Tokyo",
    "hnd": "Tokyo",
    "nrt": "Tokyo",
}

MATCH_SCORE_THRESHOLD = 0.5
PINECONE_OPTION_COUNT = 6
PINECONE_INDEX_NAME = "travel-cents-inventory"
HF_EMBEDDING_URL = "https://router.huggingface.co/hf-inference/models/BAAI/bge-base-en-v1.5/pipeline/feature-extraction"
ADVISORY_MATCH_SCORE_THRESHOLD = 0.35
VIATOR_PRODUCT_DETAIL_URL = "https://api.viator.com/partner/products/{product_code}"
WEATHER_SAFE_ACTIVITY_QUERIES = {
    "rain": "rain safe indoor covered museum gallery theater aquarium market activity in {city}",
    "wind": "wind safe indoor protected museum gallery theater aquarium market activity in {city}",
    "heat": "air-conditioned indoor museum gallery theater aquarium market activity in {city}",
}
WEATHER_SAFE_TERMS = (
    "indoor",
    "covered",
    "museum",
    "gallery",
    "theater",
    "theatre",
    "aquarium",
    "market",
    "food hall",
    "mall",
    "arcade",
    "studio",
    "workshop",
    "library",
    "exhibition",
    "opera house",
)
WEATHER_UNSAFE_TERMS = (
    "kayak",
    "kayaking",
    "paddle",
    "boat",
    "boating",
    "sailing",
    "cruise",
    "ferry",
    "surf",
    "snorkel",
    "swim",
    "beach",
    "hike",
    "hiking",
    "walking tour",
    "bike tour",
    "bicycle",
    "outdoor",
    "rooftop",
    "bridge climb",
    "harbour tour",
    "harbor tour",
)


def normalize_string_list(value):
    if isinstance(value, list):
        return [str(item) for item in value if str(item or "").strip()]
    if isinstance(value, str) and value.strip():
        return [value.strip()]
    return []


def extract_viator_image_urls(product):
    images = product.get("images") or []
    if not isinstance(images, list):
        return []

    ordered_images = sorted(
        images,
        key=lambda image: 0 if image.get("isCover") else 1
    )

    urls = []
    for image in ordered_images:
        variants = image.get("variants") or []
        if not isinstance(variants, list):
            continue

        sorted_variants = sorted(
            variants,
            key=lambda variant: (variant.get("width") or 0) * (variant.get("height") or 0),
            reverse=True
        )
        for variant in sorted_variants:
            url = variant.get("url")
            if url and url not in urls:
                urls.append(url)

    return urls


def fetch_viator_product_images(product_code, api_key):
    if not product_code or not api_key:
        return []

    headers = {
        "exp-api-key": api_key,
        "Accept-Language": "en-US",
        "Accept": "application/json;version=2.0"
    }

    try:
        response = requests.get(
            VIATOR_PRODUCT_DETAIL_URL.format(product_code=product_code),
            headers=headers,
            timeout=8
        )
        if response.status_code != 200:
            print(f"Viator image lookup failed for {product_code}: {response.status_code}", flush=True)
            return []
        return extract_viator_image_urls(response.json())
    except Exception as error:
        print(f"Viator image lookup failed for {product_code}: {error}", flush=True)
        return []


def hydrate_option_images(option, viator_key):
    if option.get("image_url") or option.get("photo_urls"):
        return option

    image_urls = fetch_viator_product_images(option.get("activity_id"), viator_key)
    if image_urls:
        option["image_url"] = image_urls[0]
        option["photo_urls"] = image_urls[:5]
    return option


def normalize_city_key(value):
    normalized = unicodedata.normalize("NFKD", str(value or ""))
    ascii_text = normalized.encode("ascii", "ignore").decode("ascii")
    key = re.sub(r"[^a-z0-9]+", " ", ascii_text.lower()).strip()
    return re.sub(r"\s+", " ", key)


def fallback_city_name(value):
    first_segment = re.split(r"[,/|(-]", str(value or ""))[0].strip()
    return first_segment.title() if first_segment else "London"


def resolve_inventory_city(destination):
    destination_key = normalize_city_key(destination)
    if not destination_key:
        return "London"

    if destination_key in CITY_ALIASES:
        return CITY_ALIASES[destination_key]

    first_segment_key = normalize_city_key(fallback_city_name(destination))
    if first_segment_key in CITY_ALIASES:
        return CITY_ALIASES[first_segment_key]

    padded_key = f" {destination_key} "
    for alias_key, canonical_city in sorted(CITY_ALIASES.items(), key=lambda item: len(item[0]), reverse=True):
        if f" {alias_key} " in padded_key:
            return canonical_city

    close_matches = difflib.get_close_matches(first_segment_key, CITY_ALIASES.keys(), n=1, cutoff=0.86)
    if close_matches:
        return CITY_ALIASES[close_matches[0]]

    return fallback_city_name(destination)


def match_field(match, key, default=None):
    if isinstance(match, dict):
        return match.get(key, default)
    return getattr(match, key, default)


def match_to_activity_option(match):
    score = match_field(match, "score", 0.0) or 0.0
    metadata = match_field(match, "metadata", {}) or {}
    activity_id = str(metadata.get("activity_id") or "")
    title = metadata.get("title") or ""
    booking_url = metadata.get("booking_url") or ""
    image_url = metadata.get("image_url") or metadata.get("imageUrl") or ""
    photo_urls = normalize_string_list(metadata.get("photo_urls") or metadata.get("photoUrls"))
    if image_url and image_url not in photo_urls:
        photo_urls.insert(0, image_url)
    option_id = match_field(match, "id", "") or (
        f"{metadata.get('city', '')}::{activity_id}" if activity_id else title
    )

    return {
        "option_id": option_id,
        "activity_id": activity_id,
        "title": title,
        "real_title": title,
        "booking_url": booking_url,
        "image_url": image_url,
        "photo_urls": photo_urls[:5],
        "isNativeBookable": "true" if booking_url else "false",
        "score": score
    }


def callable_payload(req):
    req_data = req.data or {}
    if not isinstance(req_data, dict):
        req_data = {"destination": str(req_data)}
    if isinstance(req_data.get("data"), dict):
        req_data = req_data["data"]
    return req_data


def coerce_embedding_vector(value):
    if not isinstance(value, list) or not value:
        return value
    if all(isinstance(item, (int, float)) for item in value):
        return value
    rows = [row for row in value if isinstance(row, list) and row]
    if not rows:
        return value
    width = len(rows[0])
    if width == 0:
        return value
    averaged = []
    for index in range(width):
        column = [row[index] for row in rows if len(row) > index and isinstance(row[index], (int, float))]
        averaged.append(sum(column) / len(column) if column else 0.0)
    return averaged


def metadata_text(metadata, *keys):
    for key in keys:
        value = metadata.get(key)
        if isinstance(value, list):
            value = ", ".join(str(item) for item in value if item is not None)
        if value is not None and str(value).strip():
            return str(value).strip()
    return ""


def advisory_reason_key(reason):
    key = normalize_city_key(reason)
    if "rain" in key:
        return "rain"
    if "wind" in key:
        return "wind"
    if "heat" in key:
        return "heat"
    return ""


def normalized_contains_any(text, terms):
    normalized_text = normalize_city_key(text)
    return any(normalize_city_key(term) in normalized_text for term in terms)


def advisory_match_text(metadata):
    fields = (
        "title",
        "name",
        "activity_name",
        "description",
        "summary",
        "categories",
        "category",
        "activity_environment",
        "environment",
        "weather_sensitivity",
    )
    return " ".join(metadata_text(metadata, field) for field in fields)


def advisory_alternative_query(req_data, target_city):
    reason_key = advisory_reason_key(req_data.get("reason", ""))
    safe_query = WEATHER_SAFE_ACTIVITY_QUERIES.get(reason_key)
    if safe_query:
        return safe_query.format(city=target_city)

    explicit_query = req_data.get("query")
    if explicit_query:
        return str(explicit_query)

    return f"nearby indoor activity in {target_city}"


def is_weather_safe_advisory_match(match, reason):
    reason_key = advisory_reason_key(reason)
    if reason_key not in WEATHER_SAFE_ACTIVITY_QUERIES:
        return True

    metadata = match_field(match, "metadata", {}) or {}
    text = advisory_match_text(metadata)
    environment = normalize_city_key(metadata_text(metadata, "activity_environment", "environment"))
    sensitivity = normalize_city_key(metadata_text(metadata, "weather_sensitivity"))

    if normalized_contains_any(text, WEATHER_UNSAFE_TERMS):
        return False
    if environment in ("indoor", "covered") or sensitivity in ("none", "low"):
        return True
    if normalized_contains_any(text, WEATHER_SAFE_TERMS):
        return True
    if environment in ("outdoor", "mixed") or sensitivity in ("rain", "wind", "heat"):
        return False
    return False


def match_to_advisory_alternative(match, reason):
    metadata = match_field(match, "metadata", {}) or {}
    title = metadata_text(metadata, "title", "name", "activity_name")
    description = metadata_text(metadata, "description", "summary", "categories", "category")
    address = metadata_text(metadata, "address", "formatted_address", "location")
    fallback_environment = "indoor" if "rain" in str(reason).lower() else "mixed"
    return {
        "source_id": str(match_field(match, "id", "")),
        "score": match_field(match, "score", 0.0),
        "title": title,
        "description": description,
        "booking_url": metadata_text(metadata, "booking_url", "url", "website"),
        "address": address,
        "city": metadata_text(metadata, "city"),
        "price_tier": metadata_text(metadata, "price_tier", "price", "price_level"),
        "latitude": metadata_text(metadata, "latitude", "lat"),
        "longitude": metadata_text(metadata, "longitude", "lng", "lon", "longitude"),
        "activity_environment": metadata_text(metadata, "activity_environment", "environment") or fallback_environment,
        "weather_sensitivity": metadata_text(metadata, "weather_sensitivity") or "none",
        "environment_confidence": metadata_text(metadata, "environment_confidence") or "medium",
    }


@https_fn.on_call()
def generate_itinerary(req: https_fn.CallableRequest):
    # 1. Extract all the travel details sent from the Android app's TravelRequest model
    req_data = callable_payload(req)

    destination = req_data.get("destination") or req_data.get("city") or "London"
    target_city = resolve_inventory_city(destination)
    if normalize_city_key(destination) != normalize_city_key(target_city):
        print(f"Normalized destination '{destination}' to Pinecone city '{target_city}'.", flush=True)

    date_from = req_data.get("dateFrom", "Unknown")
    date_to = req_data.get("dateTo", "Unknown")
    adults = req_data.get("adults", 1)
    children = req_data.get("children", 0)
    travel_style = req_data.get("travelStyle", "comfort")
    budget = req_data.get("budgetTotal", "Flexible")
    interests = req_data.get("interests", [])
    special_requests = req_data.get("specialRequests", "None")
    flight_arrival = req_data.get("flightArrival", "Unknown")
    activity_dates = req_data.get("activityDates", [])
    activity_window = req_data.get("activityWindow") or {}
    flights = req_data.get("flights") or []
    hotel = req_data.get("hotel") or {}

    # 2. Get API Keys
    groq_key = os.getenv("GROQ_API_KEY")
    hf_token = os.getenv("HF_TOKEN")
    pinecone_key = os.getenv("PINECONE_API_KEY")
    viator_key = os.getenv("VIATOR_API_KEY")

    client = Groq(api_key=groq_key)

    # Format the interests list into a readable string
    interests_str = ", ".join(interests) if interests else "general exploring"
    activity_dates_str = ", ".join(activity_dates) if activity_dates else f"{date_from} to {date_to}"
    activity_window_str = json.dumps(activity_window, ensure_ascii=True)
    flights_str = json.dumps(flights, ensure_ascii=True)
    hotel_str = json.dumps(hotel, ensure_ascii=True)

    # 3. Dynamic Prompting: Feed the user's exact parameters to Groq
    prompt = f"""
    Design a highly personalized, magical travel itinerary for {target_city}.

    Traveler Profile & Constraints:
    - Travel Dates: {date_from} to {date_to}
    - Party Size: {adults} adults, {children} children
    - Travel Style: {travel_style}
    - Estimated Budget: {budget}
    - Core Interests: {interests_str}
    - Special Requests: {special_requests}
    - Flight Arrival: {flight_arrival}
    - Allowed Activity Dates: {activity_dates_str}

    Scheduling Context:
    - Activity Window JSON: {activity_window_str}
    - Flights JSON: {flights_str}
    - Hotel JSON: {hotel_str}

    Scheduling Rules:
    - Never schedule an activity before the activityWindow minimumStartTime on the activityWindow startDate.
    - Never schedule an activity after the activityWindow maximumEndTime on the activityWindow endDate.
    - Use the hotel location as the daily planning anchor when hotel coordinates or city are available.
    - Account for hotel check-in/check-out timing when choosing first-day and last-day activities.
    - Return start_time and end_time for every activity in 24-hour HH:MM format.
    - For each activity, classify whether it is indoor, outdoor, mixed, or unknown.
    - For each activity, classify weather_sensitivity as rain, heat, wind, or none.
    - For each activity, set environment_confidence to high, medium, or low.

    Skip the generic tourist traps. Based strictly on the traveler profile above, focus on hidden gems, enchanting local experiences, and highly creative activities that fit their specific vibe, budget, and age group.

    Create a comprehensive list of activities to fill their specific travel dates.

    Return ONLY a JSON object with this exact structure:
    {{
      "itinerary": [
        {{
          "title": "Activity Name",
          "description": "A short, captivating description that highlights why this experience is magical and why it perfectly fits their specific interests.",
          "start_time": "10:00",
          "end_time": "12:00",
          "activity_environment": "outdoor",
          "weather_sensitivity": "rain",
          "environment_confidence": "high"
        }}
      ]
    }}
    """

    chat_completion = client.chat.completions.create(
        messages=[{"role": "user", "content": prompt}],
        model="llama-3.1-8b-instant",
        response_format={"type": "json_object"},
        temperature=0.85, # Pushed higher for a more imaginative, creative response
        top_p=0.9         # Wide enough to allow unique vocabulary, but grounded enough to stay coherent
    )

    raw_itinerary = json.loads(chat_completion.choices[0].message.content)

    # 4. Setup Pinecone
    pc = Pinecone(api_key=pinecone_key)
    index = pc.Index(PINECONE_INDEX_NAME)

    final_itinerary = []

    print(f"--- 🚀 STARTING MATCHMAKING FOR {target_city} ---", flush=True)

    for activity in raw_itinerary.get("itinerary", []):
        activity_title = activity.get('title') or activity.get('Activity Name') or "Unknown"
        activity_desc = activity.get('description', '')

        print(f"🔍 Analyzing: {activity_title}...", flush=True)

        # Hybrid search query
        search_query = f"{activity_title}: {activity_desc}"

        headers = {"Authorization": f"Bearer {hf_token}"}
        hf_payload = {"inputs": search_query}

        try:
            hf_response = requests.post(HF_EMBEDDING_URL, headers=headers, json=hf_payload)

            if hf_response.status_code == 200:
                vector = coerce_embedding_vector(hf_response.json())

                # Query Pinecone with the cleaned target_city
                search_result = index.query(
                    vector=vector,
                    top_k=PINECONE_OPTION_COUNT,
                    include_metadata=True,
                    filter={"city": {"$eq": target_city}}
                )

                matches = search_result['matches']
                if matches:
                    viable_options = [
                        match_to_activity_option(match)
                        for match in matches
                        if (match_field(match, "score", 0.0) or 0.0) > MATCH_SCORE_THRESHOLD
                    ]
                    match = matches[0]
                    score = match_field(match, "score", 0.0) or 0.0
                    best_match_meta = match_field(match, "metadata", {}) or {}

                    print(f"   ✅ Best Match: {best_match_meta.get('title')} (Score: {score:.4f})", flush=True)

                    if viable_options:
                        selected_option = viable_options[0]
                        hydrate_option_images(selected_option, viator_key)
                        activity['option_id'] = selected_option.get('option_id', "")
                        activity['activity_id'] = selected_option.get('activity_id', "")
                        activity['booking_url'] = selected_option.get('booking_url', "")
                        activity['image_url'] = selected_option.get('image_url', "")
                        activity['photo_urls'] = selected_option.get('photo_urls', [])
                        activity['real_title'] = selected_option.get('title', "")
                        activity['isNativeBookable'] = "true"
                        activity['options'] = [
                            {
                                **option,
                                "selected": option.get("option_id") == selected_option.get("option_id")
                            }
                            for option in viable_options
                        ]
                    else:
                        print(f"   ⚠️ Score too low ({score:.4f}).", flush=True)
                        activity['isNativeBookable'] = "false"
                        activity['options'] = []
                else:
                    print(f"   ❌ No database matches for '{activity_title}' in {target_city}.", flush=True)
                    activity['isNativeBookable'] = "false"
                    activity['options'] = []
            else:
                print(f"   ❌ HF Error: {hf_response.status_code}", flush=True)
                activity['isNativeBookable'] = "false"
                activity['options'] = []

        except Exception as e:
            print(f"   🔥 Error: {e}", flush=True)
            activity['isNativeBookable'] = "false"
            activity['options'] = []

        final_itinerary.append(activity)

    print("--- ✅ MATCHMAKING COMPLETE ---", flush=True)
    return {"itinerary": final_itinerary}


@https_fn.on_call()
def search_activity_alternatives(req: https_fn.CallableRequest):
    req_data = callable_payload(req)
    destination = req_data.get("destination") or req_data.get("city") or "London"
    target_city = resolve_inventory_city(destination)
    reason = req_data.get("reason", "")
    limit = req_data.get("limit", 9)
    try:
        limit = max(1, min(int(limit), 12))
    except (TypeError, ValueError):
        limit = 9

    hf_token = os.getenv("HF_TOKEN")
    pinecone_key = os.getenv("PINECONE_API_KEY")
    if not hf_token or not pinecone_key:
        print("Activity alternatives unavailable: missing HF_TOKEN or PINECONE_API_KEY.", flush=True)
        return {"alternatives": []}

    query = advisory_alternative_query(req_data, target_city)
    current_title_key = normalize_city_key(req_data.get("currentActivityTitle", ""))
    headers = {"Authorization": f"Bearer {hf_token}"}

    try:
        hf_response = requests.post(HF_EMBEDDING_URL, headers=headers, json={"inputs": query})
        if hf_response.status_code != 200:
            print(f"Activity alternatives embedding failed: {hf_response.status_code}", flush=True)
            return {"alternatives": []}

        vector = coerce_embedding_vector(hf_response.json())
        pc = Pinecone(api_key=pinecone_key)
        index = pc.Index(PINECONE_INDEX_NAME)
        search_result = index.query(
            vector=vector,
            top_k=max(limit * 4, 12),
            include_metadata=True,
            filter={"city": {"$eq": target_city}}
        )

        matches = search_result.get("matches", []) if hasattr(search_result, "get") else search_result["matches"]
        alternatives = []
        seen_titles = set()
        for match in matches:
            score = float(match_field(match, "score", 0.0) or 0.0)
            if score < ADVISORY_MATCH_SCORE_THRESHOLD:
                continue
            if not is_weather_safe_advisory_match(match, reason):
                continue
            alternative = match_to_advisory_alternative(match, reason)
            title_key = normalize_city_key(alternative.get("title", ""))
            if not title_key or title_key == current_title_key or title_key in seen_titles:
                continue
            seen_titles.add(title_key)
            alternatives.append(alternative)
            if len(alternatives) >= limit:
                break

        print(f"Found {len(alternatives)} advisory alternatives for {target_city}.", flush=True)
        return {"alternatives": alternatives}
    except Exception as e:
        print(f"Activity alternatives search failed: {e}", flush=True)
        return {"alternatives": []}
