import re
from collections import defaultdict

def get_stats(vocab):
  """
  Given a vocabulary (dictionary mapping words to frequency counts), returns a
  dictionary of tuples representing the frequency count of pairs of characters
  in the vocabulary.
  """
  pairs = defaultdict(int)
  for word, freq in vocab.items():
    symbols = word.split()
    for i in range(len(symbols)-1):
      pairs[symbols[i],symbols[i+1]] += freq
  return pairs

def merge_vocab(pair, v_in):
  """
  Given a pair of characters and a vocabulary, returns a new vocabulary with the
  pair of characters merged together wherever they appear.
  """
  v_out = {}
  bigram = re.escape(' '.join(pair))
  p = re.compile(r'(?<!\S)' + bigram + r'(?!\S)')
  for word in v_in:
    w_out = p.sub(''.join(pair), word)
    v_out[w_out] = v_in[word]
  return v_out

def get_vocab(data):
  """
  Given a list of strings, returns a dictionary of words mapping to their frequency
  count in the data.
  """
  vocab = defaultdict(int)
  for line in data:
    for word in line.split():
      vocab[' '.join(list(word)) + ' </w>'] += 1
  return vocab

def byte_pair_encoding(data, n):
  """
  Given a list of strings and an integer n, returns a list of n merged pairs
  of characters found in the vocabulary of the input data.
  """
  vocab = get_vocab(data)
  for i in range(n):
    pairs = get_stats(vocab)
    best = max(pairs, key=pairs.get)
    vocab = merge_vocab(best, vocab)
  return vocab

# Example usage:
corpus = '''Tokenization is the process of breaking down 
a sequence of text into smaller units called tokens, 
which can be words, phrases, or even individual characters. 
Tokenization is often the first step in natural languages processing tasks 
such as text classification, named entity recognition, and sentiment analysis. 
The resulting tokens are typically used as input to further processing steps, 
such as vectorization, where the tokens are converted 
into numerical representations for machine learning models to use.'''

n = 230
bpe_pairs = byte_pair_encoding(corpus.split('.'), n)
print(bpe_pairs)

first_week = '''(WLCO) Genesis 1:1 בראשית ברא אלהים את השמים ואת הארץ׃
2 והארץ היתה תהו ובהו וחשך על־פני תהום ורוח אלהים מרחפת על־פני המים׃
3 ויאמר אלהים יהי אור ויהי־אור׃
4 וירא אלהים את־האור כי־טוב ויבדל אלהים בין האור ובין החשך׃
5 ויקרא אלהים ׀ לאור יום ולחשך קרא לילה ויהי־ערב ויהי־בקר יום אחד׃ פ
6 ויאמר אלהים יהי רקיע בתוך המים ויהי מבדיל בין מים למים׃
7 ויעש אלהים את־הרקיע ויבדל בין המים אשר מתחת לרקיע ובין המים אשר מעל לרקיע ויהי־כן׃
8 ויקרא אלהים לרקיע שמים ויהי־ערב ויהי־בקר יום שני׃ פ
9 ויאמר אלהים יקוו המים מתחת השמים אל־מקום אחד ותראה היבשה ויהי־כן׃
10 ויקרא אלהים ׀ ליבשה ארץ ולמקוה המים קרא ימים וירא אלהים כי־טוב׃
11 ויאמר אלהים תדשא הארץ דשא עשב מזריע זרע עץ פרי עשה פרי למינו אשר זרעו־בו על־הארץ ויהי־כן׃
12 ותוצא הארץ דשא עשב מזריע זרע למינהו ועץ עשה־פרי אשר זרעו־בו למינהו וירא אלהים כי־טוב׃
13 ויהי־ערב ויהי־בקר יום שלישי׃ פ
14 ויאמר אלהים יהי מארת ברקיע השמים להבדיל בין היום ובין הלילה והיו לאתת ולמועדים ולימים ושנים׃
15 והיו למאורת ברקיע השמים להאיר על־הארץ ויהי־כן׃
16 ויעש אלהים את־שני המארת הגדלים את־המאור הגדל לממשלת היום ואת־המאור הקטן לממשלת הלילה ואת הכוכבים׃
17 ויתן אתם אלהים ברקיע השמים להאיר על־הארץ׃
18 ולמשל ביום ובלילה ולהבדיל בין האור ובין החשך וירא אלהים כי־טוב׃
19 ויהי־ערב ויהי־בקר יום רביעי׃ פ
20 ויאמר אלהים ישרצו המים שרץ נפש חיה ועוף יעופף על־הארץ על־פני רקיע השמים׃
21 ויברא אלהים את־התנינם הגדלים ואת כל־נפש החיה ׀ הרמשת אשר שרצו המים למינהם ואת כל־עוף כנף למינהו וירא אלהים כי־טוב׃
22 ויברך אתם אלהים לאמר פרו ורבו ומלאו את־המים בימים והעוף ירב בארץ׃
23 ויהי־ערב ויהי־בקר יום חמישי׃ פ
24 ויאמר אלהים תוצא הארץ נפש חיה למינה בהמה ורמש וחיתו־ארץ למינה ויהי־כן׃
25 ויעש אלהים את־חית הארץ למינה ואת־הבהמה למינה ואת כל־רמש האדמה למינהו וירא אלהים כי־טוב׃
26 ויאמר אלהים נעשה אדם בצלמנו כדמותנו וירדו בדגת הים ובעוף השמים ובבהמה ובכל־הארץ ובכל־הרמש הרמש על־הארץ׃
27 ויברא אלהים ׀ את־האדם בצלמו בצלם אלהים ברא אתו זכר ונקבה ברא אתם׃
28 ויברך אתם אלהים ויאמר להם אלהים פרו ורבו ומלאו את־הארץ וכבשה ורדו בדגת הים ובעוף השמים ובכל־חיה הרמשת על־הארץ׃
29 ויאמר אלהים הנה נתתי לכם את־כל־עשב ׀ זרע זרע אשר על־פני כל־הארץ ואת־כל־העץ אשר־בו פרי־עץ זרע זרע לכם יהיה לאכלה׃
30 ולכל־חית הארץ ולכל־עוף השמים ולכל ׀ רומש על־הארץ אשר־בו נפש חיה את־כל־ירק עשב לאכלה ויהי־כן׃
31 וירא אלהים את־כל־אשר עשה והנה־טוב מאד ויהי־ערב ויהי־בקר יום הששי׃ פ
(WLCO) Genesis 2:1 ויכלו השמים והארץ וכל־צבאם׃
2 ויכל אלהים ביום השביעי מלאכתו אשר עשה וישבת ביום השביעי מכל־מלאכתו אשר עשה׃
3 ויברך אלהים את־יום השביעי ויקדש אתו כי בו שבת מכל־מלאכתו אשר־ברא אלהים לעשות׃ פ
(WLCO) Isaiah 9:6 כי־ילד ילד־לנו בן נתן־לנו ותהי המשרה על־שכמו ויקרא שמו פלא יועץ אל גבור אביעד שר־שלום׃
'''

n = 230
bpe_pairs = sorted(byte_pair_encoding(first_week.split('.'), n).items(), key=lambda x:x[1], reverse=True)
print(bpe_pairs)
for key in bpe_pairs[:14]:
  print(key)

f = open('../../../data/isa-dss.txt')
dss_isa = f.read()

n = 230
bpe_pairs = sorted(byte_pair_encoding(dss_isa.split('\n'), n).items(), key=lambda x:x[1], reverse=True)
print("\n Isaiah:")
# print(bpe_pairs)
for key in bpe_pairs[:230]:
  print(key)