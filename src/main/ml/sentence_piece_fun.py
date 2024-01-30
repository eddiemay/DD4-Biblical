import sentencepiece as spm

spm.SentencePieceTrainer.train(
  input='../webapp/books/isaiah_dss.txt', model_type='bpe', model_prefix='m', vocab_size=1000)

s = spm.SentencePieceProcessor(model_file='m.model')
for n in range(7):
  print(
    s.encode('רחצו והזכו והסירו רוע מעלליכם מנגד עיני חדלו הרע',
             out_type=str, enable_sampling=True, alpha=0.1, nbest_size=-1))