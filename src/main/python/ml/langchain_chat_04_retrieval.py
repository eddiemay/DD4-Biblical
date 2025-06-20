from langchain_community.vectorstores import Chroma
from pprint import pprint
from langchain_ollama import ChatOllama
from langchain_chat_03_vectorize import CHROMA_PATH, embedding, model

vectordb = Chroma(
    persist_directory=CHROMA_PATH,
    embedding_function=embedding
)

llm = ChatOllama(model=model, temperature=0)

if __name__ == '__main__':
    print(vectordb._collection.count())

    question = "How many years did Adam live?"

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

    from langchain.retrievers.self_query.base import SelfQueryRetriever
    from langchain.chains.query_constructor.base import AttributeInfo

    metadata_field_info = [
        AttributeInfo(
            name="book",
            description="The scripture the chunk is from, should be one of `Genesis` or `Exodus`",
            type="string",
        )
    ]

    document_content_description = "Bible scriptures"
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