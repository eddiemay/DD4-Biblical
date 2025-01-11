import cv2
import json
import os
import random
import shutil
import subprocess
from multiprocessing import Pool
from pathlib import Path
from urllib import request
from train_by_font import cache_bible
from train_by_font import training_text_file
from utility import unfinalize

letter_box_file = 'letter_boxes.json'
API_BASE = 'https://dd4-biblical.appspot.com/_api/'
LETTERBOX_BY_FRAGMENT_URL = API_BASE + 'letterBoxs/v1/list?filter=filename={}&pageSize=0&orderBy=y1'
BASE_OUTPUT = 'tesstrain/data/'
output_directory = f'{BASE_OUTPUT}embedding-ground-truth'
base_image = cv2.imread('../images/isaiah/columns/column_9_54.jpg')
background = base_image[1570:2200, 0:1127]
starty = 90
startx = 1100
left_limit = 90
letter_map = {}
fragment_map = {}
line_spacing = 60
min_space = 5
max_space = 10
override_letter_cache = False


def cache_letter_boxes(override_letter_cache=False):
    if os.path.exists(letter_box_file) and not override_letter_cache:
        return

    fragments = [
        'isaiah-column-2', 'isaiah-column-4', 'isaiah-column-9', 'isaiah-column-14',
        'isaiah-column-20', 'isaiah-column-27', 'isaiah-column-36', 'isaiah-column-44',
        'isaiah-column-45', 'isaiah-column-47', 'isaiah-column-48', 'isaiah-column-53']
    # Open the file for write.
    print('Writing file: ', letter_box_file)
    with (open(letter_box_file, "w", encoding="utf-8") as f):
        for fragment in fragments:
            letterbox_url = LETTERBOX_BY_FRAGMENT_URL.format(fragment)
            print('Sending request: ', letterbox_url)
            with request.urlopen(letterbox_url) as url:
                response = json.load(url)
                print('Response: ', response)
                letterboxes = response['items']
                # Dump each scripture verse into the file.
                for letterbox in letterboxes:
                    json.dump(letterbox, f)
                    f.write("\n")


def get_random_letter_box(letter):
    if not letter_map:
        print('creating letter map')
        with open(letter_box_file, "r", encoding="utf-8") as f:
            letters = 0
            for line in f:
                letter_box = json.loads(line)
                if letter_box['type'] == 'Letter':
                    letters += 1
                    collection = letter_map.get(letter_box['value'])
                    if not collection:
                        collection = []
                        letter_map[letter_box['value']] = collection
                    collection.append(letter_box)
            print(f'{letters} letters')
            for l in sorted(letter_map):
                print(f'{l}: {len(letter_map.get(l))}')

    collection = letter_map.get(letter)
    if collection:
        return collection[random.randrange(0, len(collection))]
    print(f'Found none for "{letter}"')
    return None


def process(sample):
    output_img = background.copy()
    x2 = startx
    y2 = starty
    sample['boxes'] = []
    boxes = [{'value': '\t', 'x1': x2, 'y1': y2, 'x2': x2 + 1, 'y2': y2 + 1}]
    for l in sample['text']:
        if l == '\n':
            x2 = startx
            y2 += line_spacing
            for box in reversed(boxes):
                sample['boxes'].append(box)
            boxes = [{
                'value': '\t', 'x1': x2, 'y1': y2, 'x2': x2 + 1, 'y2': y2 + 1}]
        elif l == ' ':
            if x2 < left_limit + max_space:
                x2 = startx
                y2 += line_spacing
                for box in reversed(boxes):
                    sample['boxes'].append(box)
                boxes = [{'value': '\t',
                          'x1': x2, 'y1': y2, 'x2': x2 + 1, 'y2': y2 + 1}]
            else:
                space = random.randrange(min_space, max_space + 1)
                # print(f'space of size: {space}')
                boxes.append({'value': ' ',
                              'x1': x2-space, 'y1': y2-20, 'x2': x2, 'y2': y2})
                x2 -= space
        else:
            if x2 < left_limit:
                x2 = startx
                y2 += line_spacing
                for box in reversed(boxes):
                    sample['boxes'].append(box)
                boxes = [{'value': '\t',
                          'x1': x2, 'y1': y2, 'x2': x2 + 1, 'y2': y2 + 1}]
            letter_box = get_random_letter_box(l)
            if letter_box:
                fragment = fragment_map.get(letter_box['filename'])
                if fragment is None:
                    scroll,_,column = letter_box['filename'].split('-')
                    res = 7
                    if scroll == 'isaiah' or scroll == 'temple':
                        res = 9
                    elif scroll == 'war':
                        res = 8
                    fragment = cv2.imread(
                        f'../images/{scroll}/columns/column_{res}_{column}.jpg')
                    fragment_map[letter_box['filename']] = fragment

                width = letter_box['x2'] - letter_box['x1']
                height = letter_box['y2'] - letter_box['y1']
                x1 = x2 - width
                y1 = y2 - height
                output_img[y1:y2, x1:x2] = fragment[letter_box['y1']:letter_box['y2'], letter_box['x1']:letter_box['x2']]
                boxes.append({'value': l, 'x1': x1, 'y1': y1, 'x2': x2, 'y2': y2})
                x2 = x1

    if len(boxes) > 0:
        for box in reversed(boxes):
            sample['boxes'].append(box)
    sample['image'] = output_img
    return sample


def output_files(sample):
    training_text_file_name = Path(training_text_file).stem
    file_base_name = f'{training_text_file_name}_{sample["id"]}'

    with open(f'{output_directory}/{file_base_name}.gt.txt', 'w') as output_file:
        output_file.writelines([sample['text']])

    cv2.imwrite(f'{output_directory}/{file_base_name}.tif', sample['image'])

    with open(f'{output_directory}/{file_base_name}.box', 'w') as box_file:
        for box in sample['boxes']:
            y1 = background.shape[0] - box['y1']
            y2 = background.shape[0] - box['y2']
            box_file.write(
                f'{box["value"]} {box["x1"]} {y2} {box["x2"]} {y1} 0\n')


def process_and_output(sample):
    process(sample)
    output_files(sample)


def create_isa_6_7_11_sample():
    with open('dss_isa_6_7-11.txt', 'r') as input_file:
        sample = {'id': 'isa_6_7-11', 'text': unfinalize(input_file.read())}

    process(sample)
    cv2.imshow(sample['id'], sample['image'])
    cv2.imwrite('dss_isa_9_6_7-11_embedded.jpg', sample['image'])
    cv2.waitKey()


if __name__ == '__main__':
    # cv2.imshow('Base Image', base_image)
    # cv2.waitKey()
    # cv2.imshow('Background', background)
    # cv2.waitKey()

    cache_bible()
    cache_letter_boxes(override_letter_cache)

    # Read each training file line and put it in an array.
    lines = []
    with open(training_text_file, 'r') as input_file:
        for line in input_file.readlines():
            lines.append(unfinalize(line.strip()))

    # Git clone the training programs if they don't exist.
    if not os.path.exists(BASE_OUTPUT):
        subprocess.run(
            ['git', 'clone', 'https://github.com/tesseract-ocr/tesstrain'])
        subprocess.run(
            ['git', 'clone', 'https://github.com/tesseract-ocr/tessdata_best'])

    # Delete and recreate the training data directories.
    if os.path.exists(output_directory):
        shutil.rmtree(output_directory)
    if os.path.exists(BASE_OUTPUT + 'embedding'):
        shutil.rmtree(BASE_OUTPUT + 'embedding')
    if os.path.exists(BASE_OUTPUT + 'embedding' + '.traineddata'):
        Path(BASE_OUTPUT + 'embedding' + '.traineddata').unlink()

    Path(output_directory).mkdir(parents=True, exist_ok=True)

    samples = [{'id': 'gen_1_1', 'text': lines[0]}, {
        'id': 'gen_1_1-7',
        'text': f'{lines[0]}  {lines[1]}  {lines[2]}  {lines[3]}  {lines[4]}\n'
                f'{lines[5]}  {lines[6]}  {lines[7]}'}]

    create_isa_6_7_11_sample()

    process(samples[0])
    output_files(samples[0])
    cv2.imshow('Gen 1:1', samples[0]['image'])
    process(samples[1])
    output_files(samples[1])
    cv2.imshow('Gen 1:1-7', samples[1]['image'])
    cv2.waitKey()
    # Close all windows
    cv2.destroyAllWindows()

    # The edge cases are a must-have.
    with open('edge_cases.txt', 'r') as edge_cases:
        samples.append({'id': 'edge_cases', 'text': unfinalize(edge_cases.read())})

    # Randomly pick samples from the training set to include.
    # random.shuffle(lines)
    for l in range(0, len(lines) - 3, 4):
        samples.append({
            'id': l / 4,
            'text': f'{lines[l]}  {lines[l+1]}\n{lines[l+2]}  {lines[l+3]}'})

    with Pool() as pool:
        pool.map(process_and_output, samples)

    # Need to change directory to tesstrain then run the following:
    # subprocess.run(['make', 'tesseract-langdata']) #once
    # make training MODEL_NAME=embedding START_MODEL=heb TESSDATA=../tessdata_best MAX_ITERATIONS=4096
    # cp data/embedding.traineddata /opt/homebrew/share/tessdata
