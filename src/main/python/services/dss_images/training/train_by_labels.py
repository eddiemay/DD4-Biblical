import cv2
import math
import numpy as np
import os
import random
import shutil
import subprocess
import time
from letterbox_utils import DSSLettersDataset, TRAINING_SET, get_img_file_path
from pathlib import Path
from verify import process_image

BASE_MODEL = 'Hebrew_Font_Embedding'
BASE_OUTPUT = 'tesstrain/data/'
ITERATIONS = 20
MODEL_NAME = f'{BASE_MODEL}_Label_{len(TRAINING_SET)}_more_rows_{ITERATIONS}K'
output_directory = f'{BASE_OUTPUT}label-ground-truth'
FILE_BASE_NAME = "{}_res_{}_rows_{}_to_{}"
row_map = {}
img_map = {}


def is_in_row(row_box, letter_box):
    if (row_box['filename'] != letter_box['filename']
        or row_box['y2'] < letter_box['y2']
        or row_box['x1'] > letter_box['x1']
        or row_box['x2'] < letter_box['x2']):
        return False

    coords = row_box['coords']
    ci = 0
    while coords[ci + 1]['x'] <= letter_box['x1']:
        ci += 1
    slope = (coords[ci + 1]['y'] - coords[ci]['y']) / (coords[ci + 1]['x'] - coords[ci]['x'])
    yAtX = (letter_box['x1'] - coords[ci]['x']) * slope + coords[ci]['y']

    return yAtX >= letter_box['y2']


def get_row(filename, row):
    if not row_map:
        print('creating row map')
        letter_boxes = []
        row_boxes = []
        dataset = DSSLettersDataset()
        for _, _, letter_box in dataset:
            if letter_box['type'] == 'Row':
                letter_box['_letterBoxes'] = []
                row_boxes.append(letter_box)
                letter_box['id'] = f'{letter_box["filename"]}-{letter_box["value"]}'
                row_map[letter_box['id']] = letter_box
            elif letter_box['type'] == 'Letter':
                letter_boxes.append(letter_box)
        print(f'{len(row_boxes)} total rows')
        print(f'{len(letter_boxes)} total letters')
        row_boxes = sorted(row_boxes, key=lambda b:b['y2'])

        added_letters = 0
        for letter_box in letter_boxes:
            for row_box in row_boxes:
                if is_in_row(row_box, letter_box):
                    row_box['_letterBoxes'].append(letter_box)
                    added_letters += 1
                    break

        print(f'{added_letters} letters added')
        row_boxes = sorted(row_boxes, key=lambda r:r['id'])
        for row_box in row_boxes:
            print(row_box['id'] + ":", len(row_box['_letterBoxes']))
            row_box['_letterBoxes'] = sorted(
                row_box['_letterBoxes'], key=lambda b:b['x2'], reverse=True)

    row_box = row_map.get(f'{filename}-{row}')
    if row_box is None and 1 < row < 31:
        print(f'Found none for {filename} Row: {row}')

    return row_box


def get_box_text(row_box, bottom, ratio):
    boxes = []
    if len(row_box['_letterBoxes']) == 0:
        print(f'No letter boxes for {row_box}')
        return boxes

    prev_x2 = None
    for letter_box in reversed(row_box['_letterBoxes']):
        x1 = math.floor(letter_box['x1'] * ratio)
        x2 = math.ceil(letter_box['x2'] * ratio)
        y1 = bottom - math.ceil(letter_box['y2'] * ratio)
        y2 = bottom - math.floor(letter_box['y1'] * ratio)
        if prev_x2 and letter_box['x1'] - prev_x2 >= 5:
            boxes.append({'value': ' ', 'x1': math.ceil(prev_x2 * ratio),
                          'y1': y1, 'x2': x1, 'y2': y2})
        value = letter_box['value']
        boxes.append({'id': letter_box['id'], 'value': value,
                      'x1': x1, 'y1': y1, 'x2': x2, 'y2': y2})
        prev_x2 = letter_box['x2']

    boxes.append({'value': '\t', 'x1': x2, 'y1': y1,
                  'x2': math.ceil(x2 + 2 * ratio), 'y2': y2})
    return boxes


def get_text(row_box):
    # Get the text of the row
    txt = ''
    prev_x1 = None
    for letter_box in row_box['_letterBoxes']:
        if prev_x1 and prev_x1 - letter_box['x2'] >= 5:
            txt += ' '
        prev_x1 = letter_box['x1']
        txt += letter_box['value']
    return txt


def process(sample):
    fragment = sample['fragment']
    res = sample.get('res') or 9 # random.randrange(8, 11)
    sample['res'] = res

    if res == 10:
        ratio = 2
    elif res == 8:
        ratio = .5
    else:
        ratio = 1

    srow, erow = sample['srow'], sample['erow']
    start_row_box = get_row(fragment, srow)
    end_row_box = get_row(fragment, erow)
    if start_row_box is None or end_row_box is None:
        return None

    img_filename = get_img_file_path(fragment, res)
    img = img_map.get(img_filename)
    if img is None:
        img = cv2.imread(img_filename)
        img, _ = process_image(img, sample.get('preprocessor'))
        img_map[img_filename] = img

    top = math.floor(start_row_box['y1'] * ratio)
    bottom = math.ceil(end_row_box['y2'] * ratio)

    row_img = img[top:bottom]
    sample['img'] = row_img

    sample['text'] = ''
    sample['boxes'] = []
    outlined = row_img.copy()
    for row in range(srow, erow + 1):
        row_box = get_row(fragment, row)
        sample['text'] += get_text(row_box) + '\n'
        sample['boxes'].extend(get_box_text(row_box, bottom, ratio))

        for letter_box in row_box['_letterBoxes']:
            x1 = math.floor(letter_box['x1'] * ratio)
            x2 = math.ceil(letter_box['x2'] * ratio)
            y1 = math.floor(letter_box['y1'] * ratio) - top
            y2 = math.ceil(letter_box['y2'] * ratio) - top
            cv2.rectangle(outlined, (x1, y1), (x2, y2), (0, 255, 0), 2)

    # Square the letter boxes

    visual_threshold = 1.333
    # Square in red the letters from the rows above and below that to be cleared
    # Create a black mask with the same size as the image
    mask = np.zeros(row_img.shape[:2], dtype=np.uint8)
    above = get_row(fragment, srow - 1)
    if above:
        for letter_box in above['_letterBoxes']:
            y2 = math.ceil(letter_box['y2'] * ratio)
            if y2 > top:
                x1 = math.floor(letter_box['x1'] * ratio)
                x2 = math.ceil(letter_box['x2'] * ratio)
                y1 = math.floor(letter_box['y1'] * ratio)
                h = y2 - y1
                y1, y2 = max(y1 - top, 0), y2 - top
                vh = y2 - y1 # Calculate the visual height
                cv2.rectangle(outlined, (x1, y1), (x2, y2), (0, 0, 255), 2)
                if vh / h > visual_threshold:
                    sample['boxes'].append({
                        'id': letter_box['id'], 'value': letter_box['value'],
                        'x1': x1, 'y1': y1, 'x2': x2, 'y2': y2})
                else:
                    cv2.rectangle(mask, (x1, y1), (x2, y2), 255, -1)

    below = get_row(fragment, erow + 1)
    if below:
        for letter_box in below['_letterBoxes']:
            y1 = math.floor(letter_box['y1'] * ratio)
            if y1 < bottom:
                x1 = math.floor(letter_box['x1'] * ratio)
                x2 = math.ceil(letter_box['x2'] * ratio)
                y2 = math.ceil(letter_box['y2'] * ratio)
                h = y2 - y1
                y1, y2 = y1 - top,  min(y2 - top, bottom - top)
                vh = y2 - y1
                cv2.rectangle(outlined, (x1, y1), (x2, y2), (0, 0, 255), 2)
                if vh / h > visual_threshold:
                    sample['boxes'].append({
                        'id': letter_box['id'], 'value': letter_box['value'],
                        'x1': x1, 'y1': y1, 'x2': x2, 'y2': y2})
                else:
                    cv2.rectangle(mask, (x1, y1), (x2, y2), 255, -1)

    # Create a 4-channel image with alpha channel
    cleared = cv2.cvtColor(row_img, cv2.COLOR_BGR2BGRA)
    # Set the alpha channel to 0 where the mask is white
    cleared[mask == 255, 3] = 0
    sample['outlined'] = outlined
    sample['image'] = cleared

    return sample


def add_salt_and_pepper(img, amount=0.01):
    noisy = img.copy()
    ''' rand = np.random.rand(*img.shape[:2])

    noisy[rand < amount / 2] = 0
    noisy[rand > 1 - amount / 2] = 255 '''

    return noisy


def output_files(sample):
    file_base_name = FILE_BASE_NAME.format(
        sample['fragment'], sample['res'], sample['srow'], sample['erow'])

    with open(f'{output_directory}/{file_base_name}.gt.txt', 'w') as output_file:
        output_file.writelines([sample['text']])

    cv2.imwrite(f'{output_directory}/{file_base_name}.tif',
                add_salt_and_pepper(sample['image']))

    with open(f'{output_directory}/{file_base_name}.box', 'w') as box_file:
        for box in sample['boxes']:
            box_file.write(f'{box["value"]} {box["x1"]} {box["y1"]} {box["x2"]} {box["y2"]} 0\n')


def process_and_output(sample):
    if process(sample):
        output_files(sample)


def display(sample):
    print(sample['text'])
    print(sample['boxes'])
    output_base = FILE_BASE_NAME.format(sample['fragment'], sample['res'], sample['srow'], sample['erow'])

    cv2.imshow(f'{output_base} cleared', sample["image"])
    cv2.imshow(f'{output_base} outlined', sample["outlined"])
    cv2.imshow(output_base, sample["img"])
    cv2.waitKey(0)


if __name__ == '__main__':
    start_time = time.time()
    # for r in range(1, 8):
        # output_row('isaiah', 44, r)
    display(process({'fragment': 'isaiah-column-44', 'srow': 25, 'erow': 25, 'res': 10}))
    display(process({'fragment': 'isaiah-column-44', 'srow': 25, 'erow': 27, 'res': 9}))
    display(process({'fragment': 'isaiah-column-44', 'srow': 1, 'erow': 7, 'res': 8}))
    display(process({'fragment': 'isaiah-column-44', 'srow': 1, 'erow': 28, 'res': 8}))

    # Git clone the training programs if they don't exist.
    if not os.path.exists(BASE_OUTPUT):
        subprocess.run(['git', 'clone', 'https://github.com/tesseract-ocr/tesstrain'])
        subprocess.run(['git', 'clone', 'https://github.com/tesseract-ocr/tessdata_best'])
        subprocess.run(['brew', 'install', 'wget'])
        subprocess.run(['cp', 'tesstrain_Makefile_gcc3.81', 'tesstrain/Makefile'])
        os.chdir('tesstrain')
        subprocess.run(['make', 'tesseract-langdata'])
        os.chdir('../')

    # Delete and recreate the training data directories.
    if os.path.exists(output_directory):
        shutil.rmtree(output_directory)
    if os.path.exists(BASE_OUTPUT + 'label'):
        shutil.rmtree(BASE_OUTPUT + 'label')
    if os.path.exists(BASE_OUTPUT + 'label' + '.traineddata'):
        Path(BASE_OUTPUT + 'label' + '.traineddata').unlink()

    Path(output_directory).mkdir(parents=True, exist_ok=True)

    image_start = time.time()
    for frag in TRAINING_SET:
        row_30 = get_row(frag, 30)
        for r in range(1, 33):
            process_and_output({'fragment': frag, 'srow': r, 'erow': r})
            if r % 3 == 1:
                process_and_output({'fragment': frag, 'srow': r, 'erow': r+2})
            if r % 7 == 1:
                process_and_output({'fragment': frag, 'srow': r, 'erow': r+6})
            if r % 10 == 1 and row_30 is not None:
                process_and_output({'fragment': frag, 'srow': r, 'erow': r+9})
            if r % 14 == 1 and row_30 is None:
                process_and_output({'fragment': frag, 'srow': r, 'erow': r+13})

    training_start = time.time()
    print(f'Files creation time: {training_start - image_start} seconds')

    os.chdir('tesstrain')
    start_model = 'script/Hebrew' if BASE_MODEL == 'Hebrew' else BASE_MODEL
    command = ['make', 'training', 'MODEL_NAME=label', f'START_MODEL={start_model}',
               'TESSDATA=../tessdata_best', f'MAX_ITERATIONS={ITERATIONS * 1024}']
    print(' '.join(command))
    subprocess.run(command)
    command = ['cp', 'data/label.traineddata', f'/opt/homebrew/share/tessdata/dabar.cloud/{MODEL_NAME}.traineddata']
    print(' '.join(command))
    subprocess.run(command)

    end_time = time.time()
    print(f'Setup time: {image_start - start_time} seconds')
    print(f'Files creation time: {training_start - image_start} seconds')
    print(f'Training time: {end_time - training_start} seconds')
    print(f'Total time: {end_time - start_time} seconds')