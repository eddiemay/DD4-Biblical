import time
import urllib.request, json
from sentence_compare import similarity_compare

# Fetch URL for Mackabee Ministries scriptures. Will fetch scriptures as a json object.
FETCH_URL = 'https://dabar.cloud/_api/scriptures/v1/fetch?searchText={}&lang=en&version={}'
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
    with urllib.request.urlopen(FETCH_URL.format(urllib.parse.quote(REFERENCES), STANDARD_VERSION)) as url:
        response = json.load(url)
        # print(response)

        for result in response['items']:
            # print(result)
            candidate = Candidate(result['book'], result['chapter'], result['verse'], result['text'])
            candidates.append(candidate)
            candidateMap[candidate.reference()] = candidate

    api_call_2_start = time.perf_counter()
    # Then fetch the comparison text
    with urllib.request.urlopen(FETCH_URL.format(urllib.parse.quote(REFERENCES), COMPARISON_VERSION)) as url:
        response = json.load(url)
        # print(response)

        for result in response['items']:
            # print(result)
            candidate = candidateMap.get(
                '{} {}:{}'.format(result['book'], result['chapter'], result['verse']))
            if candidate is None:
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

    process_start = time.perf_counter()
    cosine_scores = similarity_compare(standardTexts, compareTexts)

    for i in range(len(candidates)):
        candidates[i].score = cosine_scores[i][i]
        print(candidates[i])

    end_time = time.perf_counter()
    print("\n Processed: {} verses\n".format(len(candidates)))
    print("Total time: {} seconds\n\t1st Api Call: {} seconds\n\t2nd Api Call: {} seconds\n\tProcessing Time: {} seconds\n"
          .format(end_time - start_time, api_call_2_start - start_time, process_start - api_call_2_start, end_time - process_start))
