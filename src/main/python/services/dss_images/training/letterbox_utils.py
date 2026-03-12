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
  def __init__(self, filter=None, transform=None, override_letter_cache:bool=False):
    self.transform = transform or (lambda x:x)
    cache_letter_boxes(columns, override_letter_cache)
    self.letters:list[dict] = []
    self.labels = []
    with open(letter_box_file, "r", encoding="utf-8") as f:
      for line in f:
        letter_box = json.loads(line)
        if filter is None or filter(letter_box):
          self.letters.append(letter_box)
          value = letter_box['value']
          self.labels.append(ord(value) - ord('א') + 1 if len(value) == 1 else 0)
      self.classes = torch.unique(torch.tensor(self.labels))
      print('Unique letter count:', len(self.classes))

  def __len__(self):
    return len(self.letters)

  def __getitem__(self, idx):
    return self.transform(self.letters[idx]), self.labels[idx]


class WithImage:
  file_img_cache = {}
  def __init__(self, res=9):
    self.res = res

  def __call__(self, letter):
    if letter.get('image') is None:
      file_img = self.file_img_cache.get(letter['filename'])
      if file_img is None:
        file_img = cv2.imread(f'../images/isaiah/columns/column_{self.res}_{letter['filename'][14:]}.jpg')
        self.file_img_cache[letter['filename']] = file_img
      scale = {10: 2, 9: 1, 8: 0.5}.get(self.res)
      y1, y2 = int(letter['y1'] * scale), int(letter['y2'] * scale)
      x1, x2 = int(letter['x1'] * scale), int(letter['x2'] * scale)
      img = file_img[y1:y2, x1:x2]
      letter['image'] = img
    return letter


class ToImage:
  def __init__(self, res=9):
    self.with_image = WithImage(res)

  def __call__(self, letter):
    return self.with_image(letter)['image']


class ToPilImage:
  def __init__(self, res=9):
    self.with_image = WithImage(res)

  def __call__(self, value):
    if isinstance(value, dict):
      if value.get('pilImage') is None:
        img = value['image'] if value.get('image') is not None else self.with_image(value)['image']
        value['pilImage'] = PilImage.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
      return value['pilImage']
    else:
      # We can not cache the pillow image if is produced from an image, bcz
      # that image could be different each time.
      img = value
    return PilImage.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))


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
  dataset = DSSLettersDataset(SINGLE_LETTERS_ONLY, ToImage(), override_letter_cache=False)
  print(f'Dataset {len(dataset)} letters')
  for i in range(3):
    image, label = dataset[i]
    cv2.imshow(str(label), image)
    cv2.waitKey(3000)

  imageDataset = DSSLettersDataset(filter=None, transform=ToImage())
  print(f'Dataset {len(imageDataset)} letters')
  for i in range(3):
    image, label = imageDataset[i]
    cv2.imshow(str(label), image)
    cv2.waitKey(3000)
