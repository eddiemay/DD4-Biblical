import mcp_server
from fastmcp import Client

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