from mcp_service_base import mcp


@mcp.tool
def greet(name: str) -> str:
  """Returns a simple greeting."""
  return f"Hello {name}!"


@mcp.tool
def add(a: int, b: int) -> int:
  """Adds two numbers together."""
  return a + b


USER_PROFILES = {
  101: {"id": 101, "name": "Alice", "status": "active"},
  102: {"id": 102, "name": "Bob", "status": "inactive"},
}

@mcp.resource("users://{user_id}/profile")
def get_user_profile(user_id: int) -> dict:
  """Retrieves a user's profile by their ID."""
  # The {user_id} from the URI is automatically passed as an argument
  return USER_PROFILES.get(user_id, {"error": "User not found"})


@mcp.prompt("summarize")
async def summarize_prompt(text: str) -> list[dict]:
  """Generates a prompt to summarize the provided text."""
  return [
    {"role": "system", "content": "You are a helpful assistant skilled at summarization."},
    {"role": "user", "content": f"Please summarize the following text:\n\n{text}"}
  ]