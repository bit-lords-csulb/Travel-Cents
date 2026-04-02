#!/usr/bin/env python3
"""
Standalone trip-generation pipeline debugger for Task 3.8.

This script mirrors the current Android generation flow in:
  - NewTripViewModel.generateTrip()
  - GroqRepository.generateItinerary()
  - SerpRepository.searchFlights()/searchHotels()
  - YelpRepository.searchRestaurants()/searchActivities()/searchEvents()

What it does:
  1. Builds the same Groq itinerary prompt the app uses.
  2. Calls Groq, SerpAPI, and Yelp in the same stage order and parallel waves.
  3. Records each request and each response with an exact timestamp.
  4. Writes a chronological Markdown report plus a machine-readable JSON dump.

Notes:
  - The current Android pipeline uses Groq only for itinerary metadata.
    It does NOT call GroqRepository.generateEvents() during trip generation.
  - API keys are loaded from environment variables first, then local.properties.
  - Secrets are always redacted in the generated report.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import datetime as dt
import json
import os
import pathlib
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from dataclasses import asdict, dataclass, field
from typing import Any, Optional


ROOT = pathlib.Path(__file__).resolve().parent
REPORTS_DIR = ROOT / "debug_pipeline_runs"

GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
SERP_API_URL = "https://serpapi.com/search"
YELP_BUSINESS_SEARCH_URL = "https://api.yelp.com/v3/businesses/search"
YELP_EVENTS_URL = "https://api.yelp.com/v3/events"

GROQ_SYSTEM_PROMPT = (
    "You are a travel planner. Always respond with valid JSON only. "
    "No markdown, no extra text."
)

GROQ_MODEL = "llama-3.3-70b-versatile"
GROQ_TEMPERATURE = 0.7
GROQ_MAX_TOKENS = 4096
DEFAULT_HTTP_HEADERS = {
    "Accept": "application/json",
    "User-Agent": "okhttp/4.12.0",
}

DIETARY_ALIASES = {
    "Vegan": "vegan",
    "Vegetarian": "vegetarian",
    "Halal": "halal",
    "Kosher": "kosher",
    "Gluten-Free": "gluten_free",
}

METRO_AIRPORTS = {
    "JFK": "JFK,EWR,LGA",
    "EWR": "JFK,EWR,LGA",
    "LGA": "JFK,EWR,LGA",
    "LAX": "LAX,BUR,LGB,ONT,SNA",
    "BUR": "LAX,BUR,LGB,ONT,SNA",
    "SFO": "SFO,OAK,SJC",
    "OAK": "SFO,OAK,SJC",
    "SJC": "SFO,OAK,SJC",
    "ORD": "ORD,MDW",
    "MDW": "ORD,MDW",
    "DCA": "DCA,IAD,BWI",
    "IAD": "DCA,IAD,BWI",
    "BWI": "DCA,IAD,BWI",
    "MIA": "MIA,FLL,PBI",
    "FLL": "MIA,FLL,PBI",
    "BOS": "BOS,MHT,PVD",
    "SEA": "SEA,BFI",
    "DEN": "DEN",
    "ATL": "ATL",
    "DFW": "DFW,DAL",
    "DAL": "DFW,DAL",
    "HOU": "IAH,HOU",
    "IAH": "IAH,HOU",
    "PHX": "PHX",
    "LAS": "LAS",
    "MSP": "MSP",
    "DTW": "DTW",
    "CLT": "CLT",
    "PHL": "PHL",
    "LHR": "LHR,LGW,STN,LCY",
    "LGW": "LHR,LGW,STN,LCY",
    "CDG": "CDG,ORY",
    "ORY": "CDG,ORY",
    "NRT": "NRT,HND",
    "HND": "NRT,HND",
    "FRA": "FRA,HHN",
    "AMS": "AMS",
    "MAD": "MAD",
    "FCO": "FCO,CIA",
    "DXB": "DXB,AUH,SHJ",
    "SYD": "SYD",
    "YYZ": "YYZ,YTZ",
    "GRU": "GRU,GIG",
}


@dataclass
class TravelRequest:
    userId: str
    origin: str
    destination: str
    dateFrom: str
    dateTo: str
    adults: int
    children: int
    travelStyle: str
    currency: str
    budgetTotal: float
    dietary: list[str] = field(default_factory=list)
    interests: list[str] = field(default_factory=list)
    specialRequests: str = ""


@dataclass
class Itinerary:
    itineraryId: str
    userId: str
    tripName: str
    destination: str
    origin: str
    originIata: str
    destinationIata: str
    dateFrom: str
    dateTo: str
    durationDays: int
    currency: str
    travelStyle: str
    adults: int
    children: int
    createdAt: str
    status: str
    eventIds: list[str] = field(default_factory=list)


@dataclass
class TimelineEntry:
    seq: int
    timestamp: str
    monotonic_ms: float
    kind: str
    phase: str
    service: str
    label: str
    call_id: str
    data: dict[str, Any]


class TimelineRecorder:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._seq = 0
        self._entries: list[TimelineEntry] = []

    def add(
        self,
        *,
        kind: str,
        phase: str,
        service: str,
        label: str,
        call_id: Optional[str] = None,
        data: Optional[dict[str, Any]] = None,
    ) -> TimelineEntry:
        with self._lock:
            self._seq += 1
            entry = TimelineEntry(
                seq=self._seq,
                timestamp=utc_now_iso(),
                monotonic_ms=time.perf_counter() * 1000.0,
                kind=kind,
                phase=phase,
                service=service,
                label=label,
                call_id=call_id or "",
                data=data or {},
            )
            self._entries.append(entry)
            return entry

    def entries(self) -> list[TimelineEntry]:
        with self._lock:
            return list(self._entries)


class RemoteCallError(RuntimeError):
    def __init__(self, message: str, *, status_code: Optional[int] = None, body: Any = None) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.body = body


def utc_now_iso() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat(timespec="milliseconds")


def safe_json_dumps(payload: Any) -> str:
    return json.dumps(payload, indent=2, sort_keys=True)


def scrub_headers(headers: dict[str, str]) -> dict[str, str]:
    redacted = dict(headers)
    auth = redacted.get("Authorization")
    if auth:
        redacted["Authorization"] = "Bearer ***redacted***"
    return redacted


def scrub_query(params: Optional[dict[str, Any]]) -> dict[str, Any]:
    if not params:
        return {}
    redacted = dict(params)
    if "api_key" in redacted:
        redacted["api_key"] = "***redacted***"
    return redacted


def parse_local_properties(path: pathlib.Path) -> dict[str, str]:
    if not path.exists():
        return {}
    parsed: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        parsed[key.strip()] = value.strip()
    return parsed


def get_secret(name: str, local_properties: dict[str, str]) -> str:
    return os.environ.get(name, "").strip() or local_properties.get(name, "").strip()


def parse_csv_list(value: str) -> list[str]:
    return [item.strip() for item in value.split(",") if item.strip()]


def build_request_from_args(args: argparse.Namespace) -> TravelRequest:
    if args.request_file:
        payload = json.loads(pathlib.Path(args.request_file).read_text(encoding="utf-8"))
        return TravelRequest(
            userId=payload.get("userId", "debug-user"),
            origin=payload["origin"],
            destination=payload["destination"],
            dateFrom=payload["dateFrom"],
            dateTo=payload["dateTo"],
            adults=int(payload.get("adults", 1)),
            children=int(payload.get("children", 0)),
            travelStyle=payload.get("travelStyle", "comfort"),
            currency=payload.get("currency", "USD"),
            budgetTotal=float(payload.get("budgetTotal", 0.0)),
            dietary=list(payload.get("dietary", [])),
            interests=list(payload.get("interests", [])),
            specialRequests=payload.get("specialRequests", ""),
        )

    required = ["origin", "destination", "date_from", "date_to"]
    missing = [name for name in required if not getattr(args, name)]
    if missing:
        raise SystemExit(
            "Missing required arguments: "
            + ", ".join(f"--{name.replace('_', '-')}" for name in missing)
        )

    return TravelRequest(
        userId=args.user_id,
        origin=args.origin,
        destination=args.destination,
        dateFrom=args.date_from,
        dateTo=args.date_to,
        adults=args.adults,
        children=args.children,
        travelStyle=args.travel_style,
        currency=args.currency,
        budgetTotal=args.budget_total,
        dietary=parse_csv_list(args.dietary),
        interests=parse_csv_list(args.interests),
        specialRequests=args.special_requests,
    )


def duration_days_inclusive(date_from: str, date_to: str) -> int:
    start = dt.date.fromisoformat(date_from)
    end = dt.date.fromisoformat(date_to)
    return (end - start).days + 1


def generate_trip_dates(date_from: str, duration_days: int) -> list[str]:
    start = dt.date.fromisoformat(date_from)
    return [(start + dt.timedelta(days=offset)).isoformat() for offset in range(duration_days)]


def build_itinerary_prompt(request: TravelRequest) -> str:
    request_json = safe_json_dumps(asdict(request))
    return f"""A user submitted this travel request:
{request_json}

Return a single JSON object with these fields only:
{{
  "itinerary_id": "<uuid>",
  "user_id": "{request.userId}",
  "trip_name": "<short title>",
  "destination": "<city, country>",
  "origin": "<city, country>",
  "origin_iata": "<IATA airport code for origin city, e.g. LAX>",
  "destination_iata": "<IATA airport code for destination city, e.g. CDG>",
  "date_from": "<YYYY-MM-DD>",
  "date_to": "<YYYY-MM-DD>",
  "duration_days": <int>,
  "currency": "<ISO 4217>",
  "travel_style": "<budget|comfort|luxury>",
  "travelers": {{"adults": <int>, "children": <int>}},
  "created_at": "<ISO 8601 timestamp>",
  "status": "draft"
}}"""


def parse_groq_itinerary(raw_content: str, user_id: str) -> Itinerary:
    try:
        payload = json.loads(raw_content)
    except json.JSONDecodeError as exc:
        raise RemoteCallError("Groq response was not valid JSON", body=raw_content) from exc

    travelers = payload.get("travelers") or {}
    itinerary_id = str(payload.get("itinerary_id") or "").strip()
    if not itinerary_id or "uuid" in itinerary_id.lower():
        itinerary_id = str(uuid.uuid4())

    return Itinerary(
        itineraryId=itinerary_id,
        userId=user_id,
        tripName=str(payload.get("trip_name") or "My Trip"),
        destination=str(payload.get("destination") or ""),
        origin=str(payload.get("origin") or ""),
        originIata=str(payload.get("origin_iata") or "").upper(),
        destinationIata=str(payload.get("destination_iata") or "").upper(),
        dateFrom=str(payload.get("date_from") or ""),
        dateTo=str(payload.get("date_to") or ""),
        durationDays=int(payload.get("duration_days") or 0),
        currency=str(payload.get("currency") or "USD"),
        travelStyle=str(payload.get("travel_style") or "comfort"),
        adults=int(travelers.get("adults") or 1),
        children=int(travelers.get("children") or 0),
        createdAt=str(payload.get("created_at") or utc_now_iso()),
        status=str(payload.get("status") or "draft"),
        eventIds=[],
    )


def try_parse_json(raw_text: str) -> Any:
    raw_text = raw_text.strip()
    if not raw_text:
        return {}
    try:
        return json.loads(raw_text)
    except json.JSONDecodeError:
        return raw_text


def http_json_request(
    *,
    phase: str,
    service: str,
    label: str,
    method: str,
    url: str,
    recorder: TimelineRecorder,
    query: Optional[dict[str, Any]] = None,
    json_body: Optional[dict[str, Any]] = None,
    headers: Optional[dict[str, str]] = None,
    timeout_seconds: int = 60,
) -> Any:
    call_id = f"{service}:{label}:{uuid.uuid4().hex[:8]}"
    request_headers = dict(DEFAULT_HTTP_HEADERS)
    if headers:
        request_headers.update(headers)
    body_bytes: Optional[bytes] = None
    final_url = url

    if query:
        final_url = f"{url}?{urllib.parse.urlencode(query)}"
    if json_body is not None:
        body_bytes = safe_json_dumps(json_body).encode("utf-8")
        request_headers.setdefault("Content-Type", "application/json")

    recorder.add(
        kind="request",
        phase=phase,
        service=service,
        label=label,
        call_id=call_id,
        data={
            "method": method,
            "url": url,
            "query": scrub_query(query),
            "headers": scrub_headers(request_headers),
            "json_body": json_body,
        },
    )

    started = time.perf_counter()
    req = urllib.request.Request(
        url=final_url,
        data=body_bytes,
        headers=request_headers,
        method=method.upper(),
    )

    try:
        with urllib.request.urlopen(req, timeout=timeout_seconds) as response:
            raw_bytes = response.read()
            raw_text = raw_bytes.decode("utf-8", errors="replace")
            content_type = response.headers.get("Content-Type", "")
            parsed_body = try_parse_json(raw_text) if "json" in content_type.lower() else try_parse_json(raw_text)
            duration_ms = round((time.perf_counter() - started) * 1000.0, 1)
            recorder.add(
                kind="response",
                phase=phase,
                service=service,
                label=label,
                call_id=call_id,
                data={
                    "status_code": response.getcode(),
                    "duration_ms": duration_ms,
                    "headers": dict(response.headers.items()),
                    "body": parsed_body,
                },
            )
            return parsed_body
    except urllib.error.HTTPError as exc:
        raw_text = exc.read().decode("utf-8", errors="replace")
        parsed_body = try_parse_json(raw_text)
        duration_ms = round((time.perf_counter() - started) * 1000.0, 1)
        recorder.add(
            kind="response",
            phase=phase,
            service=service,
            label=label,
            call_id=call_id,
            data={
                "status_code": exc.code,
                "duration_ms": duration_ms,
                "headers": dict(exc.headers.items()),
                "body": parsed_body,
                "error": True,
            },
        )
        raise RemoteCallError(
            f"{service} {label} failed with HTTP {exc.code}",
            status_code=exc.code,
            body=parsed_body,
        ) from exc
    except urllib.error.URLError as exc:
        duration_ms = round((time.perf_counter() - started) * 1000.0, 1)
        recorder.add(
            kind="response",
            phase=phase,
            service=service,
            label=label,
            call_id=call_id,
            data={
                "status_code": None,
                "duration_ms": duration_ms,
                "body": {"error": str(exc)},
                "error": True,
            },
        )
        raise RemoteCallError(f"{service} {label} failed: {exc}") from exc


def infer_file_extension_from_url(url: str) -> str:
    path = urllib.parse.urlparse(url).path
    suffix = pathlib.Path(path).suffix.lower()
    if suffix and len(suffix) <= 5:
        return suffix
    return ".jpg"


def collect_hotel_image_urls(hotel_response: dict[str, Any]) -> list[dict[str, Any]]:
    properties = hotel_response.get("properties") or []
    collected: list[dict[str, Any]] = []
    seen: set[str] = set()

    for hotel_index, hotel in enumerate(properties):
        hotel_name = hotel.get("name") or f"hotel_{hotel_index + 1}"
        for image_index, image in enumerate(hotel.get("images") or []):
            for kind, key in (("thumbnail", "thumbnail"), ("original", "original_image")):
                url = image.get(key)
                if not url or url in seen:
                    continue
                seen.add(url)
                collected.append(
                    {
                        "hotel_index": hotel_index,
                        "hotel_name": hotel_name,
                        "image_index": image_index,
                        "kind": kind,
                        "url": url,
                    }
                )
    return collected


def download_hotel_images(
    hotel_response: dict[str, Any],
    output_dir: pathlib.Path,
    recorder: TimelineRecorder,
) -> dict[str, Any]:
    phase = "DOWNLOADING_HOTEL_IMAGES"
    image_dir = output_dir / "hotel_images"
    image_dir.mkdir(parents=True, exist_ok=True)

    image_urls = collect_hotel_image_urls(hotel_response)
    downloaded_files: list[str] = []
    failed_downloads: list[dict[str, str]] = []

    recorder.add(
        kind="info",
        phase=phase,
        service="pipeline",
        label="hotel_image_download_started",
        data={
            "attempted_image_count": len(image_urls),
            "folder": str(image_dir),
        },
    )

    for item in image_urls:
        extension = infer_file_extension_from_url(item["url"])
        file_name = (
            f"hotel_{item['hotel_index'] + 1:02d}_"
            f"image_{item['image_index'] + 1:02d}_"
            f"{item['kind']}{extension}"
        )
        file_path = image_dir / file_name
        try:
            req = urllib.request.Request(item["url"], headers={"User-Agent": "TravelCents-Debugger/1.0"})
            with urllib.request.urlopen(req, timeout=60) as response:
                file_path.write_bytes(response.read())
            downloaded_files.append(str(file_path))
        except Exception as exc:
            failed_downloads.append({"url": item["url"], "error": str(exc)})

    summary = {
        "attempted_count": len(image_urls),
        "downloaded_count": len(downloaded_files),
        "failed_count": len(failed_downloads),
        "folder": str(image_dir),
        "files": downloaded_files,
        "failed": failed_downloads,
    }
    recorder.add(
        kind="info",
        phase=phase,
        service="pipeline",
        label="hotel_image_download_completed",
        data=summary,
    )
    return summary


def call_groq_itinerary(
    request: TravelRequest,
    groq_api_key: str,
    recorder: TimelineRecorder,
) -> tuple[Itinerary, dict[str, Any], str]:
    phase = "CRAFTING_ITINERARY"
    prompt = build_itinerary_prompt(request)
    groq_request = {
        "model": GROQ_MODEL,
        "messages": [
            {"role": "system", "content": GROQ_SYSTEM_PROMPT},
            {"role": "user", "content": prompt},
        ],
        "temperature": GROQ_TEMPERATURE,
        "max_tokens": GROQ_MAX_TOKENS,
        "response_format": {"type": "json_object"},
    }
    response = http_json_request(
        phase=phase,
        service="groq",
        label="generate_itinerary",
        method="POST",
        url=GROQ_API_URL,
        recorder=recorder,
        json_body=groq_request,
        headers={"Authorization": f"Bearer {groq_api_key}"},
    )
    raw_content = (
        (((response or {}).get("choices") or [{}])[0].get("message") or {}).get("content", "")
        if isinstance(response, dict)
        else ""
    )
    itinerary = parse_groq_itinerary(str(raw_content).strip(), request.userId)
    recorder.add(
        kind="info",
        phase=phase,
        service="pipeline",
        label="itinerary_parsed",
        data={"itinerary": asdict(itinerary)},
    )
    return itinerary, response, prompt


def try_flight_search(
    *,
    origin_id: str,
    destination_id: str,
    request: TravelRequest,
    itinerary: Itinerary,
    stops: str,
    serp_api_key: str,
    recorder: TimelineRecorder,
    attempt_label: str,
) -> Optional[dict[str, Any]]:
    phase = "SEARCHING_FLIGHTS"
    params = {
        "engine": "google_flights",
        "departure_id": origin_id,
        "arrival_id": destination_id,
        "outbound_date": request.dateFrom,
        "return_date": request.dateTo,
        "adults": str(request.adults),
        "children": str(request.children),
        "currency": request.currency,
        "stops": stops,
        "type": "1",
        "api_key": serp_api_key,
    }
    try:
        response = http_json_request(
            phase=phase,
            service="serp",
            label=attempt_label,
            method="GET",
            url=SERP_API_URL,
            recorder=recorder,
            query=params,
        )
    except RemoteCallError:
        return None

    if not isinstance(response, dict):
        return None

    best_flights = response.get("best_flights") or []
    other_flights = response.get("other_flights") or []
    all_options = best_flights + other_flights
    if not all_options:
        recorder.add(
            kind="warning",
            phase=phase,
            service="serp",
            label=f"{attempt_label}_empty",
            data={
                "origin_id": origin_id,
                "destination_id": destination_id,
                "stops": stops,
                "message": "SerpAPI returned no flights for this attempt.",
            },
        )
        return None

    selected = best_flights[0] if best_flights else all_options[0]
    first_leg = (selected.get("flights") or [{}])[0]
    last_leg = (selected.get("flights") or [{}])[-1]
    selected_summary = {
        "source": "serp",
        "attempt_label": attempt_label,
        "origin_id": origin_id,
        "destination_id": destination_id,
        "stops": stops,
        "option_count": len(all_options),
        "best_flights_count": len(best_flights),
        "other_flights_count": len(other_flights),
        "price_level": ((response.get("price_insights") or {}).get("price_level")),
        "selected": {
            "price": selected.get("price"),
            "total_duration": selected.get("total_duration"),
            "airline_logo": selected.get("airline_logo"),
            "departure_date": str(first_leg.get("departure_time") or "").split(" ")[0],
            "departure_time": str(first_leg.get("departure_time") or "").split(" ")[-1] if first_leg.get("departure_time") else "",
            "arrival_time": str(last_leg.get("arrival_time") or "").split(" ")[-1] if last_leg.get("arrival_time") else "",
            "airline": first_leg.get("airline"),
            "flight_number": first_leg.get("flight_number"),
            "origin_airport": (first_leg.get("departure_airport") or {}).get("id"),
            "destination_airport": (last_leg.get("arrival_airport") or {}).get("id"),
            "legs": len(selected.get("flights") or []),
            "carbon_diff_percent": ((selected.get("carbon_emissions") or {}).get("difference_percent")),
        },
    }
    return {
        "response": response,
        "summary": selected_summary,
    }


def search_flights(
    request: TravelRequest,
    itinerary: Itinerary,
    serp_api_key: str,
    recorder: TimelineRecorder,
) -> dict[str, Any]:
    attempts = [
        (
            itinerary.originIata,
            itinerary.destinationIata,
            "2",
            "flight_attempt_1_exact_iata",
        ),
        (
            METRO_AIRPORTS.get(itinerary.originIata, itinerary.originIata),
            itinerary.destinationIata,
            "3",
            "flight_attempt_2_origin_metro",
        ),
        (
            METRO_AIRPORTS.get(itinerary.originIata, itinerary.originIata),
            METRO_AIRPORTS.get(itinerary.destinationIata, itinerary.destinationIata),
            "3",
            "flight_attempt_3_origin_and_destination_metro",
        ),
    ]

    for origin_id, destination_id, stops, label in attempts:
        if not origin_id or not destination_id:
            recorder.add(
                kind="warning",
                phase="SEARCHING_FLIGHTS",
                service="pipeline",
                label=f"{label}_skipped",
                data={
                    "message": "Skipped flight attempt because one or both IATA codes were blank.",
                    "origin_id": origin_id,
                    "destination_id": destination_id,
                },
            )
            continue
        result = try_flight_search(
            origin_id=origin_id,
            destination_id=destination_id,
            request=request,
            itinerary=itinerary,
            stops=stops,
            serp_api_key=serp_api_key,
            recorder=recorder,
            attempt_label=label,
        )
        if result is not None:
            return result

    placeholder = {
        "source": "serp",
        "placeholder": True,
        "selected": {
            "title": "No flights found",
            "origin_airport": itinerary.originIata,
            "destination_airport": itinerary.destinationIata,
            "booking_url": (
                "https://www.google.com/flights?q=flights+from+"
                f"{itinerary.originIata}+to+{itinerary.destinationIata}"
            ),
        },
    }
    recorder.add(
        kind="warning",
        phase="SEARCHING_FLIGHTS",
        service="pipeline",
        label="flight_placeholder",
        data=placeholder,
    )
    return {"response": {}, "summary": placeholder}


def search_hotels(
    request: TravelRequest,
    itinerary: Itinerary,
    serp_api_key: str,
    recorder: TimelineRecorder,
    max_price_per_night: float,
) -> dict[str, Any]:
    phase = "FINDING_HOTELS"
    params: dict[str, Any] = {
        "engine": "google_hotels",
        "q": f"Hotels in {itinerary.destination}",
        "check_in_date": request.dateFrom,
        "check_out_date": request.dateTo,
        "adults": str(request.adults),
        "children": str(request.children),
        "currency": request.currency,
        "sort_by": "8",
        "api_key": serp_api_key,
    }
    if max_price_per_night > 0:
        params["max_price"] = str(int(max_price_per_night))

    try:
        response = http_json_request(
            phase=phase,
            service="serp",
            label="hotel_search",
            method="GET",
            url=SERP_API_URL,
            recorder=recorder,
            query=params,
        )
    except RemoteCallError as exc:
        recorder.add(
            kind="warning",
            phase=phase,
            service="pipeline",
            label="hotel_placeholder",
            data={"message": str(exc)},
        )
        return {
            "response": {},
            "summary": {
                "source": "serp",
                "placeholder": True,
                "selected": {"hotel_name": "No hotels found"},
            },
        }

    properties = []
    if isinstance(response, dict):
        properties = response.get("properties") or []
    selected = properties[0] if properties else {}
    rooms_needed = max(1, request.adults // 2)
    rate = ((selected.get("rate_per_night") or {}).get("extracted_lowest")) if selected else None
    summary = {
        "source": "serp",
        "placeholder": not bool(properties),
        "option_count": len(properties),
        "rooms_needed": rooms_needed,
        "selected": {
            "hotel_name": selected.get("name"),
            "rating": selected.get("overall_rating"),
            "review_count": selected.get("reviews"),
            "hotel_class": selected.get("hotel_class"),
            "check_in_time": selected.get("check_in_time"),
            "check_out_time": selected.get("check_out_time"),
            "deal": selected.get("deal"),
            "rate_per_night": rate,
            "group_rate_per_night": (rate * rooms_needed) if isinstance(rate, (int, float)) else None,
            "rate_per_night_display": ((selected.get("rate_per_night") or {}).get("lowest")),
            "image_url": ((selected.get("images") or [{}])[0].get("thumbnail")) if selected.get("images") else None,
        },
    }
    return {"response": response, "summary": summary}


def build_yelp_categories(dietary: list[str]) -> str:
    aliases = [DIETARY_ALIASES[item] for item in dietary if item in DIETARY_ALIASES]
    if aliases:
        return "restaurants," + ",".join(aliases)
    return "restaurants"


def search_restaurant_for_day(
    location: str,
    date: str,
    dietary: list[str],
    yelp_api_key: str,
    recorder: TimelineRecorder,
) -> dict[str, Any]:
    phase = "FINDING_RESTAURANTS"
    params = {
        "location": location,
        "categories": build_yelp_categories(dietary),
        "limit": "5",
        "sort_by": "rating",
    }
    label = f"restaurants_{date}"
    try:
        response = http_json_request(
            phase=phase,
            service="yelp",
            label=label,
            method="GET",
            url=YELP_BUSINESS_SEARCH_URL,
            recorder=recorder,
            query=params,
            headers={"Authorization": f"Bearer {yelp_api_key}"},
        )
    except RemoteCallError as exc:
        recorder.add(
            kind="warning",
            phase=phase,
            service="pipeline",
            label=f"{label}_failed",
            data={"date": date, "message": str(exc)},
        )
        return {"date": date, "response": {}, "summary": None}

    businesses = response.get("businesses") if isinstance(response, dict) else []
    selected = businesses[0] if businesses else None
    summary = None
    if selected:
        summary = {
            "source": "yelp",
            "type": "restaurant",
            "date": date,
            "selected": {
                "id": selected.get("id"),
                "name": selected.get("name"),
                "price_tier": selected.get("price"),
                "rating": selected.get("rating"),
                "review_count": selected.get("review_count"),
                "address": ", ".join((selected.get("location") or {}).get("display_address") or []),
                "categories": [item.get("title") for item in selected.get("categories") or []],
                "image_url": selected.get("image_url"),
            },
        }
    return {"date": date, "response": response, "summary": summary}


def search_activity_for_day(
    location: str,
    date: str,
    yelp_api_key: str,
    recorder: TimelineRecorder,
) -> dict[str, Any]:
    phase = "FINDING_ACTIVITIES"
    params = {
        "location": location,
        "categories": "arts,museums,tours,landmarks",
        "limit": "5",
        "sort_by": "rating",
    }
    label = f"activities_{date}"
    try:
        response = http_json_request(
            phase=phase,
            service="yelp",
            label=label,
            method="GET",
            url=YELP_BUSINESS_SEARCH_URL,
            recorder=recorder,
            query=params,
            headers={"Authorization": f"Bearer {yelp_api_key}"},
        )
    except RemoteCallError as exc:
        recorder.add(
            kind="warning",
            phase=phase,
            service="pipeline",
            label=f"{label}_failed",
            data={"date": date, "message": str(exc)},
        )
        return {"date": date, "response": {}, "summary": None}

    businesses = response.get("businesses") if isinstance(response, dict) else []
    selected = businesses[0] if businesses else None
    summary = None
    if selected:
        summary = {
            "source": "yelp",
            "type": "activity",
            "date": date,
            "selected": {
                "id": selected.get("id"),
                "name": selected.get("name"),
                "price_tier": selected.get("price"),
                "rating": selected.get("rating"),
                "review_count": selected.get("review_count"),
                "address": ", ".join((selected.get("location") or {}).get("display_address") or []),
                "categories": [item.get("title") for item in selected.get("categories") or []],
                "image_url": selected.get("image_url"),
            },
        }
    return {"date": date, "response": response, "summary": summary}


def iso8601_to_epoch(date_string: str) -> int:
    date_value = dt.date.fromisoformat(date_string)
    return int(dt.datetime.combine(date_value, dt.time.min, tzinfo=dt.timezone.utc).timestamp())


def search_yelp_events(
    location: str,
    start_date: str,
    end_date: str,
    yelp_api_key: str,
    recorder: TimelineRecorder,
) -> dict[str, Any]:
    phase = "FINDING_ACTIVITIES"
    params = {
        "location": location,
        "limit": "20",
        "start_date": str(iso8601_to_epoch(start_date)),
        "end_date": str(iso8601_to_epoch(end_date)),
    }
    try:
        response = http_json_request(
            phase=phase,
            service="yelp",
            label="events_trip_range",
            method="GET",
            url=YELP_EVENTS_URL,
            recorder=recorder,
            query=params,
            headers={"Authorization": f"Bearer {yelp_api_key}"},
        )
    except RemoteCallError as exc:
        recorder.add(
            kind="warning",
            phase=phase,
            service="pipeline",
            label="events_trip_range_failed",
            data={"message": str(exc)},
        )
        return {"response": {}, "summary": []}

    events = response.get("events") if isinstance(response, dict) else []
    summary = []
    for item in events:
        summary.append(
            {
                "source": "yelp",
                "type": "activity",
                "date": str(item.get("time_start") or "").split("T")[0],
                "selected": {
                    "id": item.get("id"),
                    "name": item.get("name"),
                    "category": item.get("category"),
                    "is_free": item.get("is_free"),
                    "cost": item.get("cost"),
                    "cost_max": item.get("cost_max"),
                    "time_start": item.get("time_start"),
                    "time_end": item.get("time_end"),
                    "address": ", ".join((item.get("location") or {}).get("display_address") or []),
                },
            }
        )
    return {"response": response, "summary": summary}


def compute_diagnostics(
    request: TravelRequest,
    itinerary: Itinerary,
    flights: dict[str, Any],
    hotels: dict[str, Any],
    restaurants: list[dict[str, Any]],
    activities: list[dict[str, Any]],
    yelp_events: dict[str, Any],
) -> list[str]:
    diagnostics: list[str] = []

    selected_flight = (flights.get("summary") or {}).get("selected") or {}
    flight_departure_date = selected_flight.get("departure_date")
    if flight_departure_date and flight_departure_date != request.dateFrom:
        diagnostics.append(
            "Selected flight departure date does not match request.dateFrom "
            f"({flight_departure_date} vs {request.dateFrom})."
        )

    if not itinerary.originIata or not itinerary.destinationIata:
        diagnostics.append("Groq itinerary response is missing one or both IATA codes.")

    if itinerary.durationDays <= 0:
        diagnostics.append("Groq itinerary duration_days is zero or negative; Yelp daily calls will be skipped.")
    elif itinerary.durationDays != duration_days_inclusive(request.dateFrom, request.dateTo):
        diagnostics.append(
            "Groq itinerary duration_days does not match the inclusive date range "
            f"({itinerary.durationDays} vs {duration_days_inclusive(request.dateFrom, request.dateTo)})."
        )

    restaurant_ids = [item["summary"]["selected"]["id"] for item in restaurants if item.get("summary")]
    repeated_restaurants = sorted({item for item in restaurant_ids if restaurant_ids.count(item) > 1})
    if repeated_restaurants:
        diagnostics.append(
            "Repeated Yelp restaurant primary selections across days: " + ", ".join(repeated_restaurants)
        )

    activity_ids = [item["summary"]["selected"]["id"] for item in activities if item.get("summary")]
    repeated_activities = sorted({item for item in activity_ids if activity_ids.count(item) > 1})
    if repeated_activities:
        diagnostics.append(
            "Repeated Yelp activity primary selections across days: " + ", ".join(repeated_activities)
        )

    if not hotels.get("summary") or (hotels["summary"].get("placeholder") is True):
        diagnostics.append("Hotel search produced no primary hotel result.")

    if not restaurant_ids:
        diagnostics.append("No Yelp restaurant results were selected for any day.")
    if not activity_ids and not yelp_events.get("summary"):
        diagnostics.append("No Yelp activity or Yelp Events results were selected.")

    diagnostics.append(
        "Current app pipeline reality check: trip generation calls Groq for itinerary metadata only; "
        "restaurant/activity cards come from Yelp, flights/hotel cards come from SerpAPI."
    )
    diagnostics.append(
        "Current app pipeline also computes budget slices client-side, but remaining budget is not sent "
        "to any live API during generation because Groq event generation is not invoked."
    )
    return diagnostics


def to_serializable(value: Any) -> Any:
    if isinstance(value, dict):
        return {str(key): to_serializable(item) for key, item in value.items()}
    if isinstance(value, list):
        return [to_serializable(item) for item in value]
    if isinstance(value, tuple):
        return [to_serializable(item) for item in value]
    if isinstance(value, (str, int, float, bool)) or value is None:
        return value
    if hasattr(value, "__dict__"):
        return to_serializable(value.__dict__)
    return str(value)


def markdown_code_block(value: Any, language: str = "json") -> str:
    if isinstance(value, str):
        content = value
        if language == "json":
            try:
                content = safe_json_dumps(json.loads(value))
            except json.JSONDecodeError:
                language = "text"
        return f"```{language}\n{content}\n```"
    return f"```{language}\n{safe_json_dumps(to_serializable(value))}\n```"


def render_markdown_report(run: dict[str, Any]) -> str:
    lines: list[str] = []
    lines.append("# Trip Generation Debug Report")
    lines.append("")
    lines.append(f"- Status: `{run.get('status', 'unknown')}`")
    lines.append(f"- Generated at: `{run['generated_at']}`")
    lines.append(f"- Output directory: `{run['output_dir']}`")
    lines.append("- Mirrors current Android pipeline: `Groq -> Serp flights/hotels -> Yelp daily -> Yelp events`")
    lines.append("- Important: current app flow does **not** call `GroqRepository.generateEvents()` during generation.")
    lines.append("")
    if run.get("error"):
        lines.append("## Pipeline Error")
        lines.append("")
        lines.append(markdown_code_block(run["error"]))
        lines.append("")
    lines.append("## Input Request")
    lines.append("")
    lines.append(markdown_code_block(run["request"]))
    lines.append("")
    lines.append("## Parsed Itinerary")
    lines.append("")
    lines.append(markdown_code_block(run["itinerary"]))
    lines.append("")
    lines.append("## Pipeline Order")
    lines.append("")
    lines.append("1. `CRAFTING_ITINERARY`: Groq `chat/completions`")
    lines.append("2. `SEARCHING_FLIGHTS` and `FINDING_HOTELS`: SerpAPI launched in parallel")
    lines.append("3. `FINDING_RESTAURANTS`: one Yelp business search per trip day, launched in parallel")
    lines.append("4. `FINDING_ACTIVITIES`: one Yelp activity search per trip day plus one Yelp events search, launched in parallel")
    lines.append("")
    lines.append("## Diagnostics")
    lines.append("")
    diagnostics = run["diagnostics"]
    if diagnostics:
        for item in diagnostics:
            lines.append(f"- {item}")
    else:
        lines.append("- No obvious diagnostic flags were detected.")
    lines.append("")
    lines.append("## Selected Result Summary")
    lines.append("")
    lines.append("### Groq")
    lines.append("")
    lines.append(markdown_code_block(run["groq_summary"]))
    lines.append("")
    lines.append("### Serp Flights")
    lines.append("")
    lines.append(markdown_code_block(run["flights"]["summary"]))
    lines.append("")
    lines.append("### Serp Hotels")
    lines.append("")
    lines.append(markdown_code_block(run["hotels"]["summary"]))
    lines.append("")
    lines.append("### Serp Hotel Images")
    lines.append("")
    hotel_images = run["hotels"].get("images") or {}
    lines.append(f"- Attempted: `{hotel_images.get('attempted_count', 0)}`")
    lines.append(f"- Downloaded: `{hotel_images.get('downloaded_count', 0)}`")
    lines.append(f"- Failed: `{hotel_images.get('failed_count', 0)}`")
    lines.append(f"- Folder: `{hotel_images.get('folder', '')}`")
    lines.append("")
    lines.append("### Yelp Restaurants")
    lines.append("")
    lines.append(markdown_code_block([item["summary"] for item in run["restaurants"] if item.get("summary")]))
    lines.append("")
    lines.append("### Yelp Activities")
    lines.append("")
    lines.append(markdown_code_block([item["summary"] for item in run["activities"] if item.get("summary")]))
    lines.append("")
    lines.append("### Yelp Events")
    lines.append("")
    lines.append(markdown_code_block(run["yelp_events"]["summary"]))
    lines.append("")
    lines.append("## Chronological Timeline")
    lines.append("")

    for entry in run["timeline"]:
        lines.append(f"### {entry['seq']:03d} `{entry['timestamp']}` `{entry['kind'].upper()}` `{entry['service']}` `{entry['label']}`")
        lines.append("")
        lines.append(f"- Phase: `{entry['phase']}`")
        if entry["call_id"]:
            lines.append(f"- Call ID: `{entry['call_id']}`")
        data = entry["data"]
        if entry["kind"] == "request":
            lines.append(f"- Method: `{data.get('method', '')}`")
            lines.append(f"- URL: `{data.get('url', '')}`")
            if data.get("query"):
                lines.append("- Query")
                lines.append("")
                lines.append(markdown_code_block(data["query"]))
            if data.get("headers"):
                lines.append("- Headers")
                lines.append("")
                lines.append(markdown_code_block(data["headers"]))
            if data.get("json_body") is not None:
                lines.append("- JSON Body")
                lines.append("")
                lines.append(markdown_code_block(data["json_body"]))
        elif entry["kind"] == "response":
            lines.append(f"- Status: `{data.get('status_code')}`")
            lines.append(f"- Duration: `{data.get('duration_ms')}` ms")
            if data.get("error"):
                lines.append("- Error: `true`")
            if data.get("body") is not None:
                lines.append("- Body")
                lines.append("")
                lines.append(markdown_code_block(data["body"]))
        else:
            lines.append("- Payload")
            lines.append("")
            lines.append(markdown_code_block(data))
        lines.append("")

    return "\n".join(lines)


def run_pipeline(
    request: TravelRequest,
    secrets: dict[str, str],
    output_dir: pathlib.Path,
) -> dict[str, Any]:
    recorder = TimelineRecorder()
    empty_itinerary = Itinerary(
        itineraryId="",
        userId=request.userId,
        tripName="",
        destination="",
        origin="",
        originIata="",
        destinationIata="",
        dateFrom=request.dateFrom,
        dateTo=request.dateTo,
        durationDays=0,
        currency=request.currency,
        travelStyle=request.travelStyle,
        adults=request.adults,
        children=request.children,
        createdAt="",
        status="",
        eventIds=[],
    )
    recorder.add(
        kind="info",
        phase="STARTUP",
        service="pipeline",
        label="request_loaded",
        data={"request": asdict(request)},
    )
    itinerary = empty_itinerary
    groq_response: dict[str, Any] = {}
    groq_prompt = ""
    flights: dict[str, Any] = {"response": {}, "summary": {}}
    hotels: dict[str, Any] = {"response": {}, "summary": {}, "images": {}}
    restaurants: list[dict[str, Any]] = []
    activities: list[dict[str, Any]] = []
    yelp_events: dict[str, Any] = {"response": {}, "summary": []}
    diagnostics: list[str] = []
    status = "success"
    error_summary: dict[str, Any] | None = None

    try:
        itinerary, groq_response, groq_prompt = call_groq_itinerary(request, secrets["GROQ_API_KEY"], recorder)

        budget = request.budgetTotal
        hotel_budget_per_night = (budget * 0.40) / itinerary.durationDays if budget > 0 and itinerary.durationDays > 0 else 0.0
        recorder.add(
            kind="info",
            phase="FINDING_HOTELS",
            service="pipeline",
            label="budget_slice_computed",
            data={
                "budget_total": budget,
                "hotel_budget_per_night": hotel_budget_per_night,
                "duration_days": itinerary.durationDays,
            },
        )

        with concurrent.futures.ThreadPoolExecutor(max_workers=12) as executor:
            flights_future = executor.submit(search_flights, request, itinerary, secrets["SERP_API_KEY"], recorder)
            hotels_future = executor.submit(
                search_hotels,
                request,
                itinerary,
                secrets["SERP_API_KEY"],
                recorder,
                hotel_budget_per_night,
            )

            flights = flights_future.result()
            hotels = hotels_future.result()
            hotel_images = download_hotel_images(hotels.get("response") or {}, output_dir, recorder)
            hotels["images"] = hotel_images

            trip_dates = generate_trip_dates(request.dateFrom, itinerary.durationDays)
            recorder.add(
                kind="info",
                phase="FINDING_RESTAURANTS",
                service="pipeline",
                label="trip_dates_generated",
                data={"trip_dates": trip_dates},
            )

            restaurant_futures = [
                executor.submit(
                    search_restaurant_for_day,
                    itinerary.destination,
                    date,
                    request.dietary,
                    secrets["YELP_API_KEY"],
                    recorder,
                )
                for date in trip_dates
            ]
            restaurants = [future.result() for future in restaurant_futures]

            activity_futures = [
                executor.submit(
                    search_activity_for_day,
                    itinerary.destination,
                    date,
                    secrets["YELP_API_KEY"],
                    recorder,
                )
                for date in trip_dates
            ]
            yelp_events_future = executor.submit(
                search_yelp_events,
                itinerary.destination,
                request.dateFrom,
                request.dateTo,
                secrets["YELP_API_KEY"],
                recorder,
            )
            activities = [future.result() for future in activity_futures]
            yelp_events = yelp_events_future.result()

        diagnostics = compute_diagnostics(
            request=request,
            itinerary=itinerary,
            flights=flights,
            hotels=hotels,
            restaurants=restaurants,
            activities=activities,
            yelp_events=yelp_events,
        )
    except Exception as exc:
        status = "error"
        error_summary = {
            "type": type(exc).__name__,
            "message": str(exc),
        }
        if isinstance(exc, RemoteCallError):
            error_summary["status_code"] = exc.status_code
            error_summary["body"] = to_serializable(exc.body)
            body_text = str(exc.body or "")
            if exc.status_code == 403 and "1010" in body_text:
                error_summary["hint"] = (
                    "HTTP 403 with body 'error code: 1010' usually means Cloudflare blocked the "
                    "client fingerprint. The script now sends explicit User-Agent and Accept headers."
                )
        recorder.add(
            kind="error",
            phase="PIPELINE_ABORTED",
            service="pipeline",
            label="run_failed",
            data=error_summary,
        )
        diagnostics = diagnostics or []
        diagnostics.append(f"Pipeline aborted: {error_summary['message']}")

    timeline = [
        {
            "seq": entry.seq,
            "timestamp": entry.timestamp,
            "monotonic_ms": round(entry.monotonic_ms, 3),
            "kind": entry.kind,
            "phase": entry.phase,
            "service": entry.service,
            "label": entry.label,
            "call_id": entry.call_id,
            "data": to_serializable(entry.data),
        }
        for entry in sorted(recorder.entries(), key=lambda item: (item.monotonic_ms, item.seq))
    ]

    return {
        "status": status,
        "error": error_summary,
        "generated_at": utc_now_iso(),
        "request": asdict(request),
        "itinerary": asdict(itinerary),
        "groq_summary": {
            "source": "groq",
            "model": GROQ_MODEL,
            "prompt": groq_prompt,
            "parsed_itinerary": asdict(itinerary),
            "raw_response": groq_response,
        },
        "flights": flights,
        "hotels": hotels,
        "restaurants": restaurants,
        "activities": activities,
        "yelp_events": yelp_events,
        "diagnostics": diagnostics,
        "timeline": timeline,
    }


def ensure_secrets(local_properties: dict[str, str]) -> dict[str, str]:
    secrets = {
        "GROQ_API_KEY": get_secret("GROQ_API_KEY", local_properties),
        "SERP_API_KEY": get_secret("SERP_API_KEY", local_properties),
        "YELP_API_KEY": get_secret("YELP_API_KEY", local_properties),
    }
    missing = [name for name, value in secrets.items() if not value]
    if missing:
        raise SystemExit(
            "Missing API keys: "
            + ", ".join(missing)
            + ". Set them as environment variables or in local.properties."
        )
    return secrets


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Replay the Travel Cents trip-generation pipeline and write a "
            "chronological request/response report."
        )
    )
    parser.add_argument("--request-file", help="Path to a JSON file matching TravelRequest fields.")
    parser.add_argument("--origin")
    parser.add_argument("--destination")
    parser.add_argument("--date-from", dest="date_from")
    parser.add_argument("--date-to", dest="date_to")
    parser.add_argument("--user-id", default="debug-user")
    parser.add_argument("--adults", type=int, default=1)
    parser.add_argument("--children", type=int, default=0)
    parser.add_argument("--travel-style", default="comfort")
    parser.add_argument("--currency", default="USD")
    parser.add_argument("--budget-total", type=float, default=0.0)
    parser.add_argument("--dietary", default="")
    parser.add_argument("--interests", default="")
    parser.add_argument("--special-requests", default="")
    parser.add_argument(
        "--output-dir",
        help=(
            "Optional directory for report output. Defaults to "
            "debug_pipeline_runs/<timestamp>/"
        ),
    )
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    request = build_request_from_args(args)

    local_properties = parse_local_properties(ROOT / "local.properties")
    secrets = ensure_secrets(local_properties)

    timestamp = dt.datetime.now().strftime("%Y%m%d_%H%M%S")
    output_dir = pathlib.Path(args.output_dir) if args.output_dir else REPORTS_DIR / timestamp
    output_dir.mkdir(parents=True, exist_ok=True)

    run = run_pipeline(request, secrets, output_dir)
    run["output_dir"] = str(output_dir)

    report_json_path = output_dir / "timeline.json"
    report_md_path = output_dir / "timeline.md"
    report_json_path.write_text(safe_json_dumps(to_serializable(run)), encoding="utf-8")
    report_md_path.write_text(render_markdown_report(run), encoding="utf-8")

    print(f"Report written to {report_md_path}")
    print(f"Structured timeline written to {report_json_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
