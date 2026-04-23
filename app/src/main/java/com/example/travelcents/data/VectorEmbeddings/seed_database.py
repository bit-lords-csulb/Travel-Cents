import requests
import pandas as pd
import time
import os
import torch
from dotenv import load_dotenv
from pinecone import Pinecone
from sentence_transformers import SentenceTransformer

load_dotenv()


def step_one_fetch_and_filter(api_key):
    print("Connecting to Viator PRODUCTION API...")
    headers = {
        "exp-api-key": api_key,
        "Accept-Language": "en-US",
        "Accept": "application/json;version=2.0"
    }

    demo_cities = {
        "London": "737", "Paris": "479", "Amsterdam": "525",
        "New York": "687", "Miami": "662", "Cairo": "722",
        "Sydney": "357", "Tokyo": "334"
    }
    all_activities = []

    for city_name, dest_id in demo_cities.items():
        print(f"Fetching inventory for {city_name} (ID: {dest_id})...")
        payload = {
            "filtering": {"destination": dest_id},
            "currency": "USD"
        }

        response = requests.post(
            "https://api.viator.com/partner/products/search",
            headers=headers,
            json=payload
        )

        if response.status_code == 200:
            data = response.json()
            for item in data.get('products', []):
                all_activities.append({
                    "activity_id": item.get('productCode'),
                    "title": item.get('title'),
                    "description": item.get('description', ''),
                    "city": city_name,
                    "booking_url": item.get('productUrl', '')
                })
        else:
            print(f"Failed to fetch {city_name}. Status: {response.status_code}")
        time.sleep(2)

    df = pd.DataFrame(all_activities)
    df = df.dropna(subset=['description', 'booking_url'])
    print(f"Successfully fetched {len(df)} activities ready for VRAM.\n")
    return df


def step_two_embed_and_upload(df, pinecone_key):
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

    # 5. Upload in Batches
    print("Uploading vectors to the cloud...")
    upload_batch_size = 100
    for i in range(0, len(df), upload_batch_size):
        batch_df = df.iloc[i:i + upload_batch_size]
        batch_embeddings = embeddings[i:i + upload_batch_size]

        vectors_to_upsert = []
        for j, (_, row) in enumerate(batch_df.iterrows()):
            vectors_to_upsert.append({
                "id": str(row['activity_id']),
                "values": batch_embeddings[j].tolist(),
                "metadata": {
                    "title": row['title'],
                    "booking_url": row['booking_url'],
                    "city": row['city']
                }
            })
        index.upsert(vectors=vectors_to_upsert)

    print("\nSUCCESS: Database fully seeded and ready for the cloud!")


if __name__ == "__main__":
    v_key = os.getenv("VIATOR_API_KEY")
    p_key = os.getenv("PINECONE_API_KEY")

    if not v_key or not p_key:
        print("Error: Missing API keys in the .env file.")
    else:
        # Run the full pipeline
        demo_dataframe = step_one_fetch_and_filter(v_key)
        step_two_embed_and_upload(demo_dataframe, p_key)