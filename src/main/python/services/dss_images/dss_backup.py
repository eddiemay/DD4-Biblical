import json
import os
import numpy as np
import requests
from multiprocessing import Pool
from pathlib import Path

FILE_DIR = 'images/{}/'
FILE_NAME = '{}_{}_{}.jpg'
DSS_JSON_URL = 'http://dss.collections.imj.org.il/viewer/data/{}.json'
MB_TILE_URL = 'https://imgprd21.museumofthebible.org/collections/Tiled/35a9f923-d461-46f2-a946-98380a355606/TileGroup{}/{}-{}-{}.jpg'


def get_json(scroll):
    dir = FILE_DIR.format(scroll)
    Path(dir).mkdir(parents=True, exist_ok=True)
    file_path = dir + scroll + '.json'
    if os.path.isfile(file_path) and os.path.getsize(file_path) > 1024:
        with open(file_path, 'r') as f:
            ret = json.load(f)
            ret['scroll'] = scroll
            return ret

    response = requests.get(DSS_JSON_URL.format(scroll))
    with open(file_path, 'w') as output:
        output.write(response.text)
    ret = json.load(response.text)
    ret['scroll'] = scroll
    return ret


def download(collection, col, row, group=0):
    scroll = collection.scroll
    res = collection.res
    Path('images/' + scroll).mkdir(parents=True, exist_ok=True)
    file_name = FILE_NAME.format(res, col, row)
    file_path = FILE_DIR.format(scroll) + file_name
    if os.path.isfile(file_path) and os.path.getsize(file_path) > 1024:
        # print('File {} exists, exiting'.format(file_path))
        return

    if scroll == 'torah':
        # if row < 3 or row == 3 and col < 12:
          #  group = 0
        # elif row < 13 or row == 13 and col < 18:
          #  group = 1
        # else:
           # group = 2
        url = MB_TILE_URL.format(group, res, col, row)
    else:
        url = collection.tile_prefix + file_name
    print('Downloading {} to {}'.format(url, file_path))
    response = requests.get(url)
    resp_len = len(response.content)
    print(resp_len)
    if resp_len > 1024:
        with open(file_path, 'wb') as output:
            output.write(response.content)
    elif scroll == 'torah':
        download(collection, col, row, group + 1)


class Collection:
    def __init__(self, scroll=None, res=None, cols=None, rows=None,
      tile_size=None, column_offsets=None, tile_prefix=None, json=None):
        self.scroll = scroll
        self.res = res
        self.tile_prefix = tile_prefix

        if json is not None:
            if scroll is None:
                self.scroll = json['scroll']
            if tile_prefix is None:
                self.tile_prefix = json['tilePrefix']
            height = int(json['height'])
            width = int(json['width'])
            ratio = int(json['fullSize']['w']) / width
            max_zoom = int(json['maxZoom'])
            if res is None:
                self.res = res = max_zoom - 1
            for x in range(res, max_zoom):
                ratio /= 2

            self.tile_size = json['tileSize']
            self.rows = round(height * ratio / self.tile_size)
            self.cols = round(width * ratio / self.tile_size)
            columns = json['columns']
            cs = len(columns)
            self.column_offsets = [None] * (cs + 1)
            for c in range(cs):
                column = columns[c]
                self.column_offsets[int(column['id'])] = column['x'] * ratio
            self.column_offsets[0] = (
              self.column_offsets[1] + columns[cs - 1]['width'] * ratio)
            self.column_offsets = np.array(self.column_offsets)
        else:
            self.tile_size = tile_size
            self.rows = rows
            self.cols = cols
            self.column_offsets = column_offsets

    def to_string(self):
        return (
            'scroll: {}, res: {}, tile_size: {}, rows: {}, cols: {}, '
            'tile_prefix: {}, column_offsets: {}').format(
            self.scroll, self.res, self.tile_size, self.rows, self.cols,
            self.tile_prefix, self.column_offsets)


Collection.ISAIAH = Collection(json=get_json('isaiah'))
Collection.WAR = Collection(json=get_json('war'))
Collection.COMMUNITY_RULE = Collection(json=get_json('community'))
Collection.TEMPLE_SCROLL = Collection(res=9, json=get_json('temple'))
Collection.HABAKKUK = Collection(json=get_json('habakkuk'))
Collection.TORAH_5 =(
    Collection('torah', 5, 24, 17, 256, [5500, 4400, 3300, 2200, 1100, 0]))
Collection.TORAH_4 =(
    Collection('torah', 4, 14, 9, 256, [2750, 2200, 1650, 1100, 550, 0]))


class Tile:
    def __init__(self, collection, col, row):
        self.collection = collection
        self.col = col
        self.row = row


def download_tile(tile):
    download(tile.collection, tile.col, tile.row)


def download_collection(collection):
    tiles = []
    for col in range(collection.cols):
        for row in range(collection.rows):
            tiles.append(Tile(collection, col, row))

    with Pool() as pool:
        pool.map(download_tile, tiles)


if __name__ == '__main__':
    print(Collection.ISAIAH.to_string())
    print(Collection.WAR.to_string())
    print(Collection.COMMUNITY_RULE.to_string())
    print(Collection.HABAKKUK.to_string())
    print(Collection.TEMPLE_SCROLL.to_string())
    print(Collection.TORAH.to_string())

    # Great Isaiah Scroll
    download_collection(Collection.ISAIAH)

    # War Scroll
    download_collection(Collection.WAR)

    # Community Rule
    download_collection(Collection.COMMUNITY_RULE)

    # Temple Scroll
    download_collection(Collection.TEMPLE_SCROLL)

    # Commentary on Habakkuk
    download_collection(Collection.HABAKKUK)

    # Torah Scroll
    download_collection(Collection.TORAH)
