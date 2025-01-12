import cv2
import math
import numpy
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
    cv2.waitKey(2)


if __name__ == '__main__':
    # for c in range(1, 5):
        # construct_column(Collection.TORAH, c)

    # construct_column(Collection.TORAH, 5, res=5)
    # construct_column(Collection.TORAH, 4, res=5)
    # construct_column(Collection.TORAH, 3, res=5)
    # construct_column(Collection.TORAH, 2, res=5)
    # construct_column(Collection.TORAH, 1, res=5)

    # for c in range(1):
        # construct_column(Collection.ISAIAH, 47, 8)
        # construct_column(Collection.ISAIAH, 47, 9)
        # construct_column(Collection.ISAIAH, 47, 10)
        # print(f'Showing Isaiah column {c + 1}')
        # cv2.waitKey(2)
    construct_column(Collection.ISAIAH, 52, 10)
    # construct_column(Collection.ISAIAH, 45, 10)
    # construct_column(Collection.ISAIAH, 45, 8)
    # construct_column(Collection.ISAIAH, 1)
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
