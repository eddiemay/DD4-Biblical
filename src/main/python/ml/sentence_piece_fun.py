import numpy
import json
import sentencepiece as spm

# file = '../services/dss_images/books/1Q_Isaiah_a.txt'
file = '../services/dss_images/training/bible_heb.txt'

# spm.SentencePieceTrainer.train(input=file, model_type='bpe', model_prefix='sentence_piece', vocab_size=4096)


def read_dictionary(dictionary, file):
    with open(file, 'r') as f:
        for line in f:
            if line.startswith('*'):
                continue
            token = json.loads(line)
            id = f'{token.get("strongsId")}-{token["root"]}'
            if dictionary.get(id) is None:
                dictionary[id] = token


s = spm.SentencePieceProcessor(model_file='sentence_piece.model')

with open(file, 'r') as file:
    content = file.read()

encoded = ''
for n in range(3):
    encoded = s.encode(content, out_type=str, enable_sampling=True, alpha=0.1, nbest_size=3)

# print(encoded)

unique, counts = numpy.unique(numpy.array(encoded), return_counts=True)
dict = dict(zip(unique, counts))
dict = sorted(dict.items(), key=lambda item: item[1])

dictionary = {}
read_dictionary(dictionary, '../services/translation/files/heb_vocab_prefixes.json')
read_dictionary(dictionary, '../services/translation/files/heb_vocab_overrides.json')
read_dictionary(dictionary, '../services/translation/files/heb_vocab_lexicon_ancient.json')
read_dictionary(dictionary, '../services/translation/files/heb_vocab_lexicon_ancient.json')
read_dictionary(dictionary, '../services/translation/files/heb_vocab_lexicon_strongs.json')

grouped_by_root = {}
for token in dictionary.values():
    if grouped_by_root.get(token["root"]) is None:
        grouped_by_root[token["root"]] = []
    grouped_by_root[token["root"]].append(token)

results = []
for d in dict:
    result = {"piece": d[0], "count": d[1]}
    words = grouped_by_root.get(d[0].replace('▁', ''))
    if words is not None:
        result["translations"] = '|'.join(list(map(lambda t: t["translation"], words)))
        for word in words:
            if word.get('asSuffix') is not None:
                result["translations"] = result["translations"] + '|' + word.get('asSuffix')
        result["strongIds"] = ' '.join(list(map(lambda t: t.get('strongsId', ''), words)))
    else:
        result["translations"] = ''
        result["strongIds"] = ''
    print(f'{result["piece"]},{result["count"]},{result["translations"]},{result["strongIds"]}')
    results.append(result)

with open('sentence_piece_heb.csv', 'w') as csv:
    csv.write('Piece,Count,Strongs Ids,Translations\n')
    for result in reversed(results):
        csv.write(f'{result["piece"]},{result["count"]},{result["translations"]},{result["strongIds"]}\n')

# print(dictionary)
# print(grouped_by_root)


