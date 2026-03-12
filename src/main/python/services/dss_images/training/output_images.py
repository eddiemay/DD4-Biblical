import cv2
from letterbox_utils import DSSLettersDataset, WithImage, SINGLE_LETTERS_ONLY

if __name__ == '__main__':
  dataset = DSSLettersDataset(filter=SINGLE_LETTERS_ONLY, transform=WithImage(), override_letter_cache=False)
  print(f'Dataset {len(dataset)} letters')
  for letter_box, label in dataset:
    cv2.imwrite(f'letters/{letter_box['id']}.jpg', letter_box['image'])
