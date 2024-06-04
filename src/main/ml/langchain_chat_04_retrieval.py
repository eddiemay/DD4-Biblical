from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
from pprint import pprint

CHROMA_PATH = "chatbot/chroma/"

embedding = OpenAIEmbeddings()
vectordb = Chroma(
    persist_directory=CHROMA_PATH,
    embedding_function=embedding
)

print(vectordb._collection.count())

question = "what did they say about four hundred years?"

docs = vectordb.similarity_search(question, k=3)
pprint(docs)
for d in docs:
    print(d.metadata['reference'])

print("")
docs = vectordb.similarity_search(
    question,
    k=3,
    filter={"book":"Genesis"})
pprint(docs)

for d in docs:
    print(d.metadata['reference'])

from langchain_openai import OpenAI
from langchain.retrievers.self_query.base import SelfQueryRetriever
from langchain.chains.query_constructor.base import AttributeInfo

metadata_field_info = [
    AttributeInfo(
        name="book",
        description="The scripture the chunk is from, should be one of `Genesis`, or `Exodus`",
        type="string",
    )
]

document_content_description = "Bible scriptures"
llm = OpenAI(model='gpt-3.5-turbo-instruct', temperature=0)
retriever = SelfQueryRetriever.from_llm(
    llm,
    vectordb,
    document_content_description,
    metadata_field_info,
    verbose=True
)

print("")
docs = retriever.invoke(question)
pprint(docs)
for d in docs:
    print(d.metadata)