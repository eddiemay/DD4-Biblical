import asyncio
from mcp_server import mcp


if __name__ == "__main__":
  asyncio.run(
      mcp.run_async(
          transport="sse",
          host="0.0.0.0",
          port=8080,
          path="/mcp",
          log_level="debug"))