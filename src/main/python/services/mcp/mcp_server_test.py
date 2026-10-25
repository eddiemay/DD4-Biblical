import asyncio
import json
import mcp_server
from fastmcp import Client
from urllib import parse

client = Client(mcp_server.mcp)

async def call_tool(name: str, arguments: dict):
  print(f"Testing {name} with arguments {dict}")
  async with client:
    result = await client.call_tool(name, arguments)
    print(result)
    if result.is_error:
      raise RuntimeError(f"MCP Error: {result.content}")
    return result


async def read_resource(url: str):
  async with client:
    result = await client.read_resource(url)
    print(result)
    return result


async def get_prompt(name: str, arguments: dict):
  async with client:
    result = await client.get_prompt(name, arguments)
    print(result)
    return result


def test_fetch_scripture_resource():
  scriptures = json.loads(asyncio.run(read_resource(
      f"scriptures://{parse.quote('Gen 2:3')}/fetch"))[0].text)

  assert len(scriptures) == 1
  scripture = scriptures[0]
  assert scripture['version'] == 'ISR'
  assert scripture['language'] == 'en'
  assert scripture['book'] == 'Genesis'
  assert scripture['chapter'] == 2
  assert scripture['verse'] == 3
  assert "And Elohim blessed the seventh day and set it apart" in scripture['text']


def test_fetch_scripture_resource_multi():
  scriptures = json.loads(asyncio.run(read_resource(
      f"scriptures://{parse.quote('Exo 20:13-15')}/fetch"))[0].text)

  assert len(scriptures) == 3
  verse = 13
  for scripture in scriptures:
    assert scripture['version'] == 'ISR'
    assert scripture['language'] == 'en'
    assert scripture['book'] == 'Exodus'
    assert scripture['chapter'] == 20
    assert scripture['verse'] == verse
    verse += 1

  assert "You do not murder." in scriptures[0]['text']
  assert "You do not commit adultery." in scriptures[1]['text']
  assert "You do not steal." in scriptures[2]['text']


def test_fetch_scripture():
  scripture = json.loads(asyncio.run(call_tool(
      "fetch_scripture", {"reference": "Gen 2:3"})).content[0].text)[0]

  assert scripture['version'] == 'ISR'
  assert scripture['language'] == 'en'
  assert scripture['book'] == 'Genesis'
  assert scripture['chapter'] == 2
  assert scripture['verse'] == 3
  assert "And Elohim blessed the seventh day and set it apart" in scripture['text']


def test_fetch_scripture_multi():
  scriptures = json.loads(asyncio.run(call_tool(
      "fetch_scripture", {"reference": "Exo 20:13-15"})).content[0].text)

  assert len(scriptures) == 3
  verse = 13
  for scripture in scriptures:
    assert scripture['version'] == 'ISR'
    assert scripture['language'] == 'en'
    assert scripture['book'] == 'Exodus'
    assert scripture['chapter'] == 20
    assert scripture['verse'] == verse
    verse += 1

  assert "You do not murder." in scriptures[0]['text']
  assert "You do not commit adultery." in scriptures[1]['text']
  assert "You do not steal." in scriptures[2]['text']


def test_fetch_strongs_def():
  strongs_def = json.loads(asyncio.run(call_tool(
      "fetch_strongs_def", {"strongs_id": "H6963"})).content[0].text)

  assert strongs_def['id'] == 'H6963'
  assert strongs_def['word'] == 'קוֹל'
  assert strongs_def['translation'] == 'voice'
  assert strongs_def['referenceCount'] == 507
  assert strongs_def['ancient'] == "𓏱𓏲𐤒"
  assert "from an unused root meaning to call aloud; a voice or sound" in strongs_def['strongsDefinition']


def test_fetch_strongs_translations():
  translations = json.loads(asyncio.run(call_tool(
      "fetch_strongs_translations", {"strongs_id": "H1254"})).content[0].text)

  assert len(translations) == 4
  assert translations[0]['word'] == 'בורא'
  assert translations[0]['translation'] == 'creator'
  assert translations[1]['word'] == 'ברא'
  assert translations[1]['translation'] == 'create'
  assert translations[2]['word'] == 'ברוא'
  assert translations[2]['translation'] == 'created'
  assert translations[3]['word'] == 'בריא'
  assert translations[3]['translation'] == 'creating'


def test_fetch_strongs_references():
  references = json.loads(asyncio.run(call_tool(
      "fetch_strongs_references", {"strongs_id": "H3153"})).content[0].text)

  assert len(references) == 2
  assert references[0]['restored'] == 'ויזניהו'
  assert references[0]['translation'] == 'and Jezaniah'
  assert references[0]['book'] == 'Jeremiah'
  assert references[0]['chapter'] == 40
  assert references[0]['verse'] == 8
  assert references[1]['restored'] == 'ויזניה'
  assert references[1]['translation'] == 'Jezaniah'
  assert references[1]['book'] == 'Jeremiah'
  assert references[1]['chapter'] == 42
  assert references[1]['verse'] == 1


def test_search_strongs():
  references = json.loads(asyncio.run(call_tool(
      "search_strongs", {"search_text": "faith", "language": "Hebrew"})).content[0].text)

  assert len(references) == 5
  assert references[0]['id'] == 'H0529'
  assert references[0]['word'] == 'אֵמוּן'
  assert references[1]['id'] == 'H0530'
  assert references[1]['word'] == 'אֱמוּנָה'
  assert references[2]['id'] == 'H0548'
  assert references[2]['word'] == 'אֲמָנָה'
  assert references[3]['id'] == 'H4159'
  assert references[3]['word'] == 'מוֹפֵת'
  assert references[4]['id'] == 'H7343'
  assert references[4]['word'] == 'רָחָב'


def ignore_test_similarity_compare():
  result = asyncio.run(call_tool(
      "similarity_compare", {"a": "I like dogs", "b": "I like puppies"}))
  score = float(result.content[0].text)
  assert score < .8

  result = asyncio.run(call_tool(
      "similarity_compare", {"a": "I like dogs", "b": "I love dogs"}))
  score = float(result.content[0].text)
  assert score > .88

  result = asyncio.run(call_tool(
      "similarity_compare", {"a": "I like dogs", "b": "It's prime time"}))
  score = float(result.content[0].text)
  assert score < .2
