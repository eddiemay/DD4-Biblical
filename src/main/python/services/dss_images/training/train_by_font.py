import json
import os
import random
import shutil
import subprocess
from multiprocessing import Pool
from pathlib import Path
from urllib import request

training_text_file = 'bible_heb.txt'
BASE_MODEL = 'Hebrew'
font = 'DSS Paleo'
MODEL_NAME = f'{BASE_MODEL}_Font'
API_BASE = 'https://dd4-biblical.appspot.com/_api/'
BOOK_INFO_URL = API_BASE + 'books/v1/get?id={}'
SEARCH_URL = API_BASE + 'scriptures/v1/fetch?searchText={}+{}&lang=he-re'
BASE_OUTPUT = 'tesstrain/data/'
output_directory = f'{BASE_OUTPUT}{MODEL_NAME}-ground-truth'


def cache_bible():
    if os.path.exists(training_text_file):
        return

    # If the training file does not exist create it using the Hebrew Scriptures.
    books = ['Gen', 'Exo', 'Lev', 'Num', 'Deut', 'Josh', 'Jdg', 'Ruth', '1Sam', '2Sam', '1Ki', '2Ki', '1Chr', '2Chr',
             'Ezra', 'Neh', 'Est', 'Job', 'Psa', 'Prv', 'Ecc', 'sos', 'Isa', 'Jer', 'Lam', 'Eze', 'Dan', 'Hos', 'Joel',
             'Amos', 'Ob', 'Jonah', 'Micah', 'Nah', 'Hab', 'Zep', 'Hag', 'Zec', 'Mal']
    # Open the file for write.
    print('Writing file: ', training_text_file)
    with open(training_text_file, "w", encoding="utf-8") as f:
        for book in books:
            # Get the info about the book to obtain the number of chapters.
            if (book == 'Psa'):
                chapters = 150
            elif (book == 'Est'):
                chapters = 10
            else:
                book_info_url = BOOK_INFO_URL.format(book)
                print('Sending request: ', book_info_url)
                with request.urlopen(book_info_url) as url:
                    response = json.load(url)
                    print('Response: ', response)
                    chapters = response['chapterCount']

            # For each chapter make a request to get all the verses.
            for chapter in range(1, chapters + 1):
                search_url = SEARCH_URL.format(book, chapter)
                print('Sending request: ', search_url)
                with request.urlopen(search_url) as url:
                    response = json.load(url)
                    print('Response: ', response)
                    scriptures = response['items']
                    # Dump each scripture verse into the file.
                    for scripture in scriptures:
                        f.write(scripture['text'].replace('׃', '')
                                .replace('־', ' ').replace('׀', ''))
                        f.write('\n')


def output_files(sample):
    training_text_file_name = Path(training_text_file).stem
    file_base_name = f'{training_text_file_name}_{sample["id"]}'
    line_training_text = f'{output_directory}/{file_base_name}.gt.txt'
    with open(line_training_text, 'w') as output_file:
        output_file.writelines([sample['text']])

    subprocess.run([
        'text2image',
        f'--font={font}',
        f'--text={line_training_text}',
        f'--outputbase={output_directory}/{file_base_name}',
        '--max_pages=1',
        '--strip_unrenderable_words',
        '--leading=18',
        f'--xsize={969 * 1}',
        '--ysize=396',
        '--char_spacing=0.125',
        '--exposure=0',
        '--ptsize=8',
        '--margin=20'
    ])


if __name__ == '__main__':
    cache_bible()

    # Read each training file line and put it in an array.
    lines = []
    with open(training_text_file, 'r') as input_file:
        for line in input_file.readlines():
            lines.append(line.strip())

    # Git clone the training programs if they don't exist.
    if not os.path.exists(BASE_OUTPUT):
        subprocess.run(['git', 'clone', 'https://github.com/tesseract-ocr/tesstrain'])
        subprocess.run(['git', 'clone', 'https://github.com/tesseract-ocr/tessdata_best'])

    # Delete and recreate the training data directories.
    if os.path.exists(output_directory):
        shutil.rmtree(output_directory)
    if os.path.exists(BASE_OUTPUT + MODEL_NAME):
        shutil.rmtree(BASE_OUTPUT + MODEL_NAME)
    if os.path.exists(BASE_OUTPUT + MODEL_NAME + '.traineddata'):
        Path(BASE_OUTPUT + MODEL_NAME + '.traineddata').unlink()

    Path(output_directory).mkdir(parents=True, exist_ok=True)

    samples = []

    # The edge cases are a must-have.
    with open('edge_cases.txt', 'r') as edge_cases:
        samples.append({'id': 'edge_cases', 'text': edge_cases.read()})

    # Randomly pick samples from the training set to include.
    random.shuffle(lines)
    for l in range(4096):
        samples.append({
            'id': l,
            'text': f'{lines[l]}  {lines[l+1]}\n{lines[l+2]}  {lines[l+3]}'})

    with Pool() as pool:
      pool.map(output_files, samples)

    os.chdir('tesstrain')
    start_model = 'script/Hebrew' if BASE_MODEL == 'Hebrew' else BASE_MODEL
    command = ['make', 'training', f'MODEL_NAME={MODEL_NAME}', f'START_MODEL={start_model}',
               'TESSDATA=../tessdata_best', 'MAX_ITERATIONS=8192']
    print(command)
    subprocess.run(command)
    subprocess.run(['cp', f'data/{MODEL_NAME}.traineddata', '/opt/homebrew/share/tessdata'])
