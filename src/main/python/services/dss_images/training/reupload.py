import json
import requests
from email.utils import parsedate_to_datetime

URL = "https://dd4-biblical.appspot.com/_api/letterBoxs/v1/batchCreate?idToken=107208779"

with open("isa_3.json", "r", encoding="utf-8") as f:
	data = json.load(f)

items = data["items"]

for item in items:
	# Convert RFC 2822 date strings to epoch milliseconds
	for field in ("creationTime", "lastModifiedTime"):
		if field in item and item[field]:
			item[field] = int(parsedate_to_datetime(item[field]).timestamp() * 1000)

response = requests.post(
		URL,
		json={"items": items},
		headers={"Content-Type": "application/json"}
)

print("Status:", response.status_code)
print(response.text)