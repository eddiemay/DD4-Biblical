import os

gpt_model = "gpt-4.1"
deepseek_model = "deepseek-r1:7b"
model = gpt_model

if model.startswith("gpt"):
  if __name__ == "__main__":
    from langchain_openai import ChatOpenAI
    from dotenv import load_dotenv
    load_dotenv()
  api_key = os.environ.get("OPENAI_API_KEY")
  if api_key is None:
    print("Need to create an .env file and put OPENAI_API_KEY=[OPENAI_API_KEY]")
    print("For GCP create env_variables.yaml and follow instructions at "
          "https://stackoverflow.com/questions/22669528")
  llm = ChatOpenAI(model=model, api_key=api_key, use_responses_api=True)
else:
  from langchain_ollama import ChatOllama
  llm = ChatOllama(model=model)

llm.temperature = 0
llm_with_tools = llm.bind_tools([
  {
    "type": "mcp",
    "server_label": "dabar_dot_cloud",
    "server_url": "https://mcp-server-738844874589.us-central1.run.app/mcp",
    "require_approval": "never",
  }
])


def query(question, session_id=777, max_turns=7):
  return llm_with_tools.invoke(question)


if __name__ == "__main__":
  print(query("Fetch the scripture Gen 2:3"))
  print(query("How many references are there to Strong's H6963 in the bible?"))
  print(query("What is the root of Strong's H0430?"))
  # print(query("What is the similarity score of 'Hello there' and 'How are you?'"))
  # print(query("What is the similarity score of 'I like dogs' and 'I love dogs'"))
