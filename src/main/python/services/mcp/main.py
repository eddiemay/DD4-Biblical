import asyncio
import sys
import traceback

sys.stderr.reconfigure(errors="backslashreplace")

try:
  from mcp_server import mcp
except BaseException:
  traceback.print_exc(file=sys.stderr)
  raise

if __name__ == "__main__":
  asyncio.run(mcp.run_async(
      transport="sse",
      host="0.0.0.0",
      port=8080,
      path="/mcp",
      log_level="debug"))