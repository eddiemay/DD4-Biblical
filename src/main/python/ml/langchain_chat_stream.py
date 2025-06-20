import gradio as gr
from langchain_ollama import ChatOllama
from langchain_chat_03_vectorize import model

# Ensure Ollama is running and serves the API endpoint
# Replace with your Ollama API URL and model name if different
ollama_api_url = "http://localhost:11434/v1/"
# Optional: Add your Ollama API token if required
# ollama_token = "your_ollama_token"

llm = ChatOllama(
    model = model,
    temperature = 0.8,
    # num_predict = 256,
    # other params ...
)

# Define the chatbot function
async def chatbot_function(user_input, history):
  # Stream the chatbot response
  full = ""
  async for chunk in llm.astream([("human", user_input)]):
    full += chunk.content
    # print(full)
    yield full


# Create the Gradio chatbot interface
if __name__ == "__main__":
  print(chatbot_function("How long did Adam live?", ""))
  with gr.ChatInterface(
      chatbot_function,
      title="ChatLLaMA Stream with Gradio"
  ) as demo:
    demo.launch()
