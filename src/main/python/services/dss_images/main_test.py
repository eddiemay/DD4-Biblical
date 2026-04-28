import main
import datetime
from types import SimpleNamespace


def test_read_entity():
  request = SimpleNamespace()
  request.get_json = lambda: {'id': '45645513', 'x1': 45, 'x2': 55, 'y1': 456, 'y2': 487, 'creationTime': 'Mon, 27 Apr 2026 00:07:54 GMT'}
  id, entity = main.read_entity(request)

  assert id == '45645513'
  assert entity.get('id') is None
  assert entity['x1'] == 45
  assert entity['x2'] == 55
  assert entity['y1'] == 456
  assert entity['y2'] == 487
  assert type(entity['creationTime']) == datetime.datetime