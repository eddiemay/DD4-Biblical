import openai
import json
import os
from langchain_openai import OpenAIEmbeddings
from langchain_community.document_loaders import JSONLoader
from langchain_community.vectorstores import Chroma
from pathlib import Path
from pprint import pprint
from urllib import request

DATA_PATH = "chatbot/bibleCache/"
CHROMA_PATH = "chatbot/chroma/"
BOOK_INFO_URL = 'https://dd4-biblical.appspot.com/_api/books/v1/get?id={}'
SEARCH_URL = 'https://dd4-biblical.appspot.com/_api/scriptures/v1/search?searchText={}+{}&lang=en&version={}'

embedding = OpenAIEmbeddings()


def cache(book, version):
    Path(DATA_PATH).mkdir(parents=True, exist_ok=True)

    # Check to see if the file already exist, if so don't do anything.
    file_path = os.path.join(DATA_PATH, "{}_{}.json".format(book, version))
    if os.path.isfile(file_path):
        print('File {} exists, exiting'.format(file_path))
        return

    # Get the info about the book to obtain the number of chapters.
    book_info_url = BOOK_INFO_URL.format(book)
    print('Sending request: ', book_info_url)
    with request.urlopen(book_info_url) as url:
        response = json.load(url)
        print('Response: ', response)
        chapters = response['chapterCount']

    # Open the file for write.
    print('Writing file: ', file_path)
    with open(file_path, "w", encoding="utf-8") as f:
        # For each chapter make a request to get all the verses.
        for chapter in range(1, chapters + 1):
            search_url = SEARCH_URL.format(book, chapter, version)
            print('Sending request: ', search_url)
            with request.urlopen(search_url) as url:
                response = json.load(url)
                print('Response: ', response)
                scriptures = response['items']
                # Dump each scripture verse into the file.
                for scripture in scriptures:
                    json.dump(scripture, f)
                    f.write("\n")


def metadata_func(record: dict, metadata: dict) -> dict:
    metadata["version"] = record.get("version")
    metadata["book"] = record.get("book")
    metadata["chapter"] = record.get("chapter")
    metadata["verse"] = record.get("verse")
    metadata["id"] = record.get("id")
    metadata["language"] = record.get("language")
    metadata["reference"] = record.get("reference")

    return metadata


def create_loader(book, version):
    cache(book, version)
    file_path = os.path.join(DATA_PATH, "{}_{}.json".format(book, version))
    print("Reading file: ", file_path)
    # pprint(Path(file_path).read_text())
    return JSONLoader(
        file_path=file_path,
        jq_schema='.',
        content_key="text",
        json_lines=True,
        metadata_func=metadata_func)


def vectorize(books, version):
    loaders = map(lambda book: create_loader(book, version), books)

    docs = []
    for loader in loaders:
        docs.extend(loader.load())
    pprint(docs)

    # rm -rf CHROMA_PATH

    vectordb = Chroma.from_documents(
        documents=docs,
        embedding=embedding,
        persist_directory=CHROMA_PATH
    )

    print(vectordb._collection.count())
    return vectordb


if __name__ == '__main__':
    # vectorize(["Gen", "Exo"], "RSKJ")
    database = Chroma(persist_directory=CHROMA_PATH, embedding_function=embedding)
    results = database.similarity_search("How long was the sojourning to be?", k=7)
    pprint(results)
