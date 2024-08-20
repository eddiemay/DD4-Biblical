import cv2
import math
import numpy
from constants import Collection
from dss_backup import download
from dss_ocr import image_to_string

BASE_PATH = r'images/{}/{}_{}_{}.jpg'


def load_img(scroll, res, col, row):
    download(scroll, res, col, row)
    file_path = BASE_PATH.format(scroll, res, col, row)
    return cv2.imread(file_path)


def construct_column(collection, column=1):
    column_offsets = collection.column_offsets
    print(
        'bounds: {}'.format(
            [column_offsets[column], column_offsets[column - 1]]))
    rows = collection.rows
    tile_size = collection.tile_size
    col_offset = math.floor(column_offsets[column] / tile_size)
    cols = math.ceil(column_offsets[column - 1] / tile_size) - col_offset
    print(
        'Creating canvas of size: {}'.format(
            [rows * tile_size, cols * tile_size]))
    canvas = numpy.zeros((rows * tile_size, cols * tile_size, 3), dtype='uint8')
    for c in range(cols):
        for r in range(rows):
            img = load_img(collection.scroll, collection.res, c + col_offset, r)
            x = c * tile_size
            y = r * tile_size
            canvas[y:y + img.shape[0], x:x + img.shape[1]] = img

    print('Showing image...')
    cv2.imshow('Canvas', canvas)
    print(image_to_string(canvas, False))
    cv2.imwrite(
        'images/{}/column{}.jpg'.format(collection.scroll, column), canvas)
    cv2.waitKey(0)


construct_column(Collection.WAR, 15)
construct_column(Collection.WAR, 14)
construct_column(Collection.WAR, 1)
# construct_column(Collection.TORAH, 1)
# for c in range(1, 55):
# construct_column(Collection.ISAIAH, 1)
