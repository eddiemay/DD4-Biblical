# Egnlish to Spanish machine translation tutorial, taken
# from https://keras.io/examples/nlp/neural_machine_translation_with_keras_nlp/
import keras_nlp
import pathlib
import random
import tensorflow as tf

from tensorflow import keras
from tensorflow_text.tools.wordpiece_vocab import (bert_vocab_from_dataset as bert_vocab,)

BATCH_SIZE = 64
EPOCHS = 1  # This should be at least 10 for convergence
MAX_SEQUENCE_LENGTH = 40
ENG_VOCAB_SIZE = 15000
SPA_VOCAB_SIZE = 15000

EMBED_DIM = 256
INTERMEDIATE_DIM = 2048
NUM_HEADS = 8

text_file = keras.utils.get_file(
  fname="spa-eng.zip",
  origin="http://storage.googleapis.com/download.tensorflow.org/data/spa-eng.zip",
  extract=True,
)
text_file = pathlib.Path(text_file).parent / "spa-eng" / "spa.txt"

with open(text_file) as f:
  lines = f.read().split("\n")[:-1]
text_pairs = []
for line in lines:
  eng, spa = line.split("\t")
  eng = eng.lower()
  spa = spa.lower()
  text_pairs.append((eng, spa))

for _ in range(5):
  print(random.choice(text_pairs))

random.shuffle(text_pairs)
num_val_samples = int(0.15 * len(text_pairs))
num_train_samples = len(text_pairs) - 2 * num_val_samples
train_pairs = text_pairs[:num_train_samples]
val_pairs = text_pairs[num_train_samples : num_train_samples + num_val_samples]
test_pairs = text_pairs[num_train_samples + num_val_samples :]

print(f"{len(text_pairs)} total pairs")
print(f"{len(train_pairs)} training pairs")
print(f"{len(val_pairs)} validation pairs")
print(f"{len(test_pairs)} test pairs")

def train_word_piece(text_samples, vocab_size, reserved_tokens):
  word_piece_ds = tf.data.Dataset.from_tensor_slices(text_samples)
  vocab = keras_nlp.tokenizers.compute_word_piece_vocabulary(
    word_piece_ds.batch(1000).prefetch(2),
    vocabulary_size=vocab_size,
    reserved_tokens=reserved_tokens,
  )
  return vocab

reserved_tokens = ["[PAD]", "[UNK]", "[START]", "[END]"]

eng_samples = [text_pair[0] for text_pair in train_pairs]
eng_vocab = train_word_piece(eng_samples, ENG_VOCAB_SIZE, reserved_tokens)

spa_samples = [text_pair[1] for text_pair in train_pairs]
spa_vocab = train_word_piece(spa_samples, SPA_VOCAB_SIZE, reserved_tokens)

print("English Tokens: ", eng_vocab[100:110])
print("Spanish Tokens: ", spa_vocab[100:110])

eng_tokenizer = keras_nlp.tokenizers.WordPieceTokenizer(
  vocabulary=eng_vocab, lowercase=False
)
spa_tokenizer = keras_nlp.tokenizers.WordPieceTokenizer(
  vocabulary=spa_vocab, lowercase=False
)

eng_input_ex = text_pairs[0][0]
eng_tokens_ex = eng_tokenizer.tokenize(eng_input_ex)
print("English sentence: ", eng_input_ex)
print("Tokens: ", eng_tokens_ex)
print("Text after detokenizing: ", eng_tokenizer.detokenize(eng_tokens_ex))

print()

spa_input_ex = text_pairs[0][1]
spa_tokens_ex = spa_tokenizer.tokenize(spa_input_ex)
print("Spanish sentence: ", spa_input_ex)
print("Tokens: ", spa_tokens_ex)
print("Text after detokenizing: ", spa_tokenizer.detokenize(spa_tokens_ex))

def preprocess_batch(eng, spa):
  batch_size = tf.shape(spa)[0]

  eng = eng_tokenizer(eng)
  spa = spa_tokenizer(spa)

  # Pad `eng` to `MAX_SEQUENCE_LENGTH`.
  eng_start_end_packer = keras_nlp.layers.StartEndPacker(
    sequence_length=MAX_SEQUENCE_LENGTH,
    pad_value=eng_tokenizer.token_to_id("[PAD]"),
  )
  eng = eng_start_end_packer(eng)

  # Add special tokens (`"[START]"` and `"[END]"`) to `spa` and pad it as well.
  spa_start_end_packer = keras_nlp.layers.StartEndPacker(
    sequence_length=MAX_SEQUENCE_LENGTH + 1,
    start_value=spa_tokenizer.token_to_id("[START]"),
    end_value=spa_tokenizer.token_to_id("[END]"),
    pad_value=spa_tokenizer.token_to_id("[PAD]"),
  )
  spa = spa_start_end_packer(spa)

  return (
    {
      "encoder_inputs": eng,
      "decoder_inputs": spa[:, :-1],
    },
    spa[:, 1:],
  )

def make_dataset(pairs):
  eng_texts, spa_texts = zip(*pairs)
  eng_texts = list(eng_texts)
  spa_texts = list(spa_texts)
  dataset = tf.data.Dataset.from_tensor_slices((eng_texts, spa_texts))
  dataset = dataset.batch(BATCH_SIZE)
  dataset = dataset.map(preprocess_batch, num_parallel_calls=tf.data.AUTOTUNE)
  return dataset.shuffle(2048).prefetch(16).cache()

train_ds = make_dataset(train_pairs)
val_ds = make_dataset(val_pairs)

for inputs, targets in train_ds.take(1):
  print(f'inputs["encoder_inputs"].shape: {inputs["encoder_inputs"].shape}')
  print(f'inputs["decoder_inputs"].shape: {inputs["decoder_inputs"].shape}')
  print(f"targets.shape: {targets.shape}")

# Encoder
encoder_inputs = keras.Input(shape=(None,), dtype="int64", name="encoder_inputs")

x = keras_nlp.layers.TokenAndPositionEmbedding(
  vocabulary_size=ENG_VOCAB_SIZE,
  sequence_length=MAX_SEQUENCE_LENGTH,
  embedding_dim=EMBED_DIM,
  mask_zero=True,
)(encoder_inputs)

encoder_outputs = keras_nlp.layers.TransformerEncoder(
  intermediate_dim=INTERMEDIATE_DIM, num_heads=NUM_HEADS
)(inputs=x)
encoder = keras.Model(encoder_inputs, encoder_outputs)

# Decoder
decoder_inputs = keras.Input(shape=(None,), dtype="int64", name="decoder_inputs")
encoded_seq_inputs = keras.Input(shape=(None, EMBED_DIM), name="decoder_state_inputs")

x = keras_nlp.layers.TokenAndPositionEmbedding(
  vocabulary_size=SPA_VOCAB_SIZE,
  sequence_length=MAX_SEQUENCE_LENGTH,
  embedding_dim=EMBED_DIM,
  mask_zero=True,
)(decoder_inputs)

x = keras_nlp.layers.TransformerDecoder(
  intermediate_dim=INTERMEDIATE_DIM, num_heads=NUM_HEADS
)(decoder_sequence=x, encoder_sequence=encoded_seq_inputs)
x = keras.layers.Dropout(0.5)(x)
decoder_outputs = keras.layers.Dense(SPA_VOCAB_SIZE, activation="softmax")(x)
decoder = keras.Model(
  [
    decoder_inputs,
    encoded_seq_inputs,
  ],
  decoder_outputs,
)
decoder_outputs = decoder([decoder_inputs, encoder_outputs])

transformer = keras.Model(
  [encoder_inputs, decoder_inputs],
  decoder_outputs,
  name="transformer",
)

transformer.summary()
transformer.compile(
  "rmsprop", loss="sparse_categorical_crossentropy", metrics=["accuracy"]
)
transformer.fit(train_ds, epochs=EPOCHS, validation_data=val_ds)

def decode_sequences(input_sentences):
  batch_size = tf.shape(input_sentences)[0]

  # Tokenize the encoder input.
  encoder_input_tokens = eng_tokenizer(input_sentences).to_tensor(
    shape=(None, MAX_SEQUENCE_LENGTH)
  )

  # Define a function that outputs the next token's probability given the
  # input sequence.
  def next(prompt, cache, index):
    logits = transformer([encoder_input_tokens, prompt])[:, index - 1, :]
    # Ignore hidden states for now; only needed for contrastive search.
    hidden_states = None
    return logits, hidden_states, cache

  # Build a prompt of length 40 with a start token and padding tokens.
  length = 40
  start = tf.fill((batch_size, 1), spa_tokenizer.token_to_id("[START]"))
  pad = tf.fill((batch_size, length - 1), spa_tokenizer.token_to_id("[PAD]"))
  prompt = tf.concat((start, pad), axis=-1)

  generated_tokens = keras_nlp.samplers.GreedySampler()(
    next,
    prompt,
    end_token_id=spa_tokenizer.token_to_id("[END]"),
    index=1,  # Start sampling after start token.
  )
  generated_sentences = spa_tokenizer.detokenize(generated_tokens)
  return generated_sentences


test_eng_texts = [pair[0] for pair in test_pairs]
for i in range(2):
  input_sentence = random.choice(test_eng_texts)
  translated = decode_sequences(tf.constant([input_sentence]))
  translated = translated.numpy()[0].decode("utf-8")
  translated = (
    translated.replace("[PAD]", "")
    .replace("[START]", "")
    .replace("[END]", "")
    .strip()
  )
  print(f"** Example {i} **")
  print(input_sentence)
  print(translated)
  print()

rouge_1 = keras_nlp.metrics.RougeN(order=1)
rouge_2 = keras_nlp.metrics.RougeN(order=2)

for test_pair in test_pairs[:30]:
  input_sentence = test_pair[0]
  reference_sentence = test_pair[1]

  translated_sentence = decode_sequences(tf.constant([input_sentence]))
  translated_sentence = translated_sentence.numpy()[0].decode("utf-8")
  translated_sentence = (
    translated_sentence.replace("[PAD]", "")
    .replace("[START]", "")
    .replace("[END]", "")
    .strip()
  )

  rouge_1(reference_sentence, translated_sentence)
  rouge_2(reference_sentence, translated_sentence)

print("ROUGE-1 Score: ", rouge_1.result())
print("ROUGE-2 Score: ", rouge_2.result())