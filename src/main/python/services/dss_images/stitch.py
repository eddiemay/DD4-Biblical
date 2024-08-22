import cv2
import math
import numpy
from dss_backup import Collection, download, get_json
from dss_ocr import image_to_string

BASE_PATH = r'images/{}/{}_{}_{}.jpg'


def load_img(collection, col, row):
    download(collection, col, row)
    file_path = BASE_PATH.format(collection.scroll, collection.res, col, row)
    return cv2.imread(file_path)


def construct_column(collection, column=1):
    column_offsets = collection.column_offsets
    print('bounds: {}'.format(
      [column_offsets[column], column_offsets[column - 1]]))
    rows = collection.rows
    tile_size = collection.tile_size
    col_offset = math.floor(column_offsets[column] / tile_size)
    cols = math.ceil(column_offsets[column - 1] / tile_size) - col_offset
    print('Creating canvas of size: (w: {}, h: {})'.format(
        cols * tile_size, rows * tile_size))
    canvas = numpy.zeros((rows * tile_size, cols * tile_size, 3), dtype='uint8')
    for c in range(cols):
        for r in range(rows):
            img = load_img(collection, c + col_offset, r)
            x = c * tile_size
            y = r * tile_size
            canvas[y:y + img.shape[0], x:x + img.shape[1]] = img

    print('Showing image...')
    cv2.imshow('Canvas', canvas)
    print(image_to_string(canvas, False))
    # cv2.imwrite('images/{}/column{}.jpg'.format(collection.scroll, column), canvas)
    cv2.waitKey(0)


construct_column(Collection.TORAH_5, 5)
construct_column(Collection.TORAH_4, 5)
construct_column(Collection.TORAH_5, 4)
construct_column(Collection.TORAH_4, 4)
construct_column(Collection.TORAH_5, 1)
construct_column(Collection.TORAH_4, 1)

construct_column(Collection.ISAIAH, 54)
construct_column(Collection.ISAIAH, 53)
construct_column(Collection.ISAIAH, 1)

construct_column(Collection.WAR, 15)
construct_column(Collection.WAR, 14)
construct_column(Collection.WAR, 1)

construct_column(Collection.COMMUNITY_RULE, 11)
construct_column(Collection.COMMUNITY_RULE, 10)
construct_column(Collection.COMMUNITY_RULE, 1)

construct_column(Collection.TEMPLE_SCROLL, 67)
construct_column(Collection.TEMPLE_SCROLL, 66)
construct_column(Collection.TEMPLE_SCROLL, 2)
construct_column(Collection.TEMPLE_SCROLL, 1)

construct_column(Collection.HABAKKUK, 14)
construct_column(Collection.HABAKKUK, 13)
construct_column(Collection.HABAKKUK, 1)
