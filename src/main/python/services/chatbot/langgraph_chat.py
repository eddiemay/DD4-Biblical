import json
import os
import re
import urllib
from openai import OpenAI
from urllib import request

llm_name = "gpt-4o"
SEARCH_URL = 'https://dd4-biblical.appspot.com/_api/scriptures/v1/fetch?searchText={}&lang=en&version=ISR'

client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))


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
    completion = client.chat.completions.create(
      model=llm_name,
      temperature=0,
      messages=self.messages)
    return completion.choices[0].message.content


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
  search_url = SEARCH_URL.format(urllib.parse.quote(reference))
  print('Sending request: ', search_url)
  with request.urlopen(search_url) as url:
    response = json.load(url)
    print('Response: ', response)
    scriptures = response['items']
    return scriptures


known_actions = {
  "fetch_scripture": fetch_scripture
}

# python regular expression to selection action
action_re = re.compile('^Action: (\w+): (.*)$')


def query(question, max_turns=7):
  i = 0
  results = []
  bot = Agent(prompt)
  next_prompt = question
  while i < max_turns:
    i += 1
    result = bot(next_prompt)
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
