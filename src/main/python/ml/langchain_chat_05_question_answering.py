import gradio
from pprint import pprint
from langchain.chains import RetrievalQA
from langchain_chat_04_retrieval import llm, vectordb

print(vectordb._collection.count())

question = "How many sons did Abraham have?"
docs = vectordb.similarity_search(question, k=5)
pprint(docs)

qa_chain = RetrievalQA.from_chain_type(llm, retriever=vectordb.as_retriever(), return_source_documents=True)

result = qa_chain({"query": question})

pprint(result["result"])
pprint(result["source_documents"])


# chat interface
def chat_function(question, history):
    print('got here')
    response = qa_chain({"query": question})
    print('done')
    return response["result"]


# Set up the Gradio chat interface
iface = gradio.ChatInterface(
    fn=chat_function,
    title="Intelligent Search Assistant",
    description="This interface uses the bible to answer your questions.",
    theme="default")

iface.launch(share=False)