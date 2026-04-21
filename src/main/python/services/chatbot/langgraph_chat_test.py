from dotenv import load_dotenv
load_dotenv()
from langgraph_chat import Agent

agent = Agent()

def _query(question):
  return agent(question)[-1]


def test_gen_2_3():
  assert "Elohim blessed the seventh day" in _query("What does Genesis 2:3 say?")
  assert len(agent.messages) == 2
  assert "What does Genesis 2:3 say?" in agent.messages[0]['content']
  assert "Elohim blessed the seventh day" in agent.messages[1]['content']


def test_exo_12_40():
  assert "four hundred and thirty years" in _query("""From Exodus 12:40\
      How long did the children sojourn in Egypt?""")
  assert len(agent.messages) == 4
  assert "What does Genesis 2:3 say?" in agent.messages[0]['content']
  assert "Elohim blessed the seventh day" in agent.messages[1]['content']
  assert "From Exodus 12:40" in agent.messages[2]['content']
  assert "four hundred and thirty years" in agent.messages[3]['content']


def test_sons_of_abraham():
  assert "eight" in _query("How many sons did Abraham have?")
  assert len(agent.messages) == 6


def test_sons_of_jabob():
  assert "twelve" in _query("How many sons did Jacob have?")
  assert len(agent.messages) == 8


def test_remembers_context():
  assert "eight" in _query("How many sons did Abraham have?")
  assert len(agent.messages) == 10
  assert "Terah" in _query("What was his father's name?")
  assert len(agent.messages) == 12
  assert "Haran" in _query("What was the name of his brother that died?")
  assert len(agent.messages) == 14
