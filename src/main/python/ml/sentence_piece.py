import json
import numpy
import os
import sentencepiece as spm
import time
from urllib import request

lang = 'gez'
API_BASE = 'https://dd4-biblical.appspot.com/_api/'
BOOK_INFO_URL = API_BASE + 'books/v1/get?id={}'
SEARCH_URL = API_BASE + 'scriptures/v1/fetch?searchText={}+{}&lang={}&version=ISR'
books_ot = ['Gen', 'Exo', 'Lev', 'Num', 'Deut', 'Josh', 'Jdg', 'Ruth', '1Sam', '2Sam',
         '1Ki', '2Ki', '1Chr', '2Chr', 'Ezra', 'Neh', 'Est', 'Job', 'Psa', 'Prv',
         'Ecc', 'SoS', 'Isa', 'Jer', 'Lam', 'Eze', 'Dan', 'Hos', 'Joel', 'Amos',
         'Ob', 'Jonah', 'Micah', 'Nah', 'Hab', 'Zep', 'Hag', 'Zec', 'Mal']
books_nt = ['Matt', 'Mark', 'Luke', 'John', 'Act', 'Rom', '1Cor', '2Cor', 'Gal',
            'Eph', 'Phil', 'Col', '1Th', '2Th', '1Tim', '2Tim', 'Titus', 'Phm',
            'Heb', 'Jas', '1Pet', '2Pet', '1Jo', '2Jo', '3Jo', 'Jude', 'Rev']
books_all = books_ot + books_nt


def create_file(file):
    print('Writing file: ', file)
    with open(file, "w", encoding="utf-8") as f:
        for book in books_nt if lang == 'greek' else books_all if lang == 'gez' else books_ot:
            time.sleep(1)
            # Get the info about the book to obtain the number of chapters.
            if book == 'Psa':
                chapters = 150
            elif book == 'Est':
                chapters = 10
            else:
                book_info_url = BOOK_INFO_URL.format(book)
                print('Sending request: ', book_info_url)
                with request.urlopen(book_info_url) as url:
                    response = json.load(url)
                    print('Response: ', response)
                    chapters = response['chapterCount']

            # For each chapter make a request to get all the verses.
            api_lang = f'{lang}-re' if lang == 'he' or lang == 'gez' else lang
            for chapter in range(1, chapters + 1):
                search_url = SEARCH_URL.format(book, chapter, api_lang)
                print('Sending request: ', search_url)
                with request.urlopen(search_url) as url:
                    response = json.load(url)
                    print('Response: ', response)
                    scriptures = response['items']
                    # Dump each scripture verse into the file.
                    for scripture in scriptures:
                        f.write(scripture['text'].replace('׃', '')
                                .replace('־', ' ').replace('׀', '') + '\n')


def train(file):
    if not os.path.exists(file):
        create_file(file)
    spm.SentencePieceTrainer.train(
        input=file, model_type='bpe',
        model_prefix=f'sentence_piece_{lang}', vocab_size=11000)


def read_dictionary(dictionary, file):
    with open(file, 'r') as f:
        for line in f:
            if line.startswith('*'):
                continue
            token = json.loads(line)
            id = f'{token.get("strongsId")}-{token["root"]}'
            if dictionary.get(id) is None:
                dictionary[id] = token


def translate(dict):
    results = []
    for d in dict:
        if d[0] == '▁':
            continue
        result = {"piece": d[0], "count": d[1]}
        words = grouped_by_root.get(d[0].replace('▁', ''))
        if words is not None:
            result["translations"] =(
                '|'.join(list(map(lambda t: t["translation"], words))))
            for word in words:
                if word.get('asSuffix') is not None:
                    result["translations"] =(
                        result["translations"] + '|' + word.get('asSuffix'))
            result["strongIds"] =(
                ' '.join(list(map(lambda t: t.get('strongsId', ''), words))))
        else:
            result["translations"] = ''
            result["strongIds"] = ''
        print(f'{result["piece"]},{result["count"]},{result["translations"]},{result["strongIds"]}')
        results.append(result)

    return results


if __name__ == '__main__':
    file = f'bible_{lang}.txt'
    train(file)
    s = spm.SentencePieceProcessor(model_file=f'sentence_piece_{lang}.model')

    with open(file, 'r') as file:
        content = file.read()

    encoded = ''
    for n in range(3):
        encoded = s.encode(
            content, out_type=str, enable_sampling=True, alpha=0.1, nbest_size=3)

    # print(encoded)

    unique, counts = numpy.unique(numpy.array(encoded), return_counts=True)
    dict = dict(zip(unique, counts))
    dict = sorted(dict.items(), key=lambda item: item[1])

    dictionary = {}
    read_dictionary(dictionary,
                    '../services/translation/files/heb_prefixes.jsonl')
    read_dictionary(dictionary,
                    '../services/translation/files/heb_vocab_overrides.jsonl')
    read_dictionary(dictionary,
                    '../services/translation/files/heb_vocab_lexicon_ancient.jsonl')
    read_dictionary(dictionary,
                    '../services/translation/files/heb_vocab_lexicon_ancient.jsonl')
    read_dictionary(dictionary,
                    '../services/translation/files/heb_vocab_lexicon_strongs.jsonl')
    read_dictionary(dictionary,
                    '../services/translation/files/gk_vocab_overrides.jsonl')
    read_dictionary(dictionary,
                    '../services/translation/files/gk_vocab_lexicon_strongs.jsonl')
    read_dictionary(dictionary,
                    '../services/translation/files/gez_prefixes.jsonl')
    read_dictionary(dictionary, '../services/translation/files/gez_vocab.jsonl')

    grouped_by_root = {}
    for token in dictionary.values():
        if grouped_by_root.get(token["root"]) is None:
            grouped_by_root[token["root"]] = []
        grouped_by_root[token["root"]].append(token)

    results = translate(dict)

    with open(f'sentence_piece_{lang}.csv', 'w') as csv:
        csv.write('Piece,Count,Translations,Strong Ids\n')
        for result in reversed(results):
            csv.write(f'{result["piece"]},{result["count"]},{result["translations"]},{result["strongIds"]}\n')

    # print(dictionary)
    # print(grouped_by_root)

    all_words = content.split()
    unique_words, word_counts = numpy.unique(numpy.array(all_words), return_counts=True)
    word_dict = []
    for i in range(0, len(unique_words)):
        word_dict.append({0: unique_words[i], 1: word_counts[i]})
    # word_dict = dict(zip(unique_words, word_counts))
    word_dict = sorted(word_dict, key=lambda item: item[1])
    results = translate(word_dict)

    with open(f'sentence_word_piece_{lang}.csv', 'w') as csv:
        csv.write('Piece,Count,Translations,Strong Ids\n')
        for result in reversed(results):
            csv.write(f'{result["piece"]},{result["count"]},{result["translations"]},{result["strongIds"]}\n')
    # for word in results:
        # print(word)




