import json
import os
import re
from datetime import datetime, timezone
from langchain_openai import ChatOpenAI
from urllib import parse, request

from typing_extensions import final

gpt_model = "gpt-4.1"
deepseek_model = "deepseek-r1:32b"
model = gpt_model
MAX_TURNS = 7
FETCH_URL =\
  'https://dabar.cloud/_api/scriptures/v1/fetch?searchText={}&lang=en&version=ISR'

prompt = """
You run in a loop of Thought, Action, PAUSE, Observation.
At the end of the loop you output an Answer
Use Thought to describe your thoughts about the question you have been asked.
Use Action to run one of the actions available to you - then return PAUSE.
Observation will be the result of running those actions.

Your available actions are:

fetch_scripture:
e.g. fetch_scripture: Genesis 2:3
returns json array of scriptures requested.

Example session:

Question: What does Genesis 2:3 say?
Thought: I should fetch the scripture using fetch_scripture
Action: fetch_scripture: Genesis 2:3
PAUSE

You will be called again with this:

Observation: And Elohim blessed the seventh day and set it apart, because on it He rested from all His work which Elohim in creating had made.

You then output:

Answer: And Elohim blessed the seventh day and set it apart, because on it He rested from all His work which Elohim in creating had made.
""".strip()

if model.startswith("gpt"):
  api_key = os.environ.get("OPENAI_API_KEY")
  if api_key is None:
    print("Need to create an .env file and put OPENAI_API_KEY=[OPENAI_API_KEY]")
    print("For GCP create env_variables.yaml and follow instructions at " 
          "https://stackoverflow.com/questions/22669528")
  llm = ChatOpenAI(model=model, api_key=api_key)
else:
  from langchain_ollama import ChatOllama
  llm = ChatOllama(model=model)

llm.temperature = 0


class Agent:
  def __init__(self, system=prompt, ip_address=None, creation_time=None, last_modified_time=None, messages=None):
    self.system = system
    self.ip_address = ip_address
    self.creation_time = creation_time or datetime.now(timezone.utc)
    self.last_modified_time = last_modified_time
    self.messages = messages or []
    if not self.messages and self.system:
      self.messages.append({"role": "system", "content": system})

  def to_dict(self):
    self.last_modified_time = datetime.now(timezone.utc)
    return {
      "system": self.system,
      "ipAddress": self.ip_address,
      "creationTime": self.creation_time,
      "lastModifiedTime": self.last_modified_time,
      "messages": self.messages
    }

  @classmethod
  def from_dict(cls, data):
    return cls(
        system=data.get("system"),
        ip_address=data.get("ipAddress"),
        creation_time=data.get("creationTime"),
        last_modified_time=data.get("lastModifiedTime"),
        messages=data.get("messages", [])
    )

  def __call__(self, message, store=True):
    if store:
      self.messages.append({"role": "user", "content": message})

    result = self.execute()

    if store:
      self.messages.append({"role": "assistant", "content": result})

    return result

  def trim_messages(self, max_messages=20):
    if len(self.messages) > max_messages:
      self.messages = (
          [self.messages[0]] +  # keep system
          self.messages[-(max_messages - 1):]
      )

  def execute(self):
    return llm.invoke(self.messages).content


def fetch_scripture(reference):
  fetch_url = FETCH_URL.format(parse.quote(reference))
  print('Sending request: ', fetch_url)
  with request.urlopen(fetch_url, timeout=10) as url:
    response = json.load(url)
    # print('Response: ', response)
    scriptures = response.get('items', [])
    return scriptures[:7]


known_actions = {
  "fetch_scripture": fetch_scripture
}

# python regular expression to selection action
action_re = re.compile(r'^Action:\s*(\w+):\s*(.*)$')


def query(agent:Agent, question:str) -> list[str]:
  i = 0
  results = []
  next_prompt = question
  final_answer = None
  while i < MAX_TURNS:
    i += 1

    result = agent(next_prompt, store=(i==1))
    results.append(result)
    print(result)
    actions = []
    for line in result.split('\n'):
      line = line.strip()
      match = action_re.match(line)
      if match:
        actions.append(match)
    if actions:
      # There is an action to run
      action, action_input = actions[0].groups()
      if action not in known_actions:
        raise Exception(f"Unknown action: {action}: {action_input}")
      print(f" -- running {action} {action_input}")
      try:
        observation = known_actions[action](action_input)
      except Exception as e:
        observation = f"Error: {str(e)}"
      print("Observation:", observation)
      next_prompt = f"Observation: {observation}"
    if "Answer:" in result:
      final_answer = result
      break
  print()
  agent.messages.append({"role": "user", "content": question})
  agent.messages.append({"role": "assistant", "content": final_answer})
  agent.trim_messages()
  return results
