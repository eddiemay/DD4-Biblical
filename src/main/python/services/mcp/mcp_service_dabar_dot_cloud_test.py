import asyncio
import json
from urllib import parse
from mcp_service_base_test import read_resource


def test_fetch_scripture():
  scriptures = json.loads(asyncio.run(
      read_resource(f"scriptures://{parse.quote('Gen 2:3')}/fetch"))[0].text)

  assert len(scriptures) == 1
  scripture = scriptures[0]
  assert scripture['version'] == 'ISR'
  assert scripture['language'] == 'en'
  assert scripture['book'] == 'Genesis'
  assert scripture['chapter'] == 2
  assert scripture['verse'] == 3
  assert "And Elohim blessed the seventh day and set it apart" in scripture['text']


def test_fetch_scriptures():
  scriptures = json.loads(asyncio.run(
      read_resource(f"scriptures://{parse.quote('Exo 20:13-15')}/fetch"))[0].text)

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