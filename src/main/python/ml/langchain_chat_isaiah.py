import gradio as gr
import wikipedia
from langchain.agents.output_parsers import OpenAIFunctionsAgentOutputParser
from langchain.prompts import ChatPromptTemplate
from langchain.pydantic_v1 import BaseModel, Field
from langchain_community.chat_models import ChatOpenAI
from langchain_community.tools import tool
from langchain_community.tools import DuckDuckGoSearchRun
from langchain_community.tools.render import format_tool_to_openai_function


@tool
def search_wikipedia(query: str) -> str:
    """Run Wikipedia search and get page summaries."""
    page_titles = wikipedia.search(query)
    summaries = []
    for page_title in page_titles[: 3]:
        try:
            wiki_page =  wikipedia.page(title=page_title, auto_suggest=False)
            summaries.append(f"Page: {page_title}\nSummary: {wiki_page.summary}")
        except (wikipedia.wiki_client.exceptions.PageError, wikipedia.wiki_client.exceptions.DisambiguationError):
            pass
        if not summaries:
            return "No good Wikipedia Search Result was found"
        return "\n\n".join(summaries)


# 6 Define the input schema
class DuckWrapperInput(BaseModel):
    query: str = Field(..., description="Useful for when you need to answer medical or Pharmalogical questions.")


@tool(args_schema=DuckWrapperInput)
def duck_wrapper(query: str) -> str:
    "Run DuckDuckGo Search for medical questions and get results."
    # Instantiate the DuckDuckGoSearchRun tool
    ddg_search = DuckDuckGoSearchRun()
    # Run the search with the modified query
    search_results = ddg_search.run(f"site:webmd.com {query}")
    return search_results


# 8 add tools to variable for future loops
tools = [duck_wrapper, search_wikipedia]

# 9 add function formatting for openai
from langchain.agents.format_scratchpad import format_to_openai_functions

# 10 assign the tool functions to openai function
# call the model for use
# add the propmpt with memory for the chat and the ai actions
from langchain.prompts import MessagesPlaceholder
functions = [format_tool_to_openai_function(f) for f in tools]
model = ChatOpenAI(temperature=0).bind(functions=functions)
prompt = ChatPromptTemplate.from_messages([
    ("system", "You are helpful but sassy assistant"),
    MessagesPlaceholder(variable_name="chat_history"),
    ("user", "{input}"),
    MessagesPlaceholder(variable_name="agent_scratchpad")
])

# 11 create the chain the connects the prompt, the model which is bound to the function tools, and the output parser
chain = prompt | model | OpenAIFunctionsAgentOutputParser()

# 12 import runnable and adding runnable to format output to OpenAI compatible json
# the runnable passes data through the class to create the agent chain
from langchain.schema.runnable import RunnablePassthrough
agent_chain = RunnablePassthrough.assign(
    agent_scratchpad= lambda x: format_to_openai_functions(x["intermediate_steps"])
) | prompt | model | OpenAIFunctionsAgentOutputParser()

# 13 import memory buffery object to keep a list of messages in memory and line up the memory key with list of messages in placeholer
from langchain.memory import ConversationBufferMemory
memory = ConversationBufferMemory(return_messages=True,memory_key="chat_history")

# 14 import refined agent executor
from langchain.agents import AgentExecutor
agent_executor = AgentExecutor(agent=agent_chain, tools=tools, verbose=True, memory=memory)

# 15 test script
agent_executor.invoke({"input": "my name is "})


@tool
def bible_wrapper(query: str) -> str:
    """Run DuckDuckGo Search for biblical questions and get results."""
    # Instantiate the DuckDuckGoSearchRun tool
    ddg_search = DuckDuckGoSearchRun()
    # Run the search with the modified query
    search_results = ddg_search.run(f"site:biblegateway.com {query}")
    return search_results


# chat interface
def chat_function(message, history):
    result = agent_executor.invoke({"input": message})
    return result["output"]


# Set up the Gradio chat interface
iface = gr.ChatInterface(
    fn=chat_function,
    title="Intelligent Search Assistant",
    description="This interface uses Wikipedia and DuckDuckGo to answer your questions.",
    theme="default")

iface.launch(share=True)


# 17 Define the Gradio interface with the convchain function
def chat_function(message):
    result = agent_executor.invoke({"input": message})
    return result["output"]


# 18 Set up the Gradio interface
iface = gr.Interface(
    fn=chat_function,
    inputs=gr.Textbox(lines=2, placeholder="Ask me anything!"),
    outputs="text",
    title="Intelligent Search Assistant",
    description="I am here to answer all your bible Questions.",
    theme="default",
    allow_flagging="never",
    live=True)

iface.launch(share=True)