import gradio
from pprint import pprint
from langchain.chains import RetrievalQA
from langchain.memory import ConversationBufferMemory
from langchain.prompts import PromptTemplate
from langchain_chat_04_retrieval import llm, vectordb
template = """Use the following pieces of context to answer the question at the end.
Use metadata to determine the bible book, chapter and verse.
If you don't know the answer, just say that you don't know, don't try to make up an answer.
Do not use your own knowledge if the information in not in the context, only
answer questions from the context. If the answer is not in the context say:
'I am only trained on the Torah, can only answer questions from the Torah'
Thought review act
{context}
Question: {question}
Helpful Answer:"""
QA_CHAIN_PROMPT = PromptTemplate(input_variables=["context", "question"], template=template)

memory = ConversationBufferMemory(memory_key="chat_history", output_key="result", return_messages=True)

qa_chain = RetrievalQA.from_chain_type(
    llm,
    retriever=vectordb.as_retriever(),
    memory=memory,
    return_source_documents=True,
    chain_type_kwargs={"prompt": QA_CHAIN_PROMPT})


# chat interface
def chat_function(question, history):
    response = qa_chain.invoke({"query": question})
    result = response['result']
    references = '\n'.join(doc.metadata['reference'] for doc in response['source_documents'])
    return f"Answer: {result}\n\nReferences:\n{references}"


pprint(chat_function("How long did Adam live before he died?", ""))
pprint(chat_function("How sons did Abraham have?", ""))
# pprint(chat_function("How long did Jacob serve for Rachel?", ""))
# pprint(chat_function("In what source documents do we learn about clean and unclean animals?", ""))
# pprint(chat_function("What does source document Genesis 2:3 say?", ""))

# Set up the Gradio chat interface
iface = gradio.ChatInterface(
    fn=chat_function,
    title="Bible Search Assistant",
    description="This interface uses the bible to answer your questions.",
    theme="default")

iface.launch(share=False)
