import langgraph_chat
from datetime import datetime
from flask import Flask, request
from google.cloud import datastore

# If `entrypoint` is not defined in app.yaml, App Engine will look for an app
# called `app` in `main.py`.
app = Flask(__name__)

datastore_client = datastore.Client()


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

    return chat(request), 200, headers


def chat(request):
    question = request.args.get('question')
    session_id = request.args.get('sessionId')

    # 🔍 Get client IP (Cloud Run compatible)
    ip_address = (
        request.headers.get("X-Forwarded-For", "").split(",")[0]
        or request.remote_addr
    )

    print(f"session_id: {session_id} question: {question} ip: {ip_address}")

    # 🧠 Get model answer
    answer = langgraph_chat.query(question, session_id)

    # 🗃️ Save to Datastore
    log_chat_interaction(
        question=question,
        answer=answer,
        session_id=session_id,
        ip_address=ip_address,
    )

    return answer


def log_chat_interaction(question:str, answer:str, session_id:str, ip_address:str):
  entity = datastore.Entity(
      key=datastore_client.key("ChatInteraction"),
      exclude_from_indexes=("answer",))

  entity.update({
    "question": question,
    "answer": answer,
    "sessionId": session_id,
    "ipAddress": ip_address,
    "timestamp": datetime.utcnow(),
  })

  datastore_client.put(entity)

