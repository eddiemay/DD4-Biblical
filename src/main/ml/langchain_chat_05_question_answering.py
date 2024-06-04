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

question = "How old was Abraham when Ishmael was born?"
docs = vectordb.similarity_search(question, k=5)
pprint(docs)

llm = ChatOpenAI(model_name=llm_name, temperature=0)

qa_chain = RetrievalQA.from_chain_type(llm, retriever=vectordb.as_retriever(), return_source_documents=True)

result = qa_chain({"query": question})

pprint(result["result"])
pprint(result["source_documents"])