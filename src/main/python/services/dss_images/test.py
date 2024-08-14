# Copyright 2018 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import main


def test_index():
  main.app.testing = True
  client = main.app.test_client()

  r = client.get("/")
  assert r.status_code == 200
  assert "DeadSea Scroll Images Service" in r.data.decode("utf-8")


def test_file_wanted():
  main.app.testing = True
  client = main.app.test_client()

  r = client.get("/?file=isaiah_9_0_2.jpg")
  assert r.status_code == 200
  assert r.headers["Content-Type"] == 'image/jpeg'


def test_not_found():
  main.app.testing = True
  client = main.app.test_client()

  r = client.get("/?file=non_existing.jpg")
  assert r.status_code == 404