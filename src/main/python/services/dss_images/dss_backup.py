import os
import requests
from constants import Collection
from multiprocessing import Pool
from pathlib import Path

FILE_PATH = 'images/{}/{}_{}_{}.jpg'
DSS_URL = 'http://tiles.imj.org.il/tiles/{}/{}_{}_{}.jpg'
MB_URL = 'https://imgprd21.museumofthebible.org/collections/Tiled/35a9f923-d461-46f2-a946-98380a355606/TileGroup{}/{}-{}-{}.jpg'


def download(scroll, res, col, row):
    Path('images/' + scroll).mkdir(parents=True, exist_ok=True)
    file_path = FILE_PATH.format(scroll, res, col, row)
    if os.path.isfile(file_path) and os.path.getsize(file_path) > 1024:
        # print('File {} exists, exiting'.format(file_path))
        return
    if scroll == 'torah':
        if row < 3 or row == 3 and col < 12:
            group = 0
        elif row < 13 or row == 13 and col < 18:
            group = 1
        else:
            group = 2
        url = MB_URL.format(group, res, col, row)
    else:
        url = DSS_URL.format(scroll, res, col, row)
    print('Downloading {} to {}'.format(url, file_path))
    response = requests.get(url)
    resp_len = len(response.content)
    print(resp_len)
    if resp_len > 1024:
        with open(file_path, 'wb') as output:
            output.write(response.content)


class Tile:
    def __init__(self, scroll, res, col, row):
        self.scroll = scroll
        self.res = res
        self.col = col
        self.row = row


def download_tile(tile):
    download(tile.scroll, tile.res, tile.col, tile.row)


def download_collection(collection):
    tiles = []
    for col in range(collection.cols + 1):
        for row in range(collection.rows + 1):
            tiles.append(Tile(collection.scroll, collection.res, col, row))

    with Pool() as pool:
        pool.map(download_tile, tiles)


if __name__ == '__main__':
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
