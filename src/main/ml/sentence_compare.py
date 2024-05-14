from transformers.utils import logging
logging.set_verbosity_error()

from sentence_transformers import SentenceTransformer
from sentence_transformers import util

model = SentenceTransformer("all-MiniLM-L6-v2")

sentences1 = ['Exodus 12:1 And יהוה spake unto Moses and Aaron in the land of Egypt, saying,',
              'Exodus 12:2 This month shall be unto you the beginning of months: it shall be the first month of the year to you.',
              'Exodus 12:40 Now the sojourning of the children of Israel, who dwelt in Egypt, was four hundred and thirty years.']
embeddings1 = model.encode(sentences1, convert_to_tensor=True)
print(embeddings1)

sentences2 = ['Exodus 12:1 And the Lord spoke to Moses and Aaron in the land of Egypt, saying,',
              'Exodus 12:2 This month shall be to you the beginning of months: it is the first to you among the months of the year.',
              'Exodus 12:40 And the sojourning of the children of Israel, while they sojourned in the land of Egypt and the land of Chanaan, was four hundred and thirty years.']
embeddings2 = model.encode(sentences2, convert_to_tensor=True)
print(embeddings2)

cosine_scores = util.cos_sim(embeddings1, embeddings2)
print(cosine_scores)

for i in range(len(sentences1)):
    print("Score: {:.4f} \t\t {} \t\t {}".format(cosine_scores[i][i], sentences1[i], sentences2[i]))