import cv2
import json
import os
import numpy as np
import torch
import torchvision.transforms as transforms
from PIL import Image as PilImage
from typing import TypedDict
from torch.utils.data import Dataset
from urllib import request

letter_box_file = 'letter_boxes.jsonl'
API_BASE = 'https://dd4-biblical.appspot.com/_api/'
LETTERBOX_BY_FRAGMENT_URL = API_BASE + 'letterBoxs/v1/list?filter=filename={}&pageSize=0&orderBy=y1'
TRAINING_SET = list(map(lambda c: f'isaiah-column-{c}', [2, 4, 7, 9, 13, 14, 16, 17, 18, 20, 26, 27, 36, 40, 44, 45, 47, 48, 53]))
ALL = TRAINING_SET.copy().append('4QCalendrical-4Q420-Frag1')
SINGLE_LETTERS_ONLY =\
  lambda letter_box: letter_box['type'] == 'Letter' and len(letter_box['value']) == 1
mean, std = (0.5,), (0.5,)


class ToPilImage:
  def __call__(self, img:np.ndarray) -> PilImage:
    return PilImage.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))


class PadToSize:
  def __init__(self, target_w, target_h, fill=0):
    self.target_w = target_w
    self.target_h = target_h
    self.fill = fill

  def __call__(self, img):
    w, h = img.size
    pad_w = self.target_w - w
    pad_h = self.target_h - h
    padding = (pad_w // 2, pad_h // 2, pad_w - pad_w // 2, pad_h - pad_h // 2)
    return transforms.functional.pad(img, padding, fill=self.fill)


test_transform = transforms.Compose([
  ToPilImage(),
  PadToSize(40, 80, 0),
  transforms.CenterCrop([40, 80]),
  transforms.GaussianBlur(3, sigma=(0.1, 1.5)),
  transforms.Grayscale(),
  transforms.ToTensor(),
  transforms.Normalize(mean, std)
])


class LetterBox(TypedDict):
  id: int
  filename: str
  type: str
  x1: int
  y1: int
  x2: int
  y2: int
  value: str
  creationTime: int
  creationUsername: str
  lastModifiedTime: int
  lastModifiedUsername: str


class DSSLettersDataset(Dataset):
  def __init__(self, fragments:list[str]=None,
      filter:callable(LetterBox)=None, transform:callable(any)=None,
      cache_file:str=letter_box_file, override_cache:bool=False, res:int=9):
    self.transform = transform or (lambda x:x)
    self.res = res
    self.metadata:list[LetterBox] = []
    self.labels:list[int] = []
    classes = set()
    cache_letter_boxes(fragments or TRAINING_SET, cache_file, override_cache)
    with open(cache_file, "r", encoding="utf-8") as f:
      for line in f:
        letter_box: LetterBox = json.loads(line)
        if filter is None or filter(letter_box):
          self.metadata.append(letter_box)
          value = letter_box['value']
          classes.add(value)
          self.labels.append(ord(value) - ord('א') if len(value) == 1 and 'א' <= value <= 'ת' else 27)
      self.classes = [chr(c) for c in range(ord('א'), ord('ת') + 1)] + ['?']
      print(f'Classes: {self.classes} {len(self.classes)}')
      print(f'Labels: {torch.unique(torch.tensor(self.labels))} {type(self.labels[0])}')
      self.images:list = [None] * len(self.metadata)

  def __len__(self) -> int:
    return len(self.metadata)

  def __getitem__(self, idx:int) -> (any,int,dict):
    img = self.images[idx]
    if img is None:
      img = get_image(self.metadata[idx], self.res)
      self.images[idx] = img
    return self.transform(img), self.labels[idx], self.metadata[idx]


def parse_file_name(file_name):
  # Split the filename into scroll and the rest at the first hyphen
  parts = file_name.split('-', 1)
  if len(parts) != 2:
    return None  # Invalid format

  scroll, rest = parts[0], parts[1]

  is_column = rest.startswith('column-')
  if is_column:
    fragment_or_colnum = rest.split('-')[-1]
  else:
    # For fragments, take the entire rest as the fragment
    fragment_or_colnum = rest

  return scroll, is_column, fragment_or_colnum


def get_img_file_path(file_name, res):
  scroll, is_column, fragment = parse_file_name(file_name)
  return f"../images/{scroll}/columns/column_{res}_{fragment}.jpg" if is_column else f"../images/{scroll}/columns/{fragment}.jpg"


file_img_cache:dict[str, np.ndarray] = {}
def get_image(letter_box:dict, res:int=9) -> np.ndarray:
  """
  Retrieve a cropped letter image from a larger column image, using caching
  to avoid repeatedly loading the same file from disk.

  Parameters:
      letter_box (dict): A dictionary containing bounding box metadata with keys:
          - 'filename' (str): Identifier for the source column image
          - 'x1', 'y1', 'x2', 'y2' (int/float): Bounding box coordinates
      res (int, optional): Resolution level of the source image.
          Supported values:
              10 → scale factor 2
               9 → scale factor 1
               8 → scale factor 0.5
          Defaults to 9.

  Returns:
      np.ndarray: Cropped image corresponding to the bounding box.

  Behavior:
      - Loads the full column image from disk only once per filename
      - Stores it in a global cache (`file_img_cache`)
      - Applies scaling to bounding box coordinates based on resolution
      - Returns the cropped region from the cached image
  """

  file_img = file_img_cache.get(letter_box['filename'])
  if file_img is None:
    file_path = get_img_file_path(letter_box['filename'], res)
    file_img = cv2.imread(file_path)
    file_img_cache[letter_box['filename']] = file_img
  scale = {10: 2, 9: 1, 8: 0.5}.get(res, 1)
  y1, y2 = int(letter_box['y1'] * scale), int(letter_box['y2'] * scale)
  x1, x2 = int(letter_box['x1'] * scale), int(letter_box['x2'] * scale)
  return file_img[y1:y2, x1:x2]


def cache_letter_boxes(fragments:list[str], cache_file, override_cache:bool=False):
  if os.path.exists(cache_file) and not override_cache:
    return

  # Open the file for write.
  print('Writing file: ', cache_file)
  with open(cache_file, "w", encoding="utf-8") as f:
    for fragment in fragments:
      letterbox_url = LETTERBOX_BY_FRAGMENT_URL.format(fragment)
      print('Sending request: ', letterbox_url)
      with request.urlopen(letterbox_url) as url:
        response = json.load(url)
        print('Response: ', response)
        letterboxes = response['items']
        # Dump each scripture verse into the file.
        for letterbox in letterboxes:
          json.dump(letterbox, f)
          f.write("\n")


if __name__ == '__main__':
  # Filter to letters, exclude rows and words.
  dataset = DSSLettersDataset(filter=SINGLE_LETTERS_ONLY, override_cache=False)
  print(f'Dataset {len(dataset)} letters')
  for i in range(3):
    image, label, metadata = dataset[i]
    cv2.imshow(f"{metadata['value']} {metadata['filename']} ({metadata['x1']},{metadata['y1']})", image)
    cv2.waitKey(2000)

  imageDataset = DSSLettersDataset()
  print(f'Dataset {len(imageDataset)} letters')
  for i in range(3):
    image, label, metadata = imageDataset[i]
    cv2.imshow(f"{metadata['value']} {metadata['filename']} ({metadata['x1']},{metadata['y1']})", image)
    cv2.waitKey(2000)

  image = get_image({"filename": "isaiah-column-2", "type": "Letter", "x1": 76, "y1": 326, "x2": 94, "y2": 348, "value": "\u05e9"})
  cv2.imshow('Direct load', image)
  cv2.waitKey(2000)

  image = get_image({"filename": "4QCalendrical-4Q320-Frag1", "type": "Letter", "x1": 76, "y1": 326, "x2": 94, "y2": 348, "value": "\u05e9"})
  cv2.imshow('Direct load, 4Q320', image)
  cv2.waitKey(3000)
