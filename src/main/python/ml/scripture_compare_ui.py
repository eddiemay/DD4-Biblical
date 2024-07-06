
import gradio as gr
import urllib.request, json
from sentence_transformers import SentenceTransformer
from sentence_transformers import util
from transformers.utils import logging
logging.set_verbosity_error()

# Search URL for Maccabees Ministries scriptures. Will fetch scriptures as a json object.
SEARCH_URL = 'https://dd4-biblical.appspot.com/_api/scriptures/v1/search?searchText={}&lang=en&version={}'

model = SentenceTransformer("all-MiniLM-L6-v2")


# Candidate helper class that holds a scripture, the comparison text and the resulting match score.
class Candidate:
    compareText = ''
    score = 0

    def __init__(self, book, chapter, verse, standard_text):
        self.book = book
        self.chapter = chapter
        self.verse = verse
        self.standardText = standard_text

    def reference(self):
        return '{} {}:{}'.format(self.book, self.chapter, self.verse)

    def to_tabbed(self):
        return '{}\t\t {:.4f}\t\t {}\t\t {}'.format(self.reference(), self.score, self.standardText, self.compareText)

    def to_csv(self):
        return '{},{:.4f},"{}","{}"'.format(self.reference(), self.score, self.standardText, self.compareText)

    def __str__(self):
        return self.to_csv()

    def __repr__(self):
        return self.to_csv()


def compare(reference, standard_version, compare_version):
    candidates = []
    candidate_map = {}
    # Connect to the api and get the standard translation
    with urllib.request.urlopen(SEARCH_URL.format(urllib.parse.quote(reference), standard_version)) as url:
        response = json.load(url)
        # print(response)

        for result in response['items']:
            # print(result)
            candidate = Candidate(result['book'], result['chapter'], result['verse'], result['text'])
            candidates.append(candidate)
            candidate_map[candidate.reference()] = candidate

    # Then fetch the comparison text
    with urllib.request.urlopen(SEARCH_URL.format(urllib.parse.quote(reference), compare_version)) as url:
        response = json.load(url)
        # print(response)

        for result in response['items']:
            # print(result)
            candidate = candidate_map['{} {}:{}'.format(result['book'], result['chapter'], result['verse'])]
            if candidate is None:
                candidate = Candidate(result['book'], result['chapter'], result['verse'], '')
                candidates.append(candidate)
                candidate_map[candidate.reference()] = candidate

            candidate.compareText = result['text']

        # Isa 1:1 standardText: This is the book of Isaiah compareText: The book that Isaiah wrote.
        # Isa 1:2 standardText: Isaiah was a good man compareText: Isaiah did what was right.

    standard_texts = []  # 2 items This is the book of Isaiah, Isaiah was a good man
    compare_texts = []  # 2 items The book that Isaiah wrote., Isaiah did what was right.
    for candidate in candidates:
        standard_texts.append(candidate.standardText)
        compare_texts.append(candidate.compareText)

    embeddings1 = model.encode(standard_texts, convert_to_tensor=True)
    embeddings2 = model.encode(compare_texts, convert_to_tensor=True)
    cosine_scores = util.cos_sim(embeddings1, embeddings2)

    for i in range(len(candidates)):
        candidates[i].score = cosine_scores[i][i]
        # print(candidates[i])

    return '\n\n'.join(c.to_tabbed() for c in candidates)


ui = gr.Interface(
    compare,
    [
        gr.Textbox(
            label="Scriptures",
            info="""Enter a verse, verse range, chapter(s) or mix
                (e.g. Gen 2:3 or Gen 2:1-3 or Exo 12 or Lev 20-23 or Gen 2:3,Exo 12,Lev 20-23)""",
            lines=1,
        ),
        gr.Radio(["ISR", "RSKJ", "NRSV", "NWT", "SEP", "DSS"], label="Control Version",
                 info="The version of scripture to use as the standard.", value="RSKJ"),
        gr.Radio(["ISR", "RSKJ", "NRSV", "NWT", "SEP", "DSS"], label="Comparison Version",
                 info="The version of scripture to use as the comparison.", value="SEP")
    ],
    gr.Textbox(label="Comparisons", lines=28))
ui.launch()
