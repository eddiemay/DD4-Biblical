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
      "fetch_scripture", {"reference": "Gen 2:3"}))[0].text)

  assert scripture['version'] == 'ISR'
  assert scripture['language'] == 'en'
  assert scripture['book'] == 'Genesis'
  assert scripture['chapter'] == 2
  assert scripture['verse'] == 3
  assert "And Elohim blessed the seventh day and set it apart" in scripture['text']


def test_fetch_scripture_multi():
  scriptures = json.loads(asyncio.run(call_tool(
      "fetch_scripture", {"reference": "Exo 20:13-15"}))[0].text)

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
      "fetch_strongs_def", {"strongs_id": "H6963"}))[0].text)

  assert strongs_def['id'] == 'H6963'
  assert strongs_def['word'] == 'קוֹל'
  assert strongs_def['translation'] == 'voice'
  assert strongs_def['referenceCount'] == 507
  assert strongs_def['ancient'] == "𓏱𓏲𐤒"
  assert "from an unused root meaning to call aloud; a voice or sound" in strongs_def['strongsDefinition']


def test_fetch_strongs_translations():
  translations = json.loads(asyncio.run(call_tool(
      "fetch_strongs_translations", {"strongs_id": "H1254"}))[0].text)

  assert len(translations) == 4
  assert translations[0]['word'] == 'בורא'
  assert translations[0]['translation'] == 'creator'
  assert translations[1]['word'] == 'ברא'
  assert translations[1]['translation'] == 'create'
  assert translations[2]['word'] == 'ברוא'
  assert translations[2]['translation'] == 'created'
  assert translations[3]['word'] == 'בריא'
  assert translations[3]['translation'] == 'creating'


def test_similarity_compare():
  result = asyncio.run(call_tool(
      "similarity_compare", {"a": "I like dogs", "b": "I like puppies"}))
  score = float(result[0].text)
  assert score < .8

  result = asyncio.run(call_tool(
      "similarity_compare", {"a": "I like dogs", "b": "I love dogs"}))
  score = float(result[0].text)
  assert score > .88

  result = asyncio.run(call_tool(
      "similarity_compare", {"a": "I like dogs", "b": "It's prime time"}))
  score = float(result[0].text)
  assert score < .2
