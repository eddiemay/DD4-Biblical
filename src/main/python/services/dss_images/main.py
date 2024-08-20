# Copyright 2018 Google LLC
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

# [START gae_python38_app]
# [START gae_python3_app]
from flask import Flask, request

# If `entrypoint` is not defined in app.yaml, App Engine will look for an app
# called `app` in `main.py`.
app = Flask(__name__)


@app.route("/")
def cors_enabled_function():
    # For more information about CORS and CORS preflight requests, see:
    # https://developer.mozilla.org/en-US/docs/Glossary/Preflight_request

    # Set CORS headers for the preflight request
    if request.method == "OPTIONS":
        # Allows GET requests from any origin with the Content-Type
        # header and caches preflight response for an 3600s
        headers = {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Methods": "GET",
            "Access-Control-Allow-Headers": "Content-Type",
            "Access-Control-Max-Age": "3600",
        }

        return "", 204, headers

    # Set CORS headers for the main request
    headers = {"Access-Control-Allow-Origin": "*"}

    return fetch(request, headers)


def fetch(request, headers):
    file = request.args.get('file')
    if file is None:
        return "DeadSea Scroll Images Service"

    try:
        # Opening the binary file in binary mode as rb(read binary)
        f = open("images/isaiah/{}".format(file), mode="rb")

        # Reading file data with read() method
        data = f.read()

        # Printing our byte sequenced data
        print(data)

        # Closing the opened file
        f.close()

        headers["Content-Type"] = 'image/jpeg'
        return data, 200, headers
    except OSError:
        print("Could not open/read file:", file)
        return "File not found", 404, headers
