import numpy
import os
from sentence_piece import create_file, read_dictionary

lang = 'gez'


def tokenize(file):
  if not os.path.exists(file):
    create_file(file)



if __name__ == '__main__':
  file = f'bible_{lang}.txt'
  tokenize(file)

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

  with open(f'sentence_piece_{lang}.csv', 'w') as csv:
    csv.write('Piece,Count,Translations,Strong Ids\n')
    for result in reversed(results):
      csv.write(f'{result["piece"]},{result["count"]},{result["translations"]},{result["strongIds"]}\n')