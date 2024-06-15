import gradio
from pprint import pprint
from langchain.chains import ConversationalRetrievalChain
from langchain.chains import RetrievalQA
from langchain.memory import ConversationBufferMemory
from langchain.prompts import PromptTemplate
from langchain_community.vectorstores import Chroma
from langchain_openai import ChatOpenAI
from langchain_openai import OpenAIEmbeddings

llm_name = "gpt-4o"
CHROMA_PATH = "chatbot/chroma/"

embedding = OpenAIEmbeddings()
vectordb = Chroma(persist_directory=CHROMA_PATH, embedding_function=embedding)

question = "How many children did Abraham have?"
docs = vectordb.similarity_search(question,k=3)
pprint(docs)

llm = ChatOpenAI(model_name=llm_name, temperature=0)
llm.invoke("Hello world!")

template = """Use the following pieces of context to answer the question at the end. If you don't know the answer, just say that you don't know, don't try to make up an answer. Use three sentences maximum. Always say "thanks for asking!" at the end of the answer. 
{context}
Question: {question}
Helpful Answer:"""
QA_CHAIN_PROMPT = PromptTemplate(input_variables=["context", "question"],template=template,)

memory = ConversationBufferMemory(
    memory_key="chat_history",
    return_messages=True)

qa_chain = RetrievalQA.from_chain_type(
    llm,
    retriever=vectordb.as_retriever(),
    memory=memory,
    chain_type_kwargs={"prompt": QA_CHAIN_PROMPT})


result = qa_chain({"query": question})
print(result["result"])

memory = ConversationBufferMemory(
    memory_key="chat_history",
    return_messages=True)

retriever=vectordb.as_retriever()
qa = ConversationalRetrievalChain.from_llm(
    llm,
    retriever=retriever,
    memory=memory
)

result = qa({"question": question})
pprint(result['answer'])

result = qa({"question": "What are the names of his children?"})
pprint(result['answer'])

question = "How many wives did he have?"
result = qa({"question": question})
pprint(result['answer'])


# chat interface
def chat_function_qa_chain(question, history):
    response = qa_chain({"query": question})
    return response["result"]


# chat interface
def chat_function_llm(question, history):
    response = qa({"question": question})
    return response["answer"]


# Set up the Gradio chat interface
iface = gradio.ChatInterface(
    fn=chat_function_qa_chain,
    title="Intelligent Search Assistant",
    description="This interface uses the bible to answer your questions.",
    theme="default")

iface.launch(share=False)