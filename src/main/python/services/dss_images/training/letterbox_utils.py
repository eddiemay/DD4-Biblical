import cv2
import json
import matplotlib.pyplot as plt
import os
import pandas as pd
import torch
from PIL import Image
from torch.utils.data import Dataset
from urllib import request

letter_box_file = 'letter_boxes.jsonl'
API_BASE = 'https://dd4-biblical.appspot.com/_api/'
LETTERBOX_BY_FRAGMENT_URL = API_BASE + 'letterBoxs/v1/list?filter=filename={}&pageSize=0&orderBy=y1'
columns = [2, 4, 7, 9, 13, 14, 16, 17, 18, 20, 26, 27, 36, 40, 44, 45, 47, 48, 53]
VISUALIZE_PAGE_SIZE = 32
pd.set_option("display.max_columns", None)
pd.set_option("display.width", None)
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
        value['pilImage'] = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
      return value['pilImage']
    else:
      # We can not cache the pillow image if is produced from an image, bcz
      # that image could be different each time.
      img = value
    return Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))


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


def find_abnormals(letter, df, prop):
  mean = df[prop].mean()
  std = df[prop].std()

  # print(df.describe())
  abnormal = df[abs(df[prop] - mean) > 3 * std]

  if not abnormal.empty:
    title = (f'{len(abnormal)} abnormal {prop} for {letter}: mean: {mean:.1f}, '
             f'std: {std:.1f}, normal range: {mean - 3 * std:.2f}-{mean + 3 * std:.2f}')
    print(title)
    for box in abnormal.itertuples():
      print(f"\t{box.filename} width={box.width} height={box.height} x1={box.x1} y1={box.y1}")
    for start in range(0, len(abnormal), VISUALIZE_PAGE_SIZE):
      page = abnormal.iloc[start:start + VISUALIZE_PAGE_SIZE]
      visualize_abnormals(f"{title} ({start + 1}–{start + len(page)})", page)
  else:
    print(f"No abnormal for {letter}")


def visualize_abnormals(title, abnormal):
  # See what the augmentation actually does to your images
  fig, axes = plt.subplots(8, 4, figsize=(12, 6))
  axes = axes.flatten()

  for i, box in enumerate(abnormal.itertuples()):
    img = box.image
    axes[i].imshow(Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))) # CHW -> HWC
    axes[i].set_title(f'{box.filename} {box.x1} {box.y1} {box.width} {box.height}')

  for i in range(VISUALIZE_PAGE_SIZE):
    axes[i].axis('off')

  plt.suptitle(title)
  plt.tight_layout()
  plt.show()


if __name__ == '__main__':
  # Filter to letters, exclude rows and words.
  dataset = DSSLettersDataset(SINGLE_LETTERS_ONLY, WithImage(), override_letter_cache=False)
  print(f'Dataset {len(dataset)} letters')
  letters = []
  for letter_box, label in dataset:
    letters.append(letter_box)

  df = pd.DataFrame(letters)
  df["width"] = df["x2"] - df["x1"]
  df["height"] = df["y2"] - df["y1"]

  print(df['value'].value_counts())

  find_abnormals("all", df, 'width')
  # find_abnormals("all", df, 'height')

  lessthan_7 = df[df['width'] < 7]
  if not lessthan_7.empty:
    visualize_abnormals("Width < 7", lessthan_7)

  lessthan_7 = df[df['height'] < 7]
  if not lessthan_7.empty:
    visualize_abnormals("Height < 7", lessthan_7)

  groups = df.groupby("value")
  for letter, group in groups:
    find_abnormals(letter, group, 'width')
    find_abnormals(letter, group, 'height')

  imageDataset = DSSLettersDataset(filter=None, transform=ToImage())
  print(f'Dataset {len(imageDataset)} letters')
  for i in range(3):
    image, label = imageDataset[i]
    cv2.imshow(str(label), image)
    cv2.waitKey(3000)
