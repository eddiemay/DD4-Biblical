import json
import os

from langchain_community.document_loaders import JSONLoader
from langchain_community.vectorstores import Chroma
from langchain_huggingface import HuggingFaceEmbeddings
from pathlib import Path
from pprint import pprint
from urllib import request

DATA_PATH = "chatbot/bibleCache/"
CHROMA_PATH = "chatbot/chroma/"
BOOK_INFO_URL = 'https://dd4-biblical.appspot.com/_api/books/v1/get?id={}'
FETCH_URL = 'https://dd4-biblical.appspot.com/_api/scriptures/v1/fetch?searchText={}+{}&lang=en&version={}'
model = "deepseek-r1:7b"

embedding = HuggingFaceEmbeddings(model_name="sentence-transformers/all-mpnet-base-v2")


def cache(book, version):
    Path(DATA_PATH).mkdir(parents=True, exist_ok=True)

    # Check to see if the file already exist, if so don't do anything.
    file_path = os.path.join(DATA_PATH, f"{book}_{version}.json")
    chapter_file_path = os.path.join(DATA_PATH, f"{book}_by_ch_{version}.json")
    if os.path.isfile(file_path) and os.path.isfile(chapter_file_path):
        print(f'File {file_path} exists, exiting')
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
        with open(chapter_file_path, "w", encoding="utf-8") as f2:
            # For each chapter make a request to get all the verses.
            for chapter in range(1, chapters + 1):
                fetch_url = FETCH_URL.format(book, chapter, version)
                print('Sending request: ', fetch_url)
                with request.urlopen(fetch_url) as url:
                    response = json.load(url)
                    print('Response: ', response)
                    scriptures = response['items']
                    chapter_text = ''
                    # Dump each scripture verse into the file.
                    for scripture in scriptures:
                        del scripture["id"]
                        chapter_text += f"{scripture['verse']} {scripture['text']} "
                        json.dump(scripture, f)
                        f.write("\n")
                    by_chapter = scriptures[0].copy()
                    by_chapter["text"] = chapter_text
                    by_chapter["reference"] = f'{by_chapter["book"]} {by_chapter["chapter"]}'
                    del by_chapter["verse"]
                    json.dump(by_chapter, f2)
                    f2.write("\n")


def metadata_func(record: dict, metadata: dict) -> dict:
    metadata["version"] = record.get("version")
    metadata["book"] = record.get("book")
    metadata["chapter"] = record.get("chapter")
    metadata["verse"] = record.get("verse")
    metadata["language"] = record.get("language")
    metadata["reference"] = record.get("reference")

    return metadata


def create_loader(book, version):
    cache(book, version)
    file_path = os.path.join(DATA_PATH, f"{book}_by_ch_{version}.json")
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
    vectorize(["Gen", "Exo", "Lev", "Num", "Deut"], "ISR")
    database = Chroma(persist_directory=CHROMA_PATH, embedding_function=embedding)
    query = "How long was the sojourning to be?"
    results = database.similarity_search(query, k=5)
    pprint(query)
    pprint(results)

    query = "What does Genesis 2:3 say?"
    results = database.similarity_search(query, k=5)
    pprint(query)
    pprint(results)

    query = "How long did Adam live?"
    results = database.similarity_search("How long did Adam live?", k=5)
    pprint(query)
    pprint(results)
