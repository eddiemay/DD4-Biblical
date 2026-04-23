# Original taken from https://keras.io/examples/nlp/lstm_seq2seq/
import keras
import numpy as np
import os
import time

from multiprocessing import Pool
from seq2seq import Interlinear, data_path, latent_dim

'''
Number of samples: 10000
Number of unique input tokens: 75
Number of unique output tokens: 40
Max sequence length for inputs: 20
Max sequence length for outputs: 17
'''

max_encoder_seq_length = 20
max_decoder_seq_length = 12

# Define sampling models
# Restore the model and construct the encoder and decoder.
model = keras.models.load_model("seq2seq_model.keras")

encoder_inputs = model.input[0]  # input_1
encoder_outputs, state_h_enc, state_c_enc = model.layers[2].output  # lstm_1
encoder_states = [state_h_enc, state_c_enc]
encoder_model = keras.Model(encoder_inputs, encoder_states)

decoder_inputs = model.input[1]  # input_2
decoder_state_input_h = keras.Input(shape=(latent_dim,))
decoder_state_input_c = keras.Input(shape=(latent_dim,))
decoder_states_inputs = [decoder_state_input_h, decoder_state_input_c]
decoder_lstm = model.layers[3]
decoder_outputs, state_h_dec, state_c_dec = decoder_lstm(decoder_inputs, initial_state=decoder_states_inputs)
decoder_states = [state_h_dec, state_c_dec]
decoder_dense = model.layers[4]
decoder_outputs = decoder_dense(decoder_outputs)
decoder_model = keras.Model([decoder_inputs] + decoder_states_inputs, [decoder_outputs] + decoder_states)
num_encoder_tokens = encoder_inputs.shape[2]
num_decoder_tokens = decoder_inputs.shape[2]

# Load the target tokens from file
with open("target_tokens.txt", "r", encoding="utf-8") as f:
    target_characters = f.read().split("\n")
target_characters = target_characters[: len(target_characters) - 1]

# Load the target tokens from file
with open("input_tokens.txt", "r", encoding="utf-8") as f:
    input_characters = f.read().split("\n")

input_characters = input_characters[: len(input_characters) - 1]

# Reverse-lookup token index to decode sequences back to
# something readable.
input_token_index = dict([(char, i) for i, char in enumerate(input_characters)])
target_token_index = dict([(char, i) for i, char in enumerate(target_characters)])
reverse_target_char_index = dict((i, char) for char, i in target_token_index.items())


def encode_input(input_text):
    input_seq = np.zeros((1, max_encoder_seq_length, num_encoder_tokens), dtype="float32")
    for t, char in enumerate(input_text):
        input_seq[0, t, input_token_index[char]] = 1.0
    input_seq[0, t + 1:, input_token_index[" "]] = 1.0
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
        output_tokens, h, c = decoder_model.predict([target_seq] + states_value, verbose=0)

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


def decode_interlinear(i):
    i.decoded = decode_sequence(encode_input(i.input())).strip()
    return i


if __name__ == '__main__':
    start_time = time.perf_counter()
    print(model.summary())
    print("encoder_inputs: ", encoder_inputs.name)
    print("encoder_outputs: ", encoder_outputs.name)
    print("Number of unique input tokens:", num_encoder_tokens)
    print("Number of unique output tokens:", num_decoder_tokens)
    print("Max sequence length for inputs:", max_encoder_seq_length)
    print("Max sequence length for outputs:", max_decoder_seq_length)
    print("Input characters length: ", len(input_characters))
    print("Target characters length: ", len(target_characters))

    candidates = []
    processed = []
    with open(os.path.join(data_path, "isa-word-map.csv"), "r", encoding="utf-8") as f:
        lines = f.read().split("\n")
        for line in lines[1:]:
            i = Interlinear(line)
            if candidates.__len__() < 20000 and i.is_candidate():
                candidates.append(i)

    print(f'processing {candidates.__len__()} candidates')
    #for i in candidates:
     #   processed.append(decode_interlinear(i))
    with Pool() as pool:
        processed = pool.map(decode_interlinear, candidates)

    # Process stats and write results out to file.
    with open(os.path.join(data_path, "isa-word-map-processed.csv"), "w", encoding="utf-8") as f:
        missmatches = 0

        needfixing = 0
        fixed = 0
        fixedNotAttempted = 0
        nonSuccessFix = 0

        noFixNeeded = 0
        correctNoChange = 0
        falseChange = 0

        f.write("Id,MT Word,DSS Word,ML Word\n")
        for i in processed:
            f.write(i.to_csv())
            if i.target() != i.decoded:
                print("{0}\nInput  :{1}\nTarget :{2}\nDecoded:{3}{4}".format(
                    i.id, i.input(), i.target(), i.decoded,
                    "" if i.target() == i.decoded else "\nmissmatch!"))
            if i.target() != i.constantsOnly:
                needfixing += 1
                if i.target() == i.decoded:
                    fixed += 1
                elif i.constantsOnly == i.decoded:
                    fixedNotAttempted += 1
                else:
                    nonSuccessFix += 1
            if i.target() == i.constantsOnly:
                noFixNeeded += 1
                if i.target() == i.decoded:
                    correctNoChange += 1
                else:
                    falseChange += 1
            if i.target() != i.decoded:
                missmatches += 1

    print(f"\nMatching on {processed.__len__() - missmatches} out of {processed.__len__()}")
    print(f"Fixed {fixed} out of {needfixing}")
    print(f"Non fix attempted {fixedNotAttempted} out of {needfixing}")
    print(f"Non success fix {nonSuccessFix} out of {needfixing}")
    print(f"Remove Nikkuds correctly {correctNoChange} out of {noFixNeeded}")
    print(f"Broke {falseChange} out of {noFixNeeded}")

    print(f"\nProgram finished in {time.perf_counter() - start_time} seconds")
