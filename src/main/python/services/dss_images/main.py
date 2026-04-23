from flask import Flask, request, jsonify
from google.cloud import datastore

# If `entrypoint` is not defined in app.yaml, App Engine will look for an app
# called `app` in `main.py`.
app = Flask(__name__)


def cors_enabled_function(method):
    # For more information about CORS and CORS preflight requests, see:
    # https://developer.mozilla.org/en-US/docs/Glossary/Preflight_request

    # Set CORS headers for the preflight request
    # Allows GET requests from any origin with the Content-Type
    # header and caches preflight response for an 3600s
    headers = {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": f"{method}, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type",
        "Access-Control-Max-Age": "3600",
    }

    return "", 204, headers


def get_datastore_client():
    return datastore.Client()


@app.route("/create_letterbox", methods=["POST", "OPTIONS"])
def create_letterbox():
    if request.method == "OPTIONS":
        return cors_enabled_function('POST')

    datastore_client = get_datastore_client()

    letter_box = request.get_json()

    id = letter_box.get('id')
    letter_box = {
        k: v for k, v in letter_box.items()
        if not k.startswith('_') and k != 'id'
    }
    entity = datastore.Entity(
        key=datastore_client.key("LetterBox", id) if id is not None else datastore_client.key("LetterBox"))
    entity.update(letter_box)
    datastore_client.put(entity)
    letter_box['id'] = entity.key.id

    from predict_letters import predict_letters
    predict_letters([letter_box])

    headers = {"Access-Control-Allow-Origin": "*", 'Content-Type': 'application/json'}
    return jsonify(letter_box), 200, headers


@app.route("/letterboxes")
def letterboxes():
    if request.method == "OPTIONS":
        return cors_enabled_function('GET')

    datastore_client = get_datastore_client()

    filename = request.args.get('filename')
    predict = request.args.get('predict')
    if not filename:
        return jsonify({"error": "filename is required"}), 400

    query = datastore_client.query(kind="LetterBox")
    query.add_filter("filename", "=", filename)
    results = list(query.fetch())
    items = []
    for r in results:
        item = dict(r)
        item['id'] = r.key.id
        items.append(item)
    # Sort: y2 ASC, x2 DESC
    items = sorted(items, key=lambda r: (r.get('y2', 0), -r.get('x2', 0)))
    if predict:
      from predict_letters import predict_letters
      predict_letters(items)

    headers = {"Access-Control-Allow-Origin": "*", 'Content-Type': 'application/json'}
    result = {
        'type': 'LetterBox',
        'filter': f'filename={filename}',
        'orderBy': 'y2,x2 DESC',
        'pageSize': 0,
        'pageToken': 1,
        'totalSize': len(results),
        'items': items
    }
    return jsonify(result), 200, headers
