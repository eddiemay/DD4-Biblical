from flask import Flask, request
from google.cloud import datastore
from langgraph_chat import Agent, query, llm, prompt

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

    return chat(request), 200, headers


def get_datastore_client():
  return datastore.Client()


def chat(request):
    datastore_client = get_datastore_client()

    question = request.args.get('question')
    session_id = request.args.get('sessionId')

    # 🔍 Get client IP (Cloud Run compatible)
    ip_address = (
        request.headers.get("X-Forwarded-For", "").split(",")[0]
        or request.remote_addr)

    print(f"session_id: {session_id} question: {question} ip: {ip_address}")

    agent = get_agent(datastore_client, session_id, ip_address)

    # 🧠 Get model answer
    answer = query(agent, question)

    # 🗃️ Save to Datastore
    save_agent(datastore_client, session_id, agent)

    return answer


def get_agent(datastore_client:datastore.Client, session_id:str, ip_address):
  entity = datastore_client.get(
      datastore_client.key("ChatSession", session_id))

  if entity:
    return Agent.from_dict(llm, entity)
  else:
    return Agent(llm, prompt, ip_address=ip_address)


def save_agent(datastore_client:datastore.Client, session_id:str, agent:Agent):
  entity = datastore.Entity(
      key=datastore_client.key("ChatSession", session_id),
      exclude_from_indexes=("messages",))
  entity.update(agent.to_dict())
  datastore_client.put(entity)

