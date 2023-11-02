import re
import string
import tensorflow as tf

from tensorflow import keras

@keras.utils.register_keras_serializable()
def custom_standardization(input_data):
  lowercase = tf.strings.lower(input_data)
  stripped_html = tf.strings.regex_replace(lowercase, '<br />', ' ')
  return tf.strings.regex_replace(
    stripped_html, '[%s]' % re.escape(string.punctuation), '')