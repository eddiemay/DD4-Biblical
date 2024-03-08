import re
import string
import tensorflow as tf

from tensorflow import keras, RaggedTensor


@keras.utils.register_keras_serializable()
def custom_standardization(input_data):
    lowercase = tf.strings.lower(input_data)
    stripped_html = tf.strings.regex_replace(lowercase, '<br />', ' ')
    # Remove all punctuation marks.
    return tf.strings.regex_replace(stripped_html, '[%s]' % re.escape(string.punctuation), '')


class Interlinear:
    decoded = ""

    def __init__(self, line):
        self.id, self.mt, self.dss, self.constantsOnly, ld, self.diff = line.split(",")
        self.ld = int(ld)

    def input(self):
        return self.mt

    def target(self):
        return self.dss

    def is_candidate(self):
        return self.ld == 0 or self.ld == 1 and self.diff == "+ו"

    def to_csv(self):
        return "{0},{1},{2},{3}\n".format(self.id, self.mt, self.dss, self.decoded)
