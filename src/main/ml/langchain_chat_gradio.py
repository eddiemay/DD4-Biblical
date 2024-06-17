import gradio
from pprint import pprint
from langchain.chains import RetrievalQA
from langchain.memory import ConversationBufferMemory
from langchain.prompts import PromptTemplate
from langchain_community.vectorstores import Chroma
from langchain_openai import ChatOpenAI
from langchain_openai import OpenAIEmbeddings

llm_name = "gpt-4o"
CHROMA_PATH = "chatbot/chroma/"
template = """Use the following pieces of context to answer the question at the end. If you don't know the answer, just say that you don't know, don't try to make up an answer. Use three sentences maximum. Always say "thanks for asking!" at the end of the answer. 
{context}
Question: {question}
Helpful Answer:"""
QA_CHAIN_PROMPT = PromptTemplate(input_variables=["context", "question"],template=template)

vectordb = Chroma(persist_directory=CHROMA_PATH, embedding_function=OpenAIEmbeddings())
memory = ConversationBufferMemory(
    memory_key="chat_history",
    output_key="result",
    return_messages=True)

qa_chain = RetrievalQA.from_chain_type(
    ChatOpenAI(model_name=llm_name, temperature=0),
    retriever=vectordb.as_retriever(),
    memory=memory,
    return_source_documents=True,
    chain_type_kwargs={"prompt": QA_CHAIN_PROMPT})


# chat interface
def chat_function(question, history):
    response = qa_chain({"query": question})
    return '{}\n\nReferences:{}'.format(
        response["result"],
        "".join("\n{} {}".format(doc.metadata['reference'], doc.page_content) for doc in response["source_documents"]))


pprint(chat_function("How long did Jacob serve for Rachel?", ""))

# Set up the Gradio chat interface
iface = gradio.ChatInterface(
    fn=chat_function,
    title="Bible Search Assistant",
    description="This interface uses the bible to answer your questions.",
    theme="default").launch(share=True)
