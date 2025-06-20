import asyncio
import json

from fastmcp import Client
from urllib import parse

async def interact_with_server():
  print("--- Creating Client ---")

  # Option 1: Connect to a server run via `python my_server.py` (uses stdio)
  # client = Client("mcp_server.py")

  # Option 2: Connect to a server run via `python mcp_server.py`
  # client =  # Use the correct URL/port

  # print(f"Client configured to connect to: {client.target}")

  try:
    async with Client("mcp_server.py") as client:
      print("--- Client Connected ---")
      # Call the 'similarity_compare' tool
      compare_result = await client.call_tool("similarity_compare", {"a": "Remote Client", "b": "Local Client"})
      print(f"compare result: {compare_result}")
      assert float(compare_result[0].text) < .8

      scripture = (await client.read_resource(f"scriptures://{parse.quote('Gen 15:13')}/fetch"))[0].text
      scripture = json.loads(scripture)[0]
      print(f"Scripture: {scripture}")
      assert "Know for certain that your seed are to be sojourners in a land that is not theirs" in scripture['text']

      scripture = (await client.call_tool("fetch_scripture", {"reference": "Gen 2:3"}))[0].text
      scripture = json.loads(scripture)
      print(f"Scripture: {scripture}")
      assert "Elohim blessed the seventh day and set it apart" in scripture['text']

      strongsDef = (await client.call_tool("fetch_strongs_def", {"strongs_id": "H5307"}))[0].text
      strongsDef = json.loads(strongsDef)
      print(f"Strong's Def: {strongsDef}")
      assert strongsDef['id'] == "H5307"
      assert strongsDef['restored'] == "נפל"
      assert strongsDef['referenceCount'] == 435

  except Exception as e:
    print(f"An error occurred: {e}")
  finally:
    print("--- Client Interaction Finished ---")

if __name__ == "__main__":
  asyncio.run(interact_with_server())