# Install dependencies if you do not have them.
import os
''' import subprocess
subprocess.run(['pip', 'install', 'opencv-python', 'numpy', 'requests']) '''

import cv2
import math
import numpy
import re
from dss_backup import Collection, download, BASE_DIR
from pathlib import Path


def load_img(collection, res, col, row):
    file_path = download(collection, res, col, row)
    return cv2.imread(file_path)


def construct_column(collection, column=1, res=None):
    if res is None:
        res = collection.max_zoom - 1
    ratio, _, rows = collection.calc_at_res(res)
    start_x = math.floor(collection.columns[column - 1]['x'] * ratio)
    width = math.ceil(collection.columns[column - 1]['width'] * ratio)
    print('bounds: {}'.format([start_x, start_x + width]))
    tile_size = collection.tile_size
    col_offset = math.floor(start_x / tile_size)
    actual_start = start_x - col_offset * tile_size
    cols = math.ceil((actual_start + width) / tile_size)
    print('Creating canvas of size: (h: {}, w: {})'.format(
        rows * tile_size, cols * tile_size))
    canvas = numpy.zeros((rows * tile_size, cols * tile_size, 3), dtype='uint8')
    for c in range(cols):
        for r in range(rows):
            img = load_img(collection, res, c + col_offset, r)
            x = c * tile_size
            y = r * tile_size
            canvas[y:y + img.shape[0], x:x + img.shape[1]] = img

    cv2.imshow('Canvas', canvas)

    print('Cropping to: (h: {}, w: {})'.format(rows * tile_size, width))
    exact = canvas[0:rows * tile_size, actual_start:actual_start + width]
    print('Showing image {} column {}... of shape: {}'.format(collection.scroll, column, exact.shape[:2]))
    cv2.imshow(f'Canvas exact Res: {res}', exact)

    columns_dir = f'{BASE_DIR.format(collection.scroll)}/columns'
    Path(columns_dir).mkdir(exist_ok=True)
    cv2.imwrite(f'{columns_dir}/column_{res}_{column}.jpg', exact)
    cv2.waitKey(1)


DSS_ORG_PATTERN = r"x(\d+)-y(\d+)-z(\d+)"
def construct_dss_org(scroll, file_pattern, output_name):
    files = []
    min_x, max_x, min_y, max_y = 99, 0, 99, 0
    tile_dir = f'images/{scroll}/tiles/'
    for entry in sorted(os.listdir(tile_dir)):
        if file_pattern in entry:
            match = re.search(DSS_ORG_PATTERN, entry)
            print(f'Found: {entry}')
            x, y = int(match.group(1)), int(match.group(2))
            files.append({'x': x, 'y': y, 'path': os.path.join(tile_dir, entry)})
            if x < min_x:
                min_x = x
            if x > max_x:
                max_x = x
            if y < min_y:
                min_y = y
            if y > max_y:
                max_y = y

    print(f'x range: ({min_x}, {max_x}), y range: ({min_y}, {max_y})')

    canvas = numpy.zeros(((max_y + 1 - min_y) * 512, (max_x + 1 - min_x) * 512, 3), dtype='uint8')
    for file in files:
        img = cv2.imread(file['path'])
        y = (file['y'] - min_y) * 512
        x = (file['x'] - min_x) * 512
        print(f'{file['path']} y, x = {y},{x}')
        canvas[y:y + 512, x:x + 512] = img

    cv2.imshow(f'{output_name}', canvas)
    cv2.waitKey(0)
    cv2.imwrite(f'images/{scroll}/{output_name}.jpg', canvas)


if __name__ == '__main__':
    # construct_dss_org('4Q320', '0hBWsyGjCZT', 'Infrared-Frag1')
    # construct_dss_org('4Q320', 'wmhm0ryg', 'Infrared-Frag2')
    # construct_dss_org('4Q320', 'BtzcLAfA', 'Infrared-Frag3')

    # for c in range(1, 5):
        # construct_column(Collection.TORAH, c)

    # construct_column(Collection.TORAH, 5, res=5)
    # construct_column(Collection.TORAH, 4, res=5)
    # construct_column(Collection.TORAH, 3, res=5)
    # construct_column(Collection.TORAH, 2, res=5)
    # construct_column(Collection.TORAH, 1, res=5)

    for res in range(8, 11):
        construct_column(Collection.ISAIAH, 16, res)
    cv2.waitKey(0)

    # for c in range(1, 16):
      #  construct_column(Collection.WAR, c)
    # construct_column(Collection.WAR, 14)
    # construct_column(Collection.WAR, 1)

    # for c in range(1, 12):
        # construct_column(Collection.COMMUNITY_RULE, c)
    # construct_column(Collection.COMMUNITY_RULE, 10)
    # construct_column(Collection.COMMUNITY_RULE, 11)

    # for c in range(2, 68):
        # construct_column(Collection.TEMPLE_SCROLL, c, res=9)
    # construct_column(Collection.TEMPLE_SCROLL, 66, res=9)
    # construct_column(Collection.TEMPLE_SCROLL, 2, res=9)
    # construct_column(Collection.TEMPLE_SCROLL, 1)

    # for c in range(1, 15):
        # construct_column(Collection.HABAKKUK, c)
    # construct_column(Collection.HABAKKUK, 13)
    # construct_column(Collection.HABAKKUK, 1)
