import requests
import pandas as pd
import time
import os
import torch
from dotenv import load_dotenv
from pinecone import Pinecone
from sentence_transformers import SentenceTransformer

load_dotenv()

DEMO_CITIES = {
    "London": "737",
    "Paris": "479",
    "Amsterdam": "525",
    "New York": "687",
    "Miami": "662",
    "Cairo": "722",
    "Sydney": "357",
    "Tokyo": "334"
}

VIATOR_PRODUCT_SEARCH_URL = "https://api.viator.com/partner/products/search"
VIATOR_PAGE_SIZE = 50
PINECONE_BATCH_SIZE = 100


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


def step_one_fetch_and_filter(api_key):
    print("Connecting to Viator PRODUCTION API...")
    headers = {
        "exp-api-key": api_key,
        "Accept-Language": "en-US",
        "Accept": "application/json;version=2.0"
    }

    all_activities = []

    for city_name, dest_id in DEMO_CITIES.items():
        print(f"Fetching inventory for {city_name} (ID: {dest_id})...")
        city_activity_count = 0
        page_start = 1

        while True:
            payload = {
                "filtering": {"destination": dest_id},
                "pagination": {
                    "start": page_start,
                    "count": VIATOR_PAGE_SIZE
                },
                "currency": "USD"
            }

            response = requests.post(
                VIATOR_PRODUCT_SEARCH_URL,
                headers=headers,
                json=payload
            )

            if response.status_code != 200:
                print(f"Failed to fetch {city_name}. Status: {response.status_code}")
                print(response.text)
                break

            data = response.json()
            products = data.get('products', [])

            if not products:
                break

            for item in products:
                activity_id = item.get('productCode')
                if not activity_id:
                    continue
                image_urls = extract_viator_image_urls(item)

                all_activities.append({
                    "activity_id": activity_id,
                    "title": item.get('title') or '',
                    "description": item.get('description', ''),
                    "city": city_name,
                    "booking_url": item.get('productUrl', ''),
                    "image_url": image_urls[0] if image_urls else '',
                    "photo_urls": image_urls[:5]
                })
                city_activity_count += 1

            print(
                f"  Page starting at {page_start}: "
                f"fetched {len(products)} activities "
                f"({city_activity_count} total for {city_name})"
            )

            if len(products) < VIATOR_PAGE_SIZE:
                break

            page_start += VIATOR_PAGE_SIZE
            time.sleep(2)

        print(f"Completed {city_name}: {city_activity_count} activities fetched.")
        time.sleep(2)

    df = pd.DataFrame(all_activities)
    if df.empty:
        print("No activities fetched.\n")
        return df

    df = df.fillna('')
    print(f"Successfully fetched {len(df)} activities ready for VRAM.\n")
    return df


def step_two_embed_and_upload(df, pinecone_key):
    if df.empty:
        print("No activities to upload.")
        return 0

    # 1. Prep the text for the AI
    df['semantic_text'] = df['title'] + " - " + df['description']
    sentences = df['semantic_text'].tolist()

    # 2. Load the Model onto the RTX 3060
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"Loading BAAI/bge-base-en-v1.5 onto: {device.upper()}...")
    model = SentenceTransformer('BAAI/bge-base-en-v1.5', device=device)

    # 3. Generate 768-Dimension Vectors
    print("Crunching the math (this will take a few minutes)...")
    # Batch size of 64 is highly optimized for 6GB VRAM
    embeddings = model.encode(sentences, batch_size=64, show_progress_bar=True)

    # 4. Connect to Pinecone
    print("\nConnecting to Pinecone...")
    pc = Pinecone(api_key=pinecone_key)

    # IMPORTANT: Update this string if you named your index something else!
    index = pc.Index("travel-cents-inventory")

    print("Clearing existing activity records for demo cities...")
    for city_name in DEMO_CITIES.keys():
        print(f"  Deleting existing Pinecone records for {city_name}...")
        index.delete(filter={"city": {"$eq": city_name}})
    print(f"Cleared existing records for {len(DEMO_CITIES)} demo cities.")

    # 5. Upload in Batches
    print("Uploading vectors to the cloud...")
    total_seeded = 0
    for i in range(0, len(df), PINECONE_BATCH_SIZE):
        batch_df = df.iloc[i:i + PINECONE_BATCH_SIZE]
        batch_embeddings = embeddings[i:i + PINECONE_BATCH_SIZE]

        vectors_to_upsert = []
        for j, (_, row) in enumerate(batch_df.iterrows()):
            vectors_to_upsert.append({
                "id": f"{row['city']}::{row['activity_id']}",
                "values": batch_embeddings[j].tolist(),
                "metadata": {
                    "activity_id": str(row['activity_id']),
                    "title": row['title'],
                    "booking_url": row['booking_url'],
                    "image_url": row['image_url'],
                    "photo_urls": row['photo_urls'],
                    "city": row['city']
                }
            })
        index.upsert(vectors=vectors_to_upsert)
        total_seeded += len(vectors_to_upsert)
        print(f"  Uploaded batch {i // PINECONE_BATCH_SIZE + 1}: {len(vectors_to_upsert)} activities")

    print("\nSUCCESS: Database fully seeded and ready for the cloud!")
    return total_seeded


if __name__ == "__main__":
    v_key = os.getenv("VIATOR_API_KEY")
    p_key = os.getenv("PINECONE_API_KEY")

    if not v_key or not p_key:
        print("Error: Missing API keys in the .env file.")
    else:
        # Run the full pipeline
        demo_dataframe = step_one_fetch_and_filter(v_key)
        seeded_count = step_two_embed_and_upload(demo_dataframe, p_key)
        print(
            f"Successfully seeded {seeded_count} total activities "
            f"across {len(DEMO_CITIES)} cities!"
        )
