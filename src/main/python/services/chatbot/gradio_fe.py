import gradio
import langgraph_chat


def chat_func(question, history):
  return langgraph_chat.query(question)[-1]

# Set up the Gradio chat interface
iface = gradio.ChatInterface(
  fn=langgraph_chat.query,
  title="Bible Search Assistant",
  description="This interface uses the bible to answer your questions.",
  theme="default")

iface.launch(share=False)
