import requests
import json

# The exact URL from your successful emulator boot
url = "http://127.0.0.1:5001/travel-cents-3e2d9/us-central1/generate_itinerary"

payload = {
    "data": {
        "city": "London"
    }
}

print("Firing request at the local Firebase brain...")
response = requests.post(url, json=payload)

print(f"Status Code: {response.status_code}")
print("Response:")
print(json.dumps(response.json(), indent=2))