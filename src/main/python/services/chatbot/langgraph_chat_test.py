from dotenv import load_dotenv
load_dotenv()
from langgraph_mcp_chat import query

def _query(question):
  return query(question, '777')[-1]


def test_gen_2_3():
  assert "Elohim blessed the seventh day" in _query("What does Genesis 2:3 say?")


def test_gen_12_40():
  assert "four hundred and thirty years" in _query("""From Exodus 12:40\
      How long did the children sojourn in Egypt?""")


def test_exo_12_1_3():
  assert "And יהוה spoke to Mosheh and to Aharon" in _query("What does Exo 12:1-3 say?")


def test_sons_of_abraham():
  assert "eight" in _query("How many sons did Abraham have?")


def test_sons_of_jabob():
  assert "twelve" in _query("How many sons did Jacob have?")


def test_children_of_jabob():
  assert "thirteen" in _query("How many children did he have?")
