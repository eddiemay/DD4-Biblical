import cv2
import numpy as np
import onnxruntime as ort

mean, std = (0.5,), (0.5,)


def pad_to_size(img: np.ndarray, target_h: int, target_w: int, fill=0) -> np.ndarray:
  h, w = img.shape[:2]

  pad_w = max(0, target_w - w)
  pad_h = max(0, target_h - h)

  left = pad_w // 2
  right = pad_w - left
  top = pad_h // 2
  bottom = pad_h - top

  return cv2.copyMakeBorder(
      img, top, bottom, left, right,
      borderType=cv2.BORDER_CONSTANT,
      value=fill
  )


def center_crop(img: np.ndarray, target_h: int, target_w: int) -> np.ndarray:
  h, w = img.shape[:2]

  top = max(0, (h - target_h) // 2)
  left = max(0, (w - target_w) // 2)

  return img[top:top+target_h, left:left+target_w]


def gaussian_blur(img: np.ndarray, kernel_size=3, sigma_min=0.1, sigma_max=1.5) -> np.ndarray:
  sigma = np.random.uniform(sigma_min, sigma_max)
  return cv2.GaussianBlur(img, (kernel_size, kernel_size), sigma)


def to_grayscale(img: np.ndarray) -> np.ndarray:
  return cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)


def to_tensor(img: np.ndarray) -> np.ndarray:
  arr = img.astype(np.float32) / 255.0

  if arr.ndim == 2:  # grayscale
    arr = np.expand_dims(arr, axis=0)  # (1, H, W)
  else:
    arr = np.transpose(arr, (2, 0, 1))  # (C, H, W)

  return arr


def normalize(tensor: np.ndarray, mean, std) -> np.ndarray:
  mean = np.array(mean, dtype=np.float32).reshape(-1, 1, 1)
  std = np.array(std, dtype=np.float32).reshape(-1, 1, 1)
  return (tensor - mean) / std


def transform(img: np.ndarray, mean, std) -> np.ndarray:
  img = pad_to_size(img, 40, 80, 0)
  img = center_crop(img, 40, 80)
  img = gaussian_blur(img, 3, 0.1, 1.5)
  img = to_grayscale(img)
  tensor = to_tensor(img)
  tensor = normalize(tensor, mean, std)
  return tensor.astype(np.float32)


def predict_letters(items):
  predictable = []
  images = []
  for item in items:
    if item['type'] == 'Letter' and len(item['value']) == 1:
      predictable.append(item)
      images.append(transform(get_image(item), mean, std))

  if len(predictable) == 0:
    return

  ort_session = ort.InferenceSession("letter_model.onnx")
  input_name = ort_session.get_inputs()[0].name
  inputs = np.array(images)
  outputs = ort_session.run(None, {input_name: inputs})[0]
  preds = np.argmax(outputs, axis=1)

  label_lookup = [chr(c) for c in range(ord('א'), ord('ת') + 1)] + ['?']
  for i in range(len(predictable)):
    predictable[i]['_predicted'] = label_lookup[preds[i]]


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
  return f"./images/{scroll}/columns/column_{res}_{fragment}.jpg" if is_column else f"./images/{scroll}/columns/{fragment}.jpg"


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


if __name__ == '__main__':
  import json
  items = []
  with open('training/4Q320-Frag1.jsonl', "r", encoding="utf-8") as f:
    for line in f:
      items.append(json.loads(line))

  predict_letters(items)
  missmatch = 0
  for letter_box in items:
    if letter_box.get('_predicted') is not None and letter_box['_predicted'] != letter_box['value']:
      missmatch += 1
      print(letter_box)

  print(f'Total missmatches: {missmatch}')

