from dotenv import load_dotenv
load_dotenv()
import main

class FakeDatastore:
  id_seq = 1000
  def __init__(self):
    self.store = {}

  def key(self, kind, id=None):
    if id is None:
      id = self.id_seq
      self.id_seq += 1
    return (kind, id)

  def get(self, key):
    return self.store.get(key)

  def put(self, entity):
    self.store[entity.key] = entity


main.app.testing = True
mockDatastore = FakeDatastore()
main.get_datastore_client = lambda: mockDatastore
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
