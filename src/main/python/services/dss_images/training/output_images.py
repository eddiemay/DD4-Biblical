import cv2
import json
from train_by_embedding import cache_letter_boxes, columns, letter_box_file

override_letter_cache = False
base_image = '../images/isaiah/columns/column_9_{}.jpg'


if __name__ == '__main__':
  cache_letter_boxes(columns, override_letter_cache)
  img_cache = {}

  with open(letter_box_file, "r", encoding="utf-8") as f:
    letters = 0
    for line in f:
      letter_box = json.loads(line)
      if letter_box['type'] == 'Letter':
        letters += 1
        img = img_cache.get(letter_box['filename'])
        if img is None:
          img = cv2.imread(base_image.format(letter_box['filename'][14:]))
          img_cache[letter_box['filename']] = img

        output = img[letter_box['y1']:letter_box['y2'], letter_box['x1']:letter_box['x2']]
        filename = f'{letter_box["id"]}.jpg'
        # cv2.imshow("Preview", output)
        # cv2.waitKey(0)
        cv2.imwrite(f'letters/{filename}', output)


    print(f'Found {letters} letters')
