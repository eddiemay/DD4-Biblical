import numpy
import sentencepiece as spm

# file = '../services/dss_images/books/1Q_Isaiah_a.txt'
file = '../services/dss_images/training/torah.txt'

spm.SentencePieceTrainer.train(
    input=file, model_type='bpe', model_prefix='sentence_piece', vocab_size=4096)

s = spm.SentencePieceProcessor(model_file='sentence_piece.model')

with open(file, 'r') as file:
    content = file.read()

encoded = ''
for n in range(3):
    encoded = s.encode(
        content, out_type=str, enable_sampling=True, alpha=0.1, nbest_size=3)

# print(encoded)

unique, counts = numpy.unique(numpy.array(encoded), return_counts=True)
dict = dict(zip(unique, counts))
dict = reversed(sorted(dict.items(), key=lambda item: item[1]))

for d in dict:
    print(d)
