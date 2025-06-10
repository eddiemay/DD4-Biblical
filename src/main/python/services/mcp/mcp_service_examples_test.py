import asyncio
from mcp_service_base_test import call_tool, get_prompt


def test_add():
  assert asyncio.run(call_tool("add", {"a": 1, "b": 1}))[0].text == '2'
  assert asyncio.run(call_tool("add", {"a": 2, "b": 2}))[0].text == '4'
  assert asyncio.run(call_tool("add", {"a": 4, "b": 4}))[0].text == '8'
  assert asyncio.run(call_tool("add", {"a": 8, "b": 8}))[0].text == '16'


def test_greet():
  assert asyncio.run(call_tool(
      "greet", {"name": "World"}))[0].text == 'Hello World!'
  assert asyncio.run(call_tool(
      "greet", {"name": "From MCP"}))[0].text == 'Hello From MCP!'


def test_summarize():
  result = asyncio.run(get_prompt(
      "summarize", {"text": "Anyone who eats leavened bread from Yom HaRashun until Yom HaShabiyah should be cut off from Israel"}))
  assert result.description == "Generates a prompt to summarize the provided text."
  assert len(result.messages) == 2
  assert "You are a helpful assistant skilled at summarization" in result.messages[0].content.text
  assert "from Yom HaRashun until Yom HaShabiyah" in result.messages[1].content.text