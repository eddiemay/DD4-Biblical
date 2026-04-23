import cv2
from letterbox_utils import DSSLettersDataset, SINGLE_LETTERS_ONLY

if __name__ == '__main__':
  dataset = DSSLettersDataset(filter=SINGLE_LETTERS_ONLY, override_cache=False)
  print(f'Dataset {len(dataset)} letters')
  for img, label, metadata in dataset:
    cv2.imwrite(f'letters/{metadata['id']}.jpg', img)
