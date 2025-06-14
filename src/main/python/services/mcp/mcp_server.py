import asyncio
from mcp_service_base import mcp
import mcp_service_examples
import mcp_service_dabar_dot_cloud


if __name__ == "__main__":
  asyncio.run(
      mcp.run_async(
          transport="streamable-http",
          host="0.0.0.0",
          port=8080,
          path="/mcp",
          log_level="debug"))