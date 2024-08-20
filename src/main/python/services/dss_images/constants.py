import numpy as np


class Collection:
    def __init__(self, scroll, res, cols, rows, tile_size, column_offsets=None):
        self.scroll = scroll
        self.res = res
        self.cols = cols
        self.rows = rows
        self.tile_size = tile_size
        self.column_offsets = column_offsets


Collection.ISAIAH = Collection('isaiah', 9, 255, 9, 227, np.array([
  15517, 15195, 14954, 14719, 14445, 14184, 13920, 13662, 13380, 13130,
  12791, 12444, 12158, 11905, 11662, 11382, 11125, 10862, 10623, 10370, 10107,
  9879, 9631, 9299, 9015, 8736, 8478, 8212, 7876, 7605, 7289, 6985, 6718, 6429,
  6105, 5831, 5512, 5148, 4835, 4517, 4181, 3830, 3478, 3189, 2925, 2669, 2390,
  2086, 1756, 1391, 1094, 793, 585, 310, 20]) * 3.7445)
Collection.WAR = Collection('war', 8, 128, 9, 236, np.array([
  8077, 7255, 6684, 6167, 5620, 5097, 4580, 4021, 3636, 3179, 2686, 2163, 1634,
  1099, 510, 0]) * 3.7445)
Collection.COMMUNITY_RULE = Collection('community', 8, 125, 11, 250, np.array([
  4276, 3948, 3657, 3323, 2858, 2413, 1983, 1573, 1212, 841, 417, 0]) * 7.45)
Collection.TEMPLE_SCROLL = Collection('temple', 9, 256, 6, 190, [52])
Collection.HABAKKUK = Collection('habakuk', 7, 62, 6, 290)
Collection.TORAH = Collection('torah', 5, 24, 17, 256, [
  5500, 4400, 3300, 2200, 1100, 0])
