import json
import os
import re
from langchain_openai import ChatOpenAI
from urllib import parse, request

gpt_model = "gpt-4.1"
deepseek_model = "deepseek-r1:32b"
model = gpt_model
FETCH_URL =\
  'https://dabar.cloud/_api/scriptures/v1/fetch?searchText={}&lang=en&version=ISR'

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
  def __init__(self, system=""):
    self.system = system
    self.messages = []
    if self.system:
      self.messages.append({"role": "system", "content": system})

  def __call__(self, message):
    self.messages.append({"role": "user", "content": message})
    result = self.execute()
    self.messages.append({"role": "assistant", "content": result})
    return result

  def execute(self):
    return llm.invoke(self.messages).content


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


def fetch_scripture(reference):
  fetch_url = FETCH_URL.format(parse.quote(reference))
  print('Sending request: ', fetch_url)
  with request.urlopen(fetch_url) as url:
    response = json.load(url)
    # print('Response: ', response)
    scriptures = response['items']
    return scriptures


known_actions = {
  "fetch_scripture": fetch_scripture
}

# python regular expression to selection action
action_re = re.compile('^Action: (\w+): (.*)$')
agents = {}


def query(question, session_id, max_turns=7):
  i = 0
  results = []
  next_prompt = question
  while i < max_turns:
    i += 1
    agent = agents.get(session_id)
    if agent is None:
      print('\nStarting new session: ', session_id)
      agent = Agent(prompt)
      agents[session_id] = agent

    result = agent(next_prompt)
    results.append(result)
    print(result)
    actions = [
      action_re.match(a)
      for a in result.split('\n')
      if action_re.match(a)
    ]
    if actions:
      # There is an action to run
      action, action_input = actions[0].groups()
      if action not in known_actions:
        raise Exception("Unknown action: {}: {}".format(action, action_input))
      print(" -- running {} {}".format(action, action_input))
      observation = known_actions[action](action_input)
      print("Observation:", observation)
      next_prompt = "Observation: {}".format(observation)
    else:
      print()
      return results
