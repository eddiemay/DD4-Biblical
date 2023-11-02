import tensorflow as tf

from src.main.ml.global_functions import custom_standardization

print(tf.__version__)

reloaded = tf.keras.models.load_model(
  'text_classification',
  custom_objects={"custom_standardization": custom_standardization})

examples = [
  "The movie was great!",
  "The movie was okay.",
  "The movie was terrible...",
  "This movie was good, so good. It is the best, just the absolute best. There has ever been a better movie in the history of the world.",
  "This movie is such trash, just the worse, the worse movie ever made, whoever made it is a stone cold loser."
]

print(reloaded.predict(examples))