import asyncio
import logging
import os

from dotenv import load_dotenv
from langchain_mcp_adapters.client import MultiServerMCPClient
from langgraph.prebuilt import create_react_agent

logger = logging.getLogger(__name__)
logging.basicConfig(format="[%(levelname)s]: %(message)s", level=logging.INFO)

gpt_model = "gpt-4.1"
deepseek_model = "deepseek-r1:7b"
model = gpt_model


load_dotenv()

async def interact_with_server():
  client = MultiServerMCPClient(
      {
        "dabar-mcp-server": {
          "command": "python",
          # Replace with absolute path to your math_server.py file
          "args": ["mcp_server/mcp_server.py"],
          "transport": "stdio",
        }
      }
  )
  tools = await client.get_tools()
  agent = create_react_agent(
      model,
      tools
  )
  math_response = await agent.ainvoke(
      {"messages": [{"role": "user", "content": "What is the similarity between 'I am Mexican' and 'I am a Mexican'"}]}
  )

if __name__ == "__main__":
  asyncio.run(interact_with_server())