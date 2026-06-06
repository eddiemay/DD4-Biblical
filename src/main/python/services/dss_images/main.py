import json
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from flask import Flask, request, jsonify
from google.cloud import datastore
from predict_letters import predict_letters

# If `entrypoint` is not defined in app.yaml, App Engine will look for an app
# called `app` in `main.py`.
app = Flask(__name__)

HEADERS = {"Access-Control-Allow-Origin": "*", 'Content-Type': 'application/json'}


def cors_enabled_function(request):
    # For more information about CORS and CORS preflight requests, see:
    # https://developer.mozilla.org/en-US/docs/Glossary/Preflight_request
    # Set CORS headers for the preflight request
    # Allows GET requests from any origin with the Content-Type
    # header and caches preflight response for an 3600s
    headers = {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST, GET, DELETE",
        "Access-Control-Allow-Headers": "Content-Type",
        "Access-Control-Max-Age": "3600",
    }
    return "", 204, headers

def get_datastore_client():
    return datastore.Client()


def create_letterbox(request, datastore_client, session):
    id, letter_box = read_entity(request)
    letter_box['x1'], letter_box['x2'], letter_box['y1'], letter_box['y2'] = \
        int(letter_box['x1']), int(letter_box['x2']), int(letter_box['y1']), int(letter_box['y2'])
    letter_box['coords'] = (
        json.dumps(letter_box['coords'])
        if letter_box.get('coords') is not None
        else None
    )

    entity = datastore.Entity(
        key=datastore_client.key("LetterBox", id) if id is not None
        else datastore_client.key("LetterBox"))
    username, now = session['username'], datetime.now(timezone.utc)
    letter_box['lastModifiedUsername'], letter_box['lastModifiedTime'] = username, now
    letter_box['creationUsername'] = letter_box.get('creationUsername', username)
    letter_box['creationTime'] = letter_box.get('creationTime', now)
    entity.update(letter_box)
    datastore_client.put(entity)
    letter_box['id'] = entity.key.id

    predict_letters([letter_box])

    letter_box['coords'] = (
        json.loads(letter_box['coords'])
        if letter_box.get('coords') is not None
        else None
    )
    return jsonify(letter_box), 200, HEADERS


def get_letterboxes(request):
    filename = request.args.get('filename')
    predict = request.args.get('predict')
    if not filename:
        return jsonify({"error": "filename is required"}), 400, HEADERS

    query = get_datastore_client().query(kind="LetterBox")
    query.add_filter("filename", "=", filename)
    results = list(query.fetch())
    items = []
    for r in results:
        item = dict(r)
        item['id'] = r.key.id
        coords = item.get('coords')
        if isinstance(coords, str):
            item['coords'] = json.loads(coords)
        items.append(item)

    # Sort: y2 ASC, x2 DESC
    items = sorted(items, key=lambda r: (r.get('y2', 0), -r.get('x2', 0)))
    if predict:
        predict_letters(items)

    result = {
        'type': 'LetterBox',
        'filter': f'filename={filename}',
        'orderBy': 'y2,x2 DESC',
        'pageSize': 0,
        'pageToken': 1,
        'totalSize': len(results),
        'items': items
    }
    return jsonify(result), 200, HEADERS


@app.route("/letterboxes", methods=["POST", "GET", "DELETE", "OPTIONS"])
def letterboxes():
    if request.method == 'OPTIONS':
        return cors_enabled_function(request)
    elif request.method == 'GET':
        return get_letterboxes(request)

    datastore_client = get_datastore_client()
    id_token = request.args.get('idToken')
    headers = {"Access-Control-Allow-Origin": "*", 'Content-Type': 'application/json'}

    session = resolve_login(datastore_client, id_token)
    if session is None:
        return jsonify({'error': 'Not Authenticated'}), 401, headers

    if request.method == 'DELETE':
        return delete_letterbox(request, datastore_client)

    return create_letterbox(request, datastore_client, session)


def delete_letterbox(request, datastore_client):
    id = request.args.get('id')
    if not id:
        return jsonify({"error": "id required"}), 400

    datastore_client.delete(datastore_client.key("LetterBox", int(id)))
    return jsonify({"result": True}), 200, HEADERS


def resolve_login(datastore_client, id_token):
    if id_token is None or len(id_token) == 0:
        return None

    key = datastore_client.key("Session", id_token)
    session = datastore_client.get(key = key)
    if session is None or session['state'] != 'ACTIVE':
        return None

    now = datetime.now(timezone.utc)
    if session['expTime'] < now:
        entity = datastore.Entity(key = key)
        session['endTime'], session['state'] = now, 'CLOSED'
        entity.update(session)
        datastore_client.put(entity)
        return None

    return session


def read_entity(request):
    entity = request.get_json()
    id = entity.get('id')
    entity = {
        k: v for k, v in entity.items()
        if not k.startswith('_') and k != 'id'
    }
    for key in list(entity.keys()):
        if key.endswith(('Time', 'Date')):
            parse_time(entity, key)

    return id, entity


def parse_time(entity, prop):
    time = entity.get(prop)

    if isinstance(time, str):
        time = parsedate_to_datetime(time)
    elif isinstance(time, datetime) and time.tzinfo is None:
        # Ensure timezone awareness
        time = time.replace(tzinfo=timezone.utc)

    entity[prop] = time