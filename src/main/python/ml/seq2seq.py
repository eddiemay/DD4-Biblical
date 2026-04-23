# Original taken from https://keras.io/examples/nlp/lstm_seq2seq/
import numpy as np
import keras
import os
from matplotlib import pyplot

data_path = "../../../../../data"
latent_dim = 128  # Latent dimensionality of the encoding space.


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
        return (self.target() == self.constantsOnly
            or self.diff == "+ו"
            or self.diff == "+י")

    def to_csv(self):
        return "{0},{1},{2},{3}\n".format(self.id, self.mt, self.dss, self.decoded)


if __name__ == "__main__":
    batch_size = 32  # Batch size for training.
    epochs = 400  # Number of epochs to train for.
    num_samples = 20000  # Number of samples to train on.
    # Path to the data txt file on disk.
    file_path = os.path.join(data_path, "isa-word-map.csv")

    # Vectorize the data.
    input_texts = []
    target_texts = []
    input_characters = set()
    target_characters = set()
    with open(file_path, "r", encoding="utf-8") as f:
        lines = f.read().split("\n")
    for line in lines[1:]:
        i = Interlinear(line)
        # We use "tab" as the "start sequence" character
        # for the targets, and " " as "end sequence" character.
        if len(input_texts) < num_samples and i.is_candidate():
            target_text = "\t" + i.target() + " "
            input_texts.append(i.input())
            target_texts.append(target_text)
            for char in target_text:
                if char not in target_characters:
                    target_characters.add(char)

        for char in i.input():
            if char not in input_characters:
                input_characters.add(char)

    input_characters = sorted(list(input_characters))
    target_characters = sorted(list(target_characters))
    num_encoder_tokens = len(input_characters)
    num_decoder_tokens = len(target_characters)
    max_encoder_seq_length = max([len(txt) for txt in input_texts])
    max_decoder_seq_length = max([len(txt) for txt in target_texts])

    print("Number of samples:", len(input_texts))
    print("Number of unique input tokens:", num_encoder_tokens)
    print("Number of unique output tokens:", num_decoder_tokens)
    print("Max sequence length for inputs:", max_encoder_seq_length)
    print("Max sequence length for outputs:", max_decoder_seq_length)

    input_token_index = dict([(char, i) for i, char in enumerate(input_characters)])
    target_token_index = dict([(char, i) for i, char in enumerate(target_characters)])

    encoder_input_data = np.zeros(
        (len(input_texts), max_encoder_seq_length, num_encoder_tokens), dtype="float32",
    )
    decoder_input_data = np.zeros(
        (len(input_texts), max_decoder_seq_length, num_decoder_tokens), dtype="float32",
    )
    decoder_target_data = np.zeros(
        (len(input_texts), max_decoder_seq_length, num_decoder_tokens), dtype="float32",
    )

    for i, (input_text, target_text) in enumerate(zip(input_texts, target_texts)):
        for t, char in enumerate(input_text):
            encoder_input_data[i, t, input_token_index[char]] = 1.0
        encoder_input_data[i, t + 1 :, input_token_index[" "]] = 1.0
        for t, char in enumerate(target_text):
            # decoder_target_data is ahead of decoder_input_data by one timestep
            decoder_input_data[i, t, target_token_index[char]] = 1.0
            if t > 0:
                # decoder_target_data will be ahead by one timestep
                # and will not include the start character.
                decoder_target_data[i, t - 1, target_token_index[char]] = 1.0
        decoder_input_data[i, t + 1 :, target_token_index[" "]] = 1.0
        decoder_target_data[i, t:, target_token_index[" "]] = 1.0

    # Save the input tokens to a file
    with open("input_tokens.txt", "w", encoding="utf-8") as f:
        for token in input_token_index:
            f.write(token + "\n")

    # Save the target tokens to a file
    with open("target_tokens.txt", "w", encoding="utf-8") as f:
        for token in target_token_index:
            f.write(token + "\n")

    # Define an input sequence and process it.
    encoder_inputs = keras.Input(shape=(None, num_encoder_tokens), name='encoder_inputs')
    encoder = keras.layers.LSTM(latent_dim, return_state=True)
    encoder_outputs, state_h, state_c = encoder(encoder_inputs)

    # We discard `encoder_outputs` and only keep the states.
    encoder_states = [state_h, state_c]

    # Set up the decoder, using `encoder_states` as initial state.
    decoder_inputs = keras.Input(shape=(None, num_decoder_tokens), name='decoder_inputs')

    # We set up our decoder to return full output sequences,
    # and to return internal states as well. We don't use the
    # return states in the training model, but we will use them in inference.
    decoder_lstm = keras.layers.LSTM(latent_dim, return_sequences=True, return_state=True)
    decoder_outputs, _, _ = decoder_lstm(decoder_inputs, initial_state=encoder_states)
    decoder_dense = keras.layers.Dense(num_decoder_tokens, activation="softmax")
    decoder_outputs = decoder_dense(decoder_outputs)

    # Define the model that will turn
    # `encoder_input_data` & `decoder_input_data` into `decoder_target_data`
    model = keras.Model([encoder_inputs, decoder_inputs], decoder_outputs)

    optimizer = 'rmsprop'
    # optimizer = keras.optimizers.Adam(learning_rate=0.01)
    loss = 'categorical_crossentropy'
    # loss = 'binary_crossentropy'
    # loss = 'mse'
    model.compile(optimizer=optimizer, loss=loss, metrics=['accuracy'])

    early_stopping_monitor = keras.callbacks.EarlyStopping(
        monitor='loss',
        min_delta=0,
        patience=10,
        verbose=1,
        mode='auto',
        baseline=None,
        restore_best_weights=True
    )

    checkpoint = keras.callbacks.ModelCheckpoint(
        # 'model-{epoch:03d}-{val_loss:03f}.keras',
        'seq2seq_model-best-checkpoint.keras',
        verbose=1, monitor='val_loss', save_best_only=True, mode='auto')

    history = model.fit(
        [encoder_input_data, decoder_input_data],
        decoder_target_data,
        batch_size=batch_size,
        epochs=epochs,
        validation_split=0.4,
        callbacks=[checkpoint, early_stopping_monitor]
    )
    # Save model
    model.save("seq2seq_model.keras")

    # plot train and validation loss
    pyplot.plot(history.history['loss'])
    pyplot.plot(history.history['val_loss'])
    pyplot.title('model train vs validation loss')
    pyplot.ylabel('loss')
    pyplot.xlabel('epoch')
    pyplot.legend(['train', 'validation'], loc='upper right')
    pyplot.show()
