import cv2
import matplotlib.pyplot as plt
import pandas as pd
from PIL import Image as PilImage
from letterbox_utils import DSSLettersDataset, SINGLE_LETTERS_ONLY, ALL, parse_file_name, ISAIAH_SET

VISUALIZE_PAGE_SIZE = 24
pd.set_option("display.max_columns", None)
pd.set_option("display.width", None)


def visualize_abnormals(title, abnormal):
  # See what the augmentation actually does to your images
  fig, axes = plt.subplots(int(VISUALIZE_PAGE_SIZE / 4), 4, figsize=(12, 6))
  axes = axes.flatten()

  for i, box in enumerate(abnormal.itertuples()):
    img = box.image
    axes[i].imshow(PilImage.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))) # CHW -> HWC
    _, _, frag = parse_file_name(box.filename)
    axes[i].set_title(f'{frag} {box.x1} {box.y1} {box.width} {box.height}')

  for i in range(VISUALIZE_PAGE_SIZE):
    axes[i].axis('off')

  plt.suptitle(title)
  plt.tight_layout()
  plt.show()


def find_abnormals(letter, df, prop, filename):
  mean = df[prop].mean()
  std = df[prop].std()
  min, max = round(mean - 3 * std), round(mean + 3 * std)

  # print(df.describe())
  if filename is not None:
    df = df[df['filename'] == filename]
  abnormal = df[(df[prop] < min) | (df[prop] > max)]

  if not abnormal.empty:
    title = (f'{len(abnormal)} abnormal {prop} for {letter}: mean: {mean:.1f}, '
             f'std: {std:.1f}, normal range: {min}-{max}')
    print(title)
    for start in range(0, len(abnormal), VISUALIZE_PAGE_SIZE):
      page = abnormal.iloc[start:start + VISUALIZE_PAGE_SIZE]
      visualize_abnormals(f"{title} ({start + 1}–{start + len(page)})", page)
  else:
    print(f"No abnormal for {letter}")


if __name__ == '__main__':
  # Filter to letters, exclude rows and words.
  dataset = DSSLettersDataset(fragments=ALL, filter=SINGLE_LETTERS_ONLY)
  print(f'Dataset {len(dataset)} letters')
  letters = []
  for img, label, metadata in dataset:
    metadata['image'] = img
    letters.append(metadata)

  df = pd.DataFrame(letters)
  df["width"] = df["x2"] - df["x1"]
  df["height"] = df["y2"] - df["y1"]

  print("Min Y: ", df["y1"].min(), "Max Y: ", df["y2"].max())

  counts = df['value'].value_counts()
  print(counts)
  mean = counts.mean()
  std = counts.std()
  print(f"Mean: {mean:.2f}, Median: {counts.median()},",
        f"Mode: {counts.round(-2).mode().tolist()}, Std: {std:.2f},",
        f"90% interval: {mean - 1.645 * std:.2f} to {mean + 1.645 * std:.2f}")

  filename = 'war-column-1'
  find_abnormals("all", df, 'width', filename)
  # find_abnormals("all", df, 'height')

  lessthan_7 = df[df['width'] < 7]
  if not lessthan_7.empty:
    visualize_abnormals("Width < 7", lessthan_7)

  lessthan_7 = df[df['height'] < 7]
  if not lessthan_7.empty:
    visualize_abnormals("Height < 7", lessthan_7)

  groups = df.groupby("value")
  for letter, group in groups:
    find_abnormals(letter, group, 'width', filename)
    find_abnormals(letter, group, 'height', filename)