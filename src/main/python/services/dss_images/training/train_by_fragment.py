import cv2
import json
import math
import numpy as np
import os
import random
import shutil
import subprocess
from multiprocessing import Pool
from pathlib import Path
from train_by_embedding import cache_letter_boxes
from train_by_embedding import letter_box_file

BASE_OUTPUT = 'tesstrain/data/'
output_directory = f'{BASE_OUTPUT}fragment-ground-truth'
FILE_BASE_NAME = "{}_res_{}_rows_{}_to_{}"
row_map = {}
img_map = {}


def get_row(filename, row):
    if not row_map:
        print('creating row map')
        letter_boxes = []
        row_boxes = []
        with open(letter_box_file, "r", encoding="utf-8") as f:
            rows = 0
            for line in f:
                letter_box = json.loads(line)
                if letter_box['type'] == 'Row':
                    rows += 1
                    letter_box['_letterBoxes'] = []
                    row_boxes.append(letter_box)
                    row_map[f'{letter_box["filename"]}-{letter_box["value"]}'] = letter_box
                elif letter_box['type'] == 'Letter':
                    letter_boxes.append(letter_box)
            print(f'{rows} total rows')
            print(f'{len(letter_boxes)} total letters')
            row_boxes = sorted(row_boxes, key=lambda b: b['y2'])

            added_letters = 0
            for letter_box in letter_boxes:
                fname = letter_box['filename']
                y2 = letter_box['y2']
                for row_box in row_boxes:
                    if fname == row_box['filename'] and y2 <= row_box['y2']:
                        row_box['_letterBoxes'].append(letter_box)
                        added_letters += 1
                        break

            print(f'{added_letters} letters added')
            for row_box in row_boxes:
                row_box['_letterBoxes'] =\
                    sorted(row_box['_letterBoxes'], key=lambda b: b['x2'], reverse=True)

    row_box = row_map.get(f'{filename}-{row}')
    if row_box is None and 1 < row < 31:
        print(f'Found none for {filename} Row: {row}')
        return None

    return row_box


def get_box_text(row_box, bottom, ratio):
    boxes = []
    if len(row_box['_letterBoxes']) == 0:
        print(f'No letter boxes for {row_box}')

    prev_x2 = None
    for letter_box in reversed(row_box['_letterBoxes']):
        x1 = math.floor(letter_box['x1'] * ratio)
        x2 = math.ceil(letter_box['x2'] * ratio)
        y1 = bottom - math.ceil(letter_box['y2'] * ratio)
        y2 = bottom - math.floor(letter_box['y1'] * ratio)
        if prev_x2 and letter_box['x1'] - prev_x2 >= 5:
            boxes.append(
                {'value': ' ', 'x1': math.ceil(prev_x2 * ratio),
                 'y1': y1, 'x2': x1, 'y2': y2})
        value = letter_box['value']
        boxes.append({'value': value, 'x1': x1, 'y1': y1, 'x2': x2, 'y2': y2})
        prev_x2 = letter_box['x2']

    boxes.append({'value': '\t', 'x1': x2, 'y1': y1, 'x2': math.ceil(x2 + 2 * ratio), 'y2': y2})
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
    scroll, fragment = sample['scroll'], sample['fragment']
    res = sample.get('res') or random.randrange(8, 11)
    sample['res'] = res

    if res == 10:
        ratio = 2
    elif res == 9:
        ratio = 1
    elif res == 8:
        ratio = .5

    srow, erow = sample['srow'], sample['erow']
    filename = f'{scroll}-column-{fragment}'
    sample['filename'] = filename
    start_row_box = get_row(filename, srow)
    end_row_box = get_row(filename, erow)
    if start_row_box is None or end_row_box is None:
        return None

    img_filename = f'../images/{scroll}/columns/column_{res}_{fragment}.jpg'
    img = img_map.get(img_filename)
    if img is None:
        img = cv2.imread(img_filename)
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        gblur = cv2.GaussianBlur(gray, (3,3), sigmaX=30, sigmaY=300)
        otsu = cv2.threshold(gblur, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]
        img_map[img_filename] = img

    top = math.floor(start_row_box['y1'] * ratio)
    bottom = math.ceil(end_row_box['y2'] * ratio)

    row_img = img[top:bottom]
    sample['img'] = row_img

    sample['text'] = ''
    sample['boxes'] = []
    outlined = row_img.copy()
    for row in range(srow, erow + 1):
        row_box = get_row(filename, row)
        sample['text'] += get_text(row_box) + '\n'
        sample['boxes'].extend(get_box_text(row_box, bottom, ratio))

        for letter_box in row_box['_letterBoxes']:
            x1 = math.floor(letter_box['x1'] * ratio)
            x2 = math.ceil(letter_box['x2'] * ratio)
            y1 = math.floor(letter_box['y1'] * ratio) - top
            y2 = math.ceil(letter_box['y2'] * ratio) - top
            cv2.rectangle(outlined, (x1, y1), (x2, y2), (0, 255, 0), 2)

    # Square the letter boxes

    # Square in red the letters from the rows above and below that to be cleared
    # Create a black mask with the same size as the image
    mask = np.zeros(row_img.shape[:2], dtype=np.uint8)
    above = get_row(filename, srow - 1)
    if above:
        for letter_box in above['_letterBoxes']:
            y2 = math.ceil(letter_box['y2'] * ratio)
            if y2 > top:
                x1 = math.floor(letter_box['x1'] * ratio)
                x2 = math.ceil(letter_box['x2'] * ratio)
                y1 = math.floor(letter_box['y1'] * ratio) - top
                y2 = y2 - top

                cv2.rectangle(outlined, (x1, y1), (x2, y2), (0, 0, 255), 2)
                cv2.rectangle(mask, (x1, y1), (x2, y2), 255, -1)

    below = get_row(filename, erow + 1)
    if below:
        for letter_box in below['_letterBoxes']:
            y1 = math.floor(letter_box['y1'] * ratio)
            if y1 < bottom:
                x1 = math.floor(letter_box['x1'] * ratio)
                x2 = math.ceil(letter_box['x2'] * ratio)
                y1, y2 = y1 - top,  math.ceil(letter_box['y2'] * ratio) - top
                cv2.rectangle(outlined, (x1, y1), (x2, y2), (0, 0, 255), 2)
                cv2.rectangle(mask, (x1, y1), (x2, y2), 255, -1)

    # Create a 4-channel image with alpha channel
    cleared = cv2.cvtColor(row_img, cv2.COLOR_BGR2BGRA)
    # Set the alpha channel to 0 where the mask is white
    cleared[mask == 255] = [0, 0, 0, 0]
    sample['outlined'] = outlined
    sample['image'] = cleared

    return sample


def output_files(sample):
    file_base_name = FILE_BASE_NAME.format(
        sample['filename'],sample['res'],sample['srow'],sample['erow'])

    with open(f'{output_directory}/{file_base_name}.gt.txt', 'w') as output_file:
        output_file.writelines([sample['text']])

    cv2.imwrite(f'{output_directory}/{file_base_name}.tif', sample['image'])

    with open(f'{output_directory}/{file_base_name}.box', 'w') as box_file:
        for box in sample['boxes']:
            box_file.write(
                f'{box["value"]} {box["x1"]} {box["y1"]} {box["x2"]} {box["y2"]} 0\n')


def process_and_output(sample):
    if process(sample):
        output_files(sample)


def display(sample):
    print(sample['text'])
    print(sample['boxes'])
    output_base = FILE_BASE_NAME.format(
        sample['filename'],sample['res'],sample['srow'],sample['erow'])

    cv2.imshow(f'{output_base} cleared', sample["image"])
    cv2.imshow(f'{output_base} outlined', sample["outlined"])
    cv2.imshow(output_base, sample["img"])
    cv2.waitKey(0)


if __name__ == '__main__':
    cache_letter_boxes()
    # for r in range(1, 8):
        # output_row('isaiah', 44, r)
    display(process({
        'scroll': 'isaiah', 'fragment': 44, 'srow': 25, 'erow': 25, 'res': 10}))
    display(process({
        'scroll': 'isaiah', 'fragment': 44, 'srow': 25, 'erow': 27, 'res': 9}))
    display(process({
        'scroll': 'isaiah', 'fragment': 44, 'srow': 1, 'erow': 7, 'res': 8}))

    # Git clone the training programs if they don't exist.
    if not os.path.exists(BASE_OUTPUT):
        subprocess.run(
            ['git', 'clone', 'https://github.com/tesseract-ocr/tesstrain'])
        subprocess.run(
            ['git', 'clone', 'https://github.com/tesseract-ocr/tessdata_best'])

    # Delete and recreate the training data directories.
    if os.path.exists(output_directory):
        shutil.rmtree(output_directory)
    if os.path.exists(BASE_OUTPUT + 'fragment'):
        shutil.rmtree(BASE_OUTPUT + 'fragment')
    if os.path.exists(BASE_OUTPUT + 'fragment' + '.traineddata'):
        Path(BASE_OUTPUT + 'fragment' + '.traineddata').unlink()

    Path(output_directory).mkdir(parents=True, exist_ok=True)

    for frag in [4, 9, 14, 20, 27, 36, 44, 45, 47, 48, 53]:
        for r in range(1, 33):
            process_and_output(
                {'scroll': 'isaiah', 'fragment': frag, 'srow': r, 'erow': r})
            if r % 3 == 1:
                process_and_output({'scroll': 'isaiah', 'fragment': frag, 'srow': r, 'erow': r+2})
            if r % 7 == 1:
                process_and_output({'scroll': 'isaiah', 'fragment': frag, 'srow': r, 'erow': r+6})