import main


def test_index():
  main.app.testing = True
  client = main.app.test_client()

  r = client.get("/?question=How%20many%20sons%20did%20Abraham%20have")
  assert r.status_code == 200
  assert "eight" in r.data.decode("utf-8")
