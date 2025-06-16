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
      # Call the 'greet' tool
      greet_result = await client.call_tool("greet", {"name": "Remote Client"})
      print(f"greet result: {greet_result}")
      assert greet_result[0].text == "Hello Remote Client!"

      # Read the 'config' resource
      # config_data = await client.read_resource("data://config")
      # print(f"config resource: {config_data}")

      # Read user profile 102
      profile_102 = await client.read_resource("users://102/profile")
      print(f"User 102 profile: {profile_102}")

      # Call the 'similarity_compare' tool
      compare_result = await client.call_tool("similarity_compare", {"a": "Remote Client", "b": "Local Client"})
      print(f"compare result: {compare_result}")
      assert float(compare_result[0].text) < .8

      scripture = (await client.read_resource(f"scriptures://{parse.quote('Gen 15:13')}/fetch"))[0].text
      scripture = json.loads(scripture)[0]
      print(f"Scripture: {scripture}")
      assert "Know for certain that your seed are to be sojourners in a land that is not theirs" in scripture['text']

  except Exception as e:
    print(f"An error occurred: {e}")
  finally:
    print("--- Client Interaction Finished ---")

if __name__ == "__main__":
  asyncio.run(interact_with_server())