import gradio
from dotenv import load_dotenv
load_dotenv()
import langgraph_chat


def chat_func(question, history):
  return langgraph_chat.query(question, '7')[-1]


if __name__ == "__main__":
  # Set up the Gradio chat interface
  iface = gradio.ChatInterface(
    fn=chat_func,
    title="Bible Search Assistant",
    description="This interface uses the bible to answer your questions.",
    theme="default")

  iface.launch(share=False)
