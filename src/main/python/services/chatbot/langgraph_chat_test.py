from dotenv import load_dotenv
load_dotenv()
import langgraph_chat

def query(question):
  return langgraph_chat.query(question, '777')[-1]


def test_gen_2_3():
  assert "Elohim blessed the seventh day" in query("What does Genesis 2:3 say?")


def test_gen_12_40():
  assert "four hundred and thirty years" in query("""From Exodus 12:40\
      How long did the children sojourn in Egypt?""")


def test_exo_12_1_3():
  assert "And יהוה spoke to Mosheh and to Aharon" in query("What does Exo 12:1-3 say?")


def test_sons_of_abraham():
  assert "eight" in query("How many sons did Abraham have?")


def test_sons_of_jabob():
  assert "twelve" in query("How many sons did Jacob have?")


def test_children_of_jabob():
  assert "thirteen" in query("How many children did he have?")
