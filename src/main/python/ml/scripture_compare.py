import time
import urllib.request, json
from sentence_transformers import SentenceTransformer
from sentence_transformers import util

# Search URL for Mackabee Ministries scriptures. Will fetch scriptures as a json object.
SEARCH_URL = 'https://dd4-biblical.appspot.com/_api/scriptures/v1/search?searchText={}&lang=en&version={}'
STANDARD_VERSION = 'RSKJ' # Normally RSKJ (Restored King James Version) as our standard text, may also use NRSV.
COMPARISON_VERSION = 'SEP' # Will use the Septuagint "SEP" version for most comparisons, also have DSS available for Isa


# The list of reference scriptures to compare, this will be used in a search that can parse a lot of different reference
# types
REFERENCES = 'Exo 12-14'


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


if __name__ == '__main__':
    start_time = time.perf_counter()
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

    api_call_2_start = time.perf_counter()
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

    flatten_start = time.perf_counter()
    standardTexts = []
    compareTexts = []
    for candidate in candidates:
        standardTexts.append(candidate.standardText)
        compareTexts.append(candidate.compareText)

    model_load_start = time.perf_counter()
    model = SentenceTransformer("all-MiniLM-L6-v2")

    process_start = time.perf_counter()
    embeddings1 = model.encode(standardTexts, convert_to_tensor=True)
    embeddings2 = model.encode(compareTexts, convert_to_tensor=True)
    cosine_scores = util.cos_sim(embeddings1, embeddings2)

    for i in range(len(candidates)):
        candidates[i].score = cosine_scores[i][i]
        print(candidates[i])

    end_time = time.perf_counter()
    print("\n Processed: {} verses\n".format(len(candidates)))
    print("Total time: {} seconds\n\t1st Api Call: {} seconds\n\t2nd Api Call: {} seconds\n\tModel Load Time: {} seconds\n\tProcessing Time: {} seconds\n"
          .format(end_time - start_time, api_call_2_start - start_time, model_load_start - api_call_2_start, process_start - model_load_start, end_time - process_start))
