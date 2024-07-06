import gradio
from pprint import pprint
from langchain.chains import RetrievalQA
from langchain_community.vectorstores import Chroma
from langchain_openai import ChatOpenAI
from langchain_openai import OpenAIEmbeddings

llm_name = "gpt-4o"
print(llm_name)

CHROMA_PATH = "chatbot/chroma/"
embedding = OpenAIEmbeddings()
vectordb = Chroma(persist_directory=CHROMA_PATH, embedding_function=embedding)

print(vectordb._collection.count())

question = "How many sons did Abraham have?"
docs = vectordb.similarity_search(question, k=5)
pprint(docs)

llm = ChatOpenAI(model_name=llm_name, temperature=0)

qa_chain = RetrievalQA.from_chain_type(llm, retriever=vectordb.as_retriever(), return_source_documents=True)

result = qa_chain({"query": question})

pprint(result["result"])
pprint(result["source_documents"])


# chat interface
def chat_function(question, history):
    response = qa_chain({"query": question})
    return response["result"]


# Set up the Gradio chat interface
iface = gradio.ChatInterface(
    fn=chat_function,
    title="Intelligent Search Assistant",
    description="This interface uses the bible to answer your questions.",
    theme="default")

iface.launch(share=False)