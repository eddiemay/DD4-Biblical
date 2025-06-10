import json
from mcp_service_base import mcp
from urllib import parse, request


@mcp.resource("scriptures://{reference}/fetch")
def fetch_scripture(reference: str) -> list:
  """Fetches Bible scriptures
    returns json array of scriptures requested.

    Examples:
      scriptures://Genesis 2:3/fetch
      returns [{\n  "id": "ISR-en-Genesis-2-3",\n  "version": "ISR",\n  "language": "en",\n  "book": "Genesis",\n  "chapter": 2,\n  "verse": 3,\n  "text": "And Elohim blessed the seventh day and set it apart, because on it He rested from all His work which Elohim in creating had made.",\n  "reference": "Genesis 2:3"\n}]

      scriptures://Exodus 20:13-15/fetch
      returns [\n  {\n    "id": "ISR-en-Exodus-20-13",\n    "version": "ISR",\n    "language": "en",\n    "book": "Exodus",\n    "chapter": 20,\n    "verse": 13,\n    "text": "“You do not murder.",\n    "reference": "Exodus 20:13"\n  },\n  {\n    "id": "ISR-en-Exodus-20-14",\n    "version": "ISR",\n    "language": "en",\n    "book": "Exodus",\n    "chapter": 20,\n    "verse": 14,\n    "text": "“You do not commit adultery.",\n    "reference": "Exodus 20:14"\n  },\n  {\n    "id": "ISR-en-Exodus-20-15",\n    "version": "ISR",\n    "language": "en",\n    "book": "Exodus",\n    "chapter": 20,\n    "verse": 15,\n    "text": "“You do not steal.",\n    "reference": "Exodus 20:15"\n  }\n]
  """

  fetch_url = "https://dd4-biblical.appspot.com/_api/scriptures/v1/fetch?" \
              f"searchText={parse.quote(reference)}&lang=en&version=ISR"
  print("Sending request: ", fetch_url)
  with request.urlopen(fetch_url) as url:
    response = json.load(url)
    # print('Response: ', response)
    scriptures = response['items']
    return scriptures
