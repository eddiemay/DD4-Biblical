import cv2
import json
import os
import torch
from PIL import Image as PilImage
from torch.utils.data import Dataset
from urllib import request

letter_box_file = 'letter_boxes.jsonl'
API_BASE = 'https://dd4-biblical.appspot.com/_api/'
LETTERBOX_BY_FRAGMENT_URL = API_BASE + 'letterBoxs/v1/list?filter=filename={}&pageSize=0&orderBy=y1'
columns = [2, 4, 7, 9, 13, 14, 16, 17, 18, 20, 26, 27, 36, 40, 44, 45, 47, 48, 53]
SINGLE_LETTERS_ONLY =\
  lambda letter_box: letter_box['type'] == 'Letter' and len(letter_box['value']) == 1


class DSSLettersDataset(Dataset):
  def __init__(self, filter=None, transform=None, res=9, override_letter_cache:bool=False):
    self.transform = transform or (lambda x:x)
    self.res = res
    cache_letter_boxes(columns, override_letter_cache)
    self.metadata:list[dict] = []
    self.labels = []
    classes = set()
    with open(letter_box_file, "r", encoding="utf-8") as f:
      for line in f:
        letter_box = json.loads(line)
        if filter is None or filter(letter_box):
          self.metadata.append(letter_box)
          value = letter_box['value']
          classes.add(value)
          self.labels.append(ord(value) - ord('א') if len(value) == 1 and value >= 'א' and value <= 'ת' else 27)
      self.classes = [chr(c) for c in range(ord('א'), ord('ת') + 1)] + ['?']
      print(f'Classes: {self.classes} {len(self.classes)}')
      print(f'Labels: {torch.unique(torch.tensor(self.labels))} {type(self.labels[0])}')
      self.images = [None] * len(self.metadata)

  def __len__(self):
    return len(self.metadata)

  def __getitem__(self, idx):
    img = self.images[idx]
    if img is None:
      img = get_image(self.metadata[idx], self.res)
      self.images[idx] = img
    return self.transform(img), self.labels[idx], self.metadata[idx]


class ToPilImage:
  def __call__(self, img):
    return PilImage.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))


file_img_cache = {}
def get_image(letter_box:dict, res:int=9):
  file_img = file_img_cache.get(letter_box['filename'])
  if file_img is None:
    file_img = cv2.imread(f'../images/isaiah/columns/column_{res}_{letter_box['filename'][14:]}.jpg')
    file_img_cache[letter_box['filename']] = file_img
  scale = {10: 2, 9: 1, 8: 0.5}.get(res)
  y1, y2 = int(letter_box['y1'] * scale), int(letter_box['y2'] * scale)
  x1, x2 = int(letter_box['x1'] * scale), int(letter_box['x2'] * scale)
  return file_img[y1:y2, x1:x2]


def cache_letter_boxes(columns, override_letter_cache=False):
  if os.path.exists(letter_box_file) and not override_letter_cache:
    return

  fragments = list(map(lambda c: f'isaiah-column-{c}', columns))
  # Open the file for write.
  print('Writing file: ', letter_box_file)
  with open(letter_box_file, "w", encoding="utf-8") as f:
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
  dataset = DSSLettersDataset(SINGLE_LETTERS_ONLY, override_letter_cache=False)
  print(f'Dataset {len(dataset)} letters')
  for i in range(3):
    image, label, metadata = dataset[i]
    cv2.imshow(str(label), image)
    cv2.waitKey(3000)

  imageDataset = DSSLettersDataset()
  print(f'Dataset {len(imageDataset)} letters')
  for i in range(3):
    image, label, metadata = imageDataset[i]
    cv2.imshow(str(label), image)
    cv2.waitKey(3000)
