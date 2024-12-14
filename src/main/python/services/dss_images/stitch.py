import cv2
import math
import numpy
from dss_backup import Collection, download
from training.dss_ocr import image_to_string
from pathlib import Path


def load_img(collection, res, col, row):
    download(collection, res, col, row)
    file_path = f'images/{collection.scroll}/tiles/{res}_{col}_{row}.jpg'
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
    cols = math.ceil(width / tile_size)
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
    canvas = canvas[0:rows * tile_size, start_x - col_offset * tile_size:width]

    print('Showing image {} column {}...'.format(collection.scroll, column))
    cv2.imshow('Canvas exact', canvas)
    print(image_to_string(canvas, False))
    Path(f'images/{collection.scroll}/columns').mkdir(exist_ok=True)
    cv2.imwrite(
        f'images/{collection.scroll}/columns/column_{res}_{column}.jpg', canvas)
    cv2.waitKey()


# for c in range(1, 5):
    # construct_column(Collection.TORAH, c)

# construct_column(Collection.TORAH, 5, res=5)
# construct_column(Collection.TORAH, 4, res=5)
# construct_column(Collection.TORAH, 3, res=5)
# construct_column(Collection.TORAH, 2, res=5)
# construct_column(Collection.TORAH, 1, res=5)

# for c in range(1, 55):
    # construct_column(Collection.ISAIAH, c)
construct_column(Collection.ISAIAH, 14)
# construct_column(Collection.ISAIAH, 1)

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
