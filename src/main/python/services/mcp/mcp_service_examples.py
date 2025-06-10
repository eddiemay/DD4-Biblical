from mcp_service_base import mcp


@mcp.tool
def greet(name: str) -> str:
  """Returns a simple greeting."""
  return f"Hello {name}!"


@mcp.tool
def add(a: int, b: int) -> int:
  """Adds two numbers together."""
  return a + b


@mcp.prompt("summarize")
async def summarize_prompt(text: str) -> list[dict]:
  """Generates a prompt to summarize the provided text."""
  return [
    {"role": "system", "content": "You are a helpful assistant skilled at summarization."},
    {"role": "user", "content": f"Please summarize the following text:\n\n{text}"}
  ]