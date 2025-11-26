import json


def reorganize(file_path: str):
  with open(file_path, "r", encoding="utf-8") as f:
    lines = []
    for line in f:
      # print(line)
      if line.startswith('**'):
        lines.append(line.strip())
        continue
      entry = json.loads(line)
      reentry = {'strongsId': entry.get('strongsId'), 'root': entry.get('root'),
                 'translation': entry.get('translation'),
                 'tokenType': entry.get('tokenType'), 'asSuffix': entry.get('asSuffix')}
      reentry = {k: v for k, v in reentry.items() if v is not None}
      l = json.dumps(reentry, ensure_ascii=False, separators=(',', ':'))
      lines.append(l)
    # sorted_data = sorted(reentrys, key=lambda e: (e['strongsId'], e['root']))
    for l in lines:
      print(l)
  with open(file_path, "w", encoding="utf-8") as f:
    for l in lines:
      f.write(l)
      f.write('\n')


if __name__ == '__main__':
  reorganize('files/gez_prefixes.jsonl')
  reorganize('files/gez_vocab.jsonl')