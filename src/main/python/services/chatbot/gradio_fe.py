import gradio
from dotenv import load_dotenv
load_dotenv()
from langgraph_chat import Agent, query, llm, prompt

agent = Agent(llm, prompt)


def chat_func(question, history):
  return query(agent, question)[-1]


if __name__ == "__main__":
  # Set up the Gradio chat interface
  iface = gradio.ChatInterface(
    fn=chat_func,
    title="Bible Search Assistant",
    description="This interface uses the bible to answer your questions.",
    theme="default")

  iface.launch(share=False)