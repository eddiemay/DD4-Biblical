# Playing with words, learning how to work with them
import pandas
import pathlib
import tensorflow as tf
import tensorflow_text as tf_text

df = pandas.read_csv("../../../data/heb_vocab.csv", dtype=str, comment='*', index_col=0)
#print(df.head(21))

wordDict = df.to_dict('index')
#print(wordDict)

allTokens = ' '.join(df[df['tokens'].notnull()].pop('tokens').array)
#print(allTokens)

splitter = tf_text.RegexSplitter("\s|־")
path = pathlib.Path('/tmp/heb_tok_vocab.txt').write_text(
  allTokens.replace(' ', '\n') + '\n')
hebTokenizer = tf_text.BertTokenizer(
  '/tmp/heb_tok_vocab.txt', token_out_type=tf.string, unknown_token=None)

class Interlinear:
  def __init__(self, word, subTokens):
    self.word = word
    self.subTokens = subTokens
    translation = []
    for subToken in subTokens:
      translation.append(subToken.get("translation"))
    self.translation = ''.join(translation)

  def getSubTokenStr(self) -> str:
    subTokenWords = []
    for subToken in self.subTokens:
      subTokenWords.append(subToken.get("subWord"))
    return '[' + ''.join(subTokenWords) + ']'

  def __str__(self) -> str:
    return 'word: ' + self.word +\
      ' subTokens: ' + self.getSubTokenStr()+\
      ' translation: ' + self.translation

def getSubTranslation(token: str) -> str:
  translation = []
  if not '#' in token:
    translation.append(' ')

  word = wordDict.get(token)
  if not word and '#' in token:
    token = token[2:]
    word = wordDict.get(token)

  if word:
    trans = word.get('translation')
    translation.append(trans if trans == trans else '')
  else:
    translation.append(token)

  return ''.join(translation)

def createSubTokens(word: str):
  subTokens = []
  for subWord in hebTokenizer.tokenize(word).flat_values:
    subWord = subWord.numpy().decode("utf-8")
    subTokens.append(
      dict(subWord = subWord, translation = getSubTranslation(subWord)))
  return subTokens

def toHebrew(mt: str):
  if mt == 'אלהים': # The DSS and Strong's H433 shows a waw was removed.
    return 'אלוהים'
  if mt == 'התנינם':
    return 'התנינים'
  if mt == 'רומש': # I believe this is "and move" but was misspelled.
    return 'ורמש'
  return mt

def createInterlinears(text: str):
  interlinears = []
  for word in splitter.split(text).flat_values:
    word = toHebrew(word.numpy().decode('utf-8'))
    interlinears.append(Interlinear(word, createSubTokens(word)))
  return interlinears

def translateAndPrint(reference: str, text: str):
  if len(text) == 0:
    return
  interlinears = createInterlinears(text)
  hebrew = []
  subTokens = []
  english = []
  for interlinear in interlinears:
    hebrew.append(interlinear.word)
    subTokens.append(interlinear.getSubTokenStr())
    english.append(interlinear.translation)
  print('\n' + reference)
  print(' '.join(hebrew))
  print(' '.join(subTokens))
  print(''.join(english))

Gen_1_1 = "בראשית ברא אלוהים את השמים ואת הארץ"
Gen_1_2 = "והארץ היתה תהו ובהו וחשך על פני תהום ורוח אלוהים מרחפת על פני המים"
Gen_1_3 = "ויאמר אלוהים יהי אור ויהי אור"
Gen_1_4 = "וירא אלוהים את האור כי טוב ויבדל אלוהים בין האור ובין החשך"
Gen_1_5 = "ויקרא אלוהים לאור יום ולחשך קרא לילה ויהי ערב ויהי בקר יום אחד"

# Night one
translateAndPrint("Gen 1:1", Gen_1_1)
translateAndPrint("Gen 1:2", Gen_1_2)
# Day one
translateAndPrint("Gen 1:3", Gen_1_3)
translateAndPrint("Gen 1:4", Gen_1_4)
translateAndPrint("Gen 1:5", Gen_1_5)
# Day two
translateAndPrint("Gen 1:6", "ויאמר אלוהים יהי רקיע בתוך המים ויהי מבדיל בין מים למים")
translateAndPrint("Gen 1:7", "ויעש אלוהים את־הרקיע ויבדל בין המים אשר מתחת לרקיע ובין המים אשר מעל לרקיע ויהי־כן")
translateAndPrint("Gen 1:8", "ויקרא אלוהים לרקיע שמים ויהי־ערב ויהי־בקר יום שני")
# Day three
translateAndPrint("Gen 1:9", "ויאמר אלוהים יקוו המים מתחת השמים אל־מקום אחד ותראה היבשה ויהי־כן")
translateAndPrint("Gen 1:10", "ויקרא אלוהים ׀ ליבשה ארץ ולמקוה המים קרא ימים וירא אלוהים כי־טוב")
translateAndPrint("Gen 1:11", "ויאמר אלוהים תדשא הארץ דשא עשב מזריע זרע עץ פרי עשה פרי למינו אשר זרעו־בו על־הארץ ויהי־כן")
translateAndPrint("Gen 1:12", "ותוצא הארץ דשא עשב מזריע זרע למינהו ועץ עשה־פרי אשר זרעו־בו למינהו וירא אלוהים כי־טוב")
translateAndPrint("Gen 1:13", "ויהי־ערב ויהי־בקר יום שלישי")
# Day four
translateAndPrint("Gen 1:14", "ויאמר אלהים יהי מארת ברקיע השמים להבדיל בין היום ובין הלילה והיו לאתת ולמועדים וליומים ושנים")
translateAndPrint("Gen 1:15", "והיו למאורת ברקיע השמים להאיר על־הארץ ויהי־כן")
translateAndPrint("Gen 1:16", "ויעש אלהים את־שני המארת הגדלים את־המאור הגדל לממשלת היום ואת־המאור הקטן לממשלת הלילה ואת הכוכבים")
translateAndPrint("Gen 1:17", "ויתן אתם אלהים ברקיע השמים להאיר על־הארץ")
translateAndPrint("Gen 1:18", "ולמשל ביום ובלילה ולהבדיל בין האור ובין החשך וירא אלהים כי־טוב")
translateAndPrint("Gen 1:19", "ויהי־ערב ויהי־בקר יום רביעי")
# Day five
translateAndPrint("Gen 1:20", "ויאמר אלהים ישרצו המים שרץ נפש חיה ועוף יעופף על־הארץ על־פני רקיע השמים")
translateAndPrint("Gen 1:21", "ויברא אלהים את־התנינם הגדלים ואת כל־נפש החיה ׀ הרמשת אשר שרצו המים למינהם ואת כל־עוף כנף למינהו וירא אלהים כי־טוב")
translateAndPrint("Gen 1:22", "ויברך אתם אלהים לאמר פרו ורבו ומלאו את־המים בימים והעוף ירב בארץ")
translateAndPrint("Gen 1:23", "ויהי־ערב ויהי־בקר יום חמישי")
# Day six
translateAndPrint("Gen 1:24", "ויאמר אלהים תוצא הארץ נפש חיה למינה בהמה ורמש וחיתו־ארץ למינה ויהי־כן")
translateAndPrint("Gen 1:25", "ויעש אלהים את־חית הארץ למינה ואת־הבהמה למינה ואת כל־רמש האדמה למינהו וירא אלהים כי־טוב")
translateAndPrint("Gen 1:26", "ויאמר אלהים נעשה אדם בצלמנו כדמותנו וירדו בדגת הים ובעוף השמים ובבהמה ובכל־הארץ ובכל־הרמש הרמש על־הארץ")
translateAndPrint("Gen 1:27", "ויברא אלהים ׀ את־האדם בצלמו בצלם אלהים ברא אתו זכר ונקבה ברא אתם")
translateAndPrint("Gen 1:28", "ויברך אתם אלהים ויאמר להם אלהים פרו ורבו ומלאו את־הארץ וכבשה ורדו בדגת הים ובעוף השמים ובכל־חיה הרמשת על־הארץ")
translateAndPrint("Gen 1:29", "ויאמר אלהים הנה נתתי לכם את־כל־עשב ׀ זרע זרע אשר על־פני כל־הארץ ואת־כל־העץ אשר־בו פרי־עץ זרע זרע לכם יהיה לאכלה")
translateAndPrint("Gen 1:30", "ולכל־חית הארץ ולכל־עוף השמים ולכל ׀ רומש על־הארץ אשר־בו נפש חיה את־כל־ירק עשב לאכלה ויהי־כן")
translateAndPrint("Gen 1:31", "וירא אלהים את־כל־אשר עשה והנה־טוב מאד ויהי־ערב ויהי־בקר יום הששי")
# Day Shabbat
translateAndPrint("Gen 2:1", "ויכלו השמים והארץ וכל־צבאם")
translateAndPrint("Gen 2:2", "ויכל אלהים ביום השביעי מלאכתו אשר עשה וישבת ביום השביעי מכל־מלאכתו אשר עשה")
translateAndPrint("Gen 2:3", "ויברך אלהים את־יום השביעי ויקדש אתו כי בו שבת מכל־מלאכתו אשר־ברא אלהים לעשות")
# Isa 9:6
translateAndPrint("Isa 9:6", "כי־ילד ילד־לנו בן נתן־לנו ותהי המשרה על־שכמו ויקרא שמו פלא יועץ אל גבור אביעד שר־שלום")
translateAndPrint("Isa 9:6 (DSS)", "כי ילד יולד לנו בן נתן לנו ותהי המשורה על שכמו וקרא שמו פלא יועץ אל גבור אבי עד שר השלום")