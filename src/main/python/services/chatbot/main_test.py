import main

main.app.testing = True
client = main.app.test_client()


def test_index():
  response = client.get(
      "/?sessionId=777&question=How%20many%20sons%20did%Jacob%20have")
  assert response.status_code == 200
  assert "twelve" in response.data.decode("utf-8")


def test_follow_up():
  response = client.get(
    "/?sessionId=777&question=How%20many%20children%20did%20he%20have")
  assert response.status_code == 200
  assert "thirteen" in response.data.decode("utf-8")
