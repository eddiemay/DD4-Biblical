import numpy as np
import keras
import os

'''
Number of samples: 10000
Number of unique input tokens: 75
Number of unique output tokens: 40
Max sequence length for inputs: 20
Max sequence length for outputs: 17
'''

data_path = "../../../data"
latent_dim = 128  # Latent dimensionality of the encoding space.
max_encoder_seq_length = 20
max_decoder_seq_length = 17

# Define sampling models
# Restore the model and construct the encoder and decoder.
model = keras.models.load_model("seq2seq_model")

print(model.summary())

encoder_inputs = model.input[0]  # input_1
encoder_outputs, state_h_enc, state_c_enc = model.layers[2].output  # lstm_1
encoder_states = [state_h_enc, state_c_enc]
encoder_model = keras.Model(encoder_inputs, encoder_states)

print("encoder_inputs: ", encoder_inputs.name)
print("encoder_outputs: ", encoder_outputs.name)

decoder_inputs = model.input[1]  # input_2
decoder_state_input_h = keras.Input(shape=(latent_dim,))
decoder_state_input_c = keras.Input(shape=(latent_dim,))
decoder_states_inputs = [decoder_state_input_h, decoder_state_input_c]
decoder_lstm = model.layers[3]
decoder_outputs, state_h_dec, state_c_dec = decoder_lstm(
  decoder_inputs, initial_state=decoder_states_inputs
)
decoder_states = [state_h_dec, state_c_dec]
decoder_dense = model.layers[4]
decoder_outputs = decoder_dense(decoder_outputs)
decoder_model = keras.Model(
  [decoder_inputs] + decoder_states_inputs, [decoder_outputs] + decoder_states
)
num_encoder_tokens = encoder_inputs.shape[2]
num_decoder_tokens = decoder_inputs.shape[2]
print("Number of unique input tokens:", num_encoder_tokens)
print("Number of unique output tokens:", num_decoder_tokens)
print("Max sequence length for inputs:", max_encoder_seq_length)
print("Max sequence length for outputs:", max_decoder_seq_length)

# Load the target tokens from file
with open("target_tokens.txt", "r", encoding="utf-8") as f:
  target_characters = f.read().split("\n")
target_characters = target_characters[: len(target_characters) - 1]

# Load the target tokens from file
with open("input_tokens.txt", "r", encoding="utf-8") as f:
  input_characters = f.read().split("\n")
input_characters = input_characters[: len(input_characters) - 1]

print("Input characters length: ", len(input_characters))
print("Target characters length: ", len(target_characters))

# Reverse-lookup token index to decode sequences back to
# something readable.
input_token_index = dict([(char, i) for i, char in enumerate(input_characters)])
reverse_input_char_index = dict((i, char) for char, i in input_token_index.items())
target_token_index = dict([(char, i) for i, char in enumerate(target_characters)])
reverse_target_char_index = dict((i, char) for char, i in target_token_index.items())

def encode_input(input_text):
  input_seq = np.zeros(
    (1, max_encoder_seq_length, num_encoder_tokens),
    dtype="float32",
  )
  for t, char in enumerate(input_text):
    input_seq[0, t, input_token_index[char]] = 1.0
  input_seq[0, t + 1 :, input_token_index[" "]] = 1.0
  return input_seq

def decode_sequence(input_seq):
  # Encode the input as state vectors.
  states_value = encoder_model.predict(input_seq, verbose=0)

  # Generate empty target sequence of length 1.
  target_seq = np.zeros((1, 1, num_decoder_tokens))
  # Populate the first character of target sequence with the start character.
  target_seq[0, 0, target_token_index["\t"]] = 1.0

  # Sampling loop for a batch of sequences
  # (to simplify, here we assume a batch of size 1).
  stop_condition = False
  decoded_sentence = ""
  while not stop_condition:
    output_tokens, h, c = decoder_model.predict(
      [target_seq] + states_value, verbose=0
    )

    # Sample a token
    sampled_token_index = np.argmax(output_tokens[0, -1, :])
    sampled_char = reverse_target_char_index[sampled_token_index]
    decoded_sentence += sampled_char

    # Exit condition: either hit max length or find stop character.
    if sampled_char == " " or len(decoded_sentence) > max_decoder_seq_length:
      stop_condition = True

    # Update the target sequence (of length 1).
    target_seq = np.zeros((1, 1, num_decoder_tokens))
    target_seq[0, 0, sampled_token_index] = 1.0

    # Update states
    states_value = [h, c]
  return decoded_sentence

examples = [
  ["חֲזוֹן֙", "חזון"],
  ["יְשַֽׁעְיָ֣הוּ", "ישעיהו"],
  ["בֶן־", "בן"],
  ["וִירוּשָׁלִָ֑ם", "וירושלים"],
  ["עֻזִּיָּ֧הוּ", "עוזיה"],
  ["יֻלַּד־", "יולד"],
  ["מִ֤י", "מיא"],
  ["אֱלֹהֵ֖ינוּ", "אלוהינו"],
  ["לֹא־", "לוא"]
]

for example in examples:
  # Take one sequence (part of the training set) for trying out decoding.
  decoded_sentence = decode_sequence(encode_input(example[0]))
  print("-")
  print("Input  :", example[0])
  print("Target :", example[1])
  print("Decoded:", decoded_sentence)

with open(os.path.join(data_path, "isa-word-map.csv"), "r", encoding="utf-8") as f:
  lines = f.read().split("\n")
with open(os.path.join(data_path, "isa-word-map-processed.csv"), "w", encoding="utf-8") as f:
  f.write("Id,MT Word,DSS Word,ML Word\n")
  processed = 0
  missmatches = 0

  needfixing = 0
  fixed = 0
  fixedNotAttempted = 0
  nonSuccessFix = 0

  noFixNeeded = 0
  correctNoChange = 0
  falseChange = 0
  for line in lines[1:]:
    id, input, target, ld, constantsOnly = line.split(",")
    ld = int(ld)
    if (ld < 2 and processed < 100):
      decoded = decode_sequence(encode_input(input)).strip()
      f.write("{0},{1},{2},{3}\n".format(id, input, target, decoded))
      processed += 1
      print(id)
      print("Input  :", input)
      print("Target :", target)
      print("Decoded:", decoded)
      if (target != constantsOnly):
        needfixing += 1
        if (target == decoded):
          fixed += 1
        elif (constantsOnly == decoded):
          fixedNotAttempted += 1
        else:
          nonSuccessFix += 1
      if (target == constantsOnly):
        noFixNeeded += 1
        if (target == decoded):
          correctNoChange += 1
        else:
          falseChange += 1
      if (target != decoded):
        missmatches += 1
        print("missmatch!")

  print("Matching on {0} out of {1}".format(processed - missmatches, processed))
  print("Fixed {0} out of {1}".format(fixed, needfixing))
  print("Non fix attempted {0} out of {1}".format(fixedNotAttempted, needfixing))
  print("Non success fix {0} out of {1}".format(nonSuccessFix, needfixing))
  print("Remove Nikkuds correctly {0} out of {1}".format(correctNoChange, noFixNeeded))
  print("Broke {0} out of {1}".format(falseChange, noFixNeeded))