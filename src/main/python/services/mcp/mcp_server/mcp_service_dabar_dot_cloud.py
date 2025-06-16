import json
from mcp_service_base import mcp
from urllib import parse, request


def _fetch_scripture(reference: str) -> list:
  fetch_url = "https://dabar.cloud/_api/scriptures/v1/fetch?" \
              f"searchText={parse.quote(reference)}&lang=en&version=ISR"
  print("Sending request: ", fetch_url)
  with request.urlopen(fetch_url) as url:
    return json.load(url)['items']

@mcp.resource("scriptures://{reference}/fetch")
def fetch_scripture(reference: str) -> list:
  """Fetches Bible scriptures
    Args:
      reference The scripture reference such as
        Gen 2:3 or Exodus 20:13-15 or Matt 5:17,Mark 9:11-14
    Returns:
       json array of scriptures requested with metadata and content as text.
    Examples:
      scriptures://Genesis 2:3/fetch
      returns [{\n  "id": "ISR-en-Genesis-2-3",\n  "version": "ISR",\n  "language": "en",\n  "book": "Genesis",\n  "chapter": 2,\n  "verse": 3,\n  "text": "And Elohim blessed the seventh day and set it apart, because on it He rested from all His work which Elohim in creating had made.",\n  "reference": "Genesis 2:3"\n}]

      scriptures://Exodus 20:13-15/fetch
      returns [\n  {\n    "id": "ISR-en-Exodus-20-13",\n    "version": "ISR",\n    "language": "en",\n    "book": "Exodus",\n    "chapter": 20,\n    "verse": 13,\n    "text": "“You do not murder.",\n    "reference": "Exodus 20:13"\n  },\n  {\n    "id": "ISR-en-Exodus-20-14",\n    "version": "ISR",\n    "language": "en",\n    "book": "Exodus",\n    "chapter": 20,\n    "verse": 14,\n    "text": "“You do not commit adultery.",\n    "reference": "Exodus 20:14"\n  },\n  {\n    "id": "ISR-en-Exodus-20-15",\n    "version": "ISR",\n    "language": "en",\n    "book": "Exodus",\n    "chapter": 20,\n    "verse": 15,\n    "text": "“You do not steal.",\n    "reference": "Exodus 20:15"\n  }\n]
  """
  return _fetch_scripture(reference)


@mcp.tool
def fetch_scriptures(reference: str) -> list:
  """Fetches Bible scriptures
    Args:
      reference The scripture reference such as
        Gen 2:3 or Exodus 20:13-15 or Matt 5:17,Mark 9:11-14
    Returns:
       json array of scriptures requested with metadata and content as text.
    Examples:
      scriptures://Genesis 2:3/fetch
      returns [{\n  "id": "ISR-en-Genesis-2-3",\n  "version": "ISR",\n  "language": "en",\n  "book": "Genesis",\n  "chapter": 2,\n  "verse": 3,\n  "text": "And Elohim blessed the seventh day and set it apart, because on it He rested from all His work which Elohim in creating had made.",\n  "reference": "Genesis 2:3"\n}]

      scriptures://Exodus 20:13-15/fetch
      returns [\n  {\n    "id": "ISR-en-Exodus-20-13",\n    "version": "ISR",\n    "language": "en",\n    "book": "Exodus",\n    "chapter": 20,\n    "verse": 13,\n    "text": "“You do not murder.",\n    "reference": "Exodus 20:13"\n  },\n  {\n    "id": "ISR-en-Exodus-20-14",\n    "version": "ISR",\n    "language": "en",\n    "book": "Exodus",\n    "chapter": 20,\n    "verse": 14,\n    "text": "“You do not commit adultery.",\n    "reference": "Exodus 20:14"\n  },\n  {\n    "id": "ISR-en-Exodus-20-15",\n    "version": "ISR",\n    "language": "en",\n    "book": "Exodus",\n    "chapter": 20,\n    "verse": 15,\n    "text": "“You do not steal.",\n    "reference": "Exodus 20:15"\n  }\n]
  """
  return _fetch_scripture(reference)


@mcp.tool
def similarity_compare(a: str, b: str) -> float:
  """ Does a similarity comparison of 2 strings.
    Args:
      a first string
      b second string
    Returns:
      A Tensor object of comparison scores for each string comparison.
  """

  from sentence_transformers import SentenceTransformer
  from sentence_transformers import util
  embedding_model = SentenceTransformer("all-MiniLM-L6-v2")
  embeddings1 = embedding_model.encode([a], convert_to_tensor=True)
  embeddings2 = embedding_model.encode([b], convert_to_tensor=True)
  return util.cos_sim(embeddings1, embeddings2)[0][0].item()


@mcp.tool
def fetch_strongs_def(strongs_id: str) -> list:
  """Fetches the Strong's definition of a given Strong's ID. With fields such
  as the Hebrew word, translation, transliteration, pronuciation, parkOfSpeech,
  rootWord, dictionaryAid, translationCounts, outline, strongsDefinition,
  referenceCount as well as constantsOnly, restored and ancient Hebrew representations.
    Args:
      strongs_id The ID of the strong's reference to fetch. i.e. H6963 or G1234
    Returns:
       A json object of the strong's reference
    Examples:
      fetch_strongs_def('H6963')
      returns {"id":"H6963","word":"קוֹל","transliteration":"qôl","pronunciation":"kole","partOfSpeech":"masculine noun","rootWord":"From an unused root meaning to call aloud","dictionaryAid":"TWOT Reference: 1998a,2028b","translationCounts":[{"word":"voice","count":383},{"word":"noise","count":49},{"word":"sound","count":39},{"word":"thunder","count":10},{"word":"proclamation (with H5674)","count":4},{"word":"send out (with H5414)","count":2},{"word":"thunderings","count":2},{"word":"fame","count":1},{"word":"miscellaneous","count":16}],"outline":[{"value":"voice, sound, noise voice sound (of instrument)","children":[{"value":"voice"},{"value":"sound (of instrument)"}]},{"value":"lightness, frivolity"}],"strongsDefinition":"קוֹל qôwl, kole; or קֹל qôl; from an unused root meaning to call aloud; a voice or sound:—+ aloud, bleating, crackling, cry (+ out), fame, lightness, lowing, noise, + hold peace, (pro-) claim, proclamation, + sing, sound, + spark, thunder(-ing), voice, + yell.","translation":"voice","referenceCount":507,"constantsOnly":"קול","restored":"קול","ancient":"𓏱𓏲𐤒"}
  """
  fetch_url = f"https://dabar.cloud/_api/lexicons/v1/get?id={strongs_id}"
  print("Sending request: ", fetch_url)
  with request.urlopen(fetch_url) as url:
    return json.load(url)


@mcp.tool
def fetch_strongs_translations(strongs_id: str) -> list:
  """Fetches the different ways a Strong's ID is translated based on the
  different Hebrew forms it shows up in the Bible.
    Args:
      strongs_id The ID of the strong's reference to fetch. i.e. H6963 or G1234
    Returns:
       A json list of the different ways it is translated based on the Hebrew
       forms.
    Examples:
      fetch_strongs_translation_forms('H1254')
      returns {"items":[{"word":"בורא","strongsId":"H1254","translation":"creator","id":"בורא-H1254"},{"word":"ברא","strongsId":"H1254","translation":"create","id":"ברא-H1254"},{"word":"ברוא","strongsId":"H1254","translation":"created","id":"ברוא-H1254"},{"word":"בריא","strongsId":"H1254","translation":"creating","id":"בריא-H1254"}]}
  """
  fetch_url = f"https://dabar.cloud/_api/tokenWords/v1/getTranslations?strongsId={strongs_id}"
  print("Sending request: ", fetch_url)
  with request.urlopen(fetch_url) as url:
    return json.load(url)['items']
