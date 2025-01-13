import json
import math
import os
import requests
from multiprocessing import Pool
from pathlib import Path

BASE_DIR = 'images/{}'
FILE_NAME = '{}_{}_{}.jpg'
DSS_JSON_URL = 'http://dss.collections.imj.org.il/viewer/data/{}.json'
MB_TILE_URL = 'https://imgprd21.museumofthebible.org/collections/Tiled/35a9f923-d461-46f2-a946-98380a355606/TileGroup{}/{}-{}-{}.jpg'


def get_json(scroll):
    base_dir = BASE_DIR.format(scroll)
    Path(base_dir).mkdir(parents=True, exist_ok=True)
    file_path = f'{base_dir}/{scroll}.json'
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


# Checks to see if the file exists and downloads it if not.
# Returns the path to the file,
def download(collection, res, col, row, group=0):
    scroll = collection.scroll
    tiles_dir = f'{BASE_DIR.format(scroll)}/{'tiles' if res < 10 else 'large_tiles'}'
    Path(tiles_dir).mkdir(parents=True, exist_ok=True)
    file_name = FILE_NAME.format(res, col, row)
    file_path = f'{tiles_dir}/{file_name}'
    if os.path.isfile(file_path) and os.path.getsize(file_path) > 1024:
        # print('File {} exists, exiting'.format(file_path))
        return file_path

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
        if group < 5:
            download(collection, res, col, row, group + 1)

    return file_path


class Collection:
    def __init__(self, scroll=None, tile_size=None, width=None, height=None,
      full_width=None, full_height=None, max_zoom=None, columns=None, tile_prefix=None, json=None):
        if json is not None:
            self.scroll = json['scroll']
            self.tile_size = json['tileSize']
            self.width = int(json['width'])
            self.height = int(json['height'])
            self.full_width = int(json['fullSize']['w'])
            self.full_height = int(json['fullSize']['h'])
            self.max_zoom = int(json['maxZoom'])
            self.tile_prefix = json['tilePrefix']

            self.columns = [None] * int(json['columns'][0]['id'])
            for c in range(len(json['columns'])):
                column = json['columns'][c]
                self.columns[int(column['id']) - 1] = {
                    'x': int(column['x']),
                    'width': int(column['width'])}
        else:
            self.scroll = scroll
            self.tile_size = tile_size
            self.width = width
            self.height = height
            self.full_width = full_width
            self.full_height = full_height
            self.max_zoom = max_zoom
            self.columns = columns
            self.tile_prefix = tile_prefix

    def calc_at_res(self, res=None):
        if res is None:
            res = self.max_zoom - 1
        ratio = self.full_width / self.width
        for x in range(res, self.max_zoom):
            ratio /= 2

        cols = math.ceil(self.width * ratio / self.tile_size)
        rows = math.ceil(self.height * ratio / self.tile_size)

        return ratio, cols, rows

    def to_string(self):
        return (f'scroll: {self.scroll}, tile_size: {self.tile_size}, width: '
                f'{self.width}, height: {self.height}, maxZoom: '
                f'{self.max_zoom}, tile_prefix: {self.tile_prefix}, columns: '
                f'{self.columns}')


def download_tile(tile):
    download(tile['collection'], tile['res'], tile['col'], tile['row'])


def download_collection(collection, res=None):
    tiles = []
    if res is None:
        res = collection.max_zoom - 1
    ratio, cols, rows = collection.calc_at_res(res)

    for col in range(cols):
        for row in range(rows):
            tiles.append(
                {'collection': collection, 'res': res, 'col': col, 'row': row})

    with Pool() as pool:
        pool.map(download_tile, tiles)


Collection.ISAIAH = Collection(json=get_json('isaiah'))
Collection.WAR = Collection(json=get_json('war'))
Collection.COMMUNITY_RULE = Collection(json=get_json('community'))
Collection.TEMPLE_SCROLL = Collection(json=get_json('temple'))
Collection.HABAKKUK = Collection(json=get_json('habakkuk'))
Collection.TORAH = (
    Collection('torah', 256, 768, 574, 6024, 4588, 5, [
        {'x': 570, 'width': 170}, {'x': 435, 'width': 145},
        {'x': 295, 'width': 140}, {'x': 145, 'width': 155},
        {'x': 0, 'width': 145}]))

if __name__ == '__main__':
    print(Collection.ISAIAH.to_string())
    print(Collection.WAR.to_string())
    print(Collection.COMMUNITY_RULE.to_string())
    print(Collection.HABAKKUK.to_string())
    print(Collection.TEMPLE_SCROLL.to_string())
    print(Collection.TORAH.to_string())

    # Great Isaiah Scroll
    download_collection(Collection.ISAIAH, 8)
    download_collection(Collection.ISAIAH, 9)
    download_collection(Collection.ISAIAH, 10)

    # War Scroll
    download_collection(Collection.WAR)

    # Community Rule
    download_collection(Collection.COMMUNITY_RULE)

    # Temple Scroll
    download_collection(Collection.TEMPLE_SCROLL, res=9)

    # Commentary on Habakkuk
    download_collection(Collection.HABAKKUK)

    # Torah Scroll
    download_collection(Collection.TORAH)
