import urllib.request, json
from transformers.utils import logging
logging.set_verbosity_error()

from sentence_transformers import SentenceTransformer
from sentence_transformers import util

# Search URL for Mackabee Ministries scriptures. Will fetch scriptures as a json object.
SEARCH_URL = 'https://dd4-biblical.appspot.com/_api/scriptures/v1/search?searchText={}&lang=en&version={}'
STANDARD_VERSION = 'RSKJ' # Normally RSKJ (Restored King James Version) as our standard text, may also use NRSV.
COMPARISON_VERSION = 'SEP' # Will use the Septuagint "SEP" version for most comparisons, also have DSS available for Isa


# The list of reference scriptures to compare, this will be used in a search that can parse a lot of different reference
# types
REFERENCES = 'Gen 1'


# Candidate helper class that holds a scripture, the comparison text and the resulting match score.
class Candidate:
    compareText = ''
    score = 0

    def __init__(self, book, chapter, verse, standardText):
        self.book = book
        self.chapter = chapter
        self.verse = verse
        self.standardText = standardText

    def reference(self):
        return '{} {}:{}'.format(self.book, self.chapter, self.verse)

    def __str__(self):
        return '{},{:.4f},"{}","{}"'.format(self.reference(), self.score, self.standardText, self.compareText)

    def __repr__(self):
        return str(self)

def compare(model, candidate):
    embeddings1 = model.encode(candidate.standardText, convert_to_tensor=True)
    #print(embeddings1)
    embeddings2 = model.encode(candidate.compareText, convert_to_tensor=True)
    #print(embeddings2)

    cosine_scores = util.cos_sim(embeddings1, embeddings2)
    #print(cosine_scores)

    candidate.score = cosine_scores[0][0]


if __name__ == '__main__':

    candidates = []
    candidateMap = {}
    # first connect to the api and get the standard translation
    with urllib.request.urlopen(SEARCH_URL.format(urllib.parse.quote(REFERENCES), STANDARD_VERSION)) as url:
        response = json.load(url)
        # print(response)

        for result in response['items']:
            # print(result)
            candidate = Candidate(result['book'], result['chapter'], result['verse'], result['text'])
            candidates.append(candidate)
            candidateMap[candidate.reference()] = candidate

    # Then fetch the comparison text
    with urllib.request.urlopen(SEARCH_URL.format(urllib.parse.quote(REFERENCES), COMPARISON_VERSION)) as url:
        response = json.load(url)
        # print(response)

        for result in response['items']:
            # print(result)
            candidate = candidateMap['{} {}:{}'.format(result['book'], result['chapter'], result['verse'])]
            if candidate is None :
                candidate = Candidate(result['book'], result['chapter'], result['verse'], '')
                candidates.append(candidate)
                candidateMap[candidate.reference()] = candidate

            candidate.compareText = result['text']

    model = SentenceTransformer("all-MiniLM-L6-v2")

    for candidate in candidates:
        compare(model, candidate)
        print(candidate)
