import langgraph_chat


def test_gen_2_3():
  assert "Elohim blessed the seventh day" in langgraph_chat.query("""What does Genesis 2:3 say?""")


def test_gen_12_40():
  assert "four hundred and thirty years" in langgraph_chat.query("""From Exodus 12:40\
      How long did the children sojourn in Egypt?""")


def test_exo_12_1_3():
  assert "And יהוה spoke to Mosheh and to Aharon" in langgraph_chat.query("""What does Exo 12:1-3 say?""")


def test_sons_of_abraham():
  assert "eight" in langgraph_chat.query("How many sons did Abraham have?")