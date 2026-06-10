import argparse
import cv2
import json
import Levenshtein
import matplotlib.pyplot as plt
import numpy as np
import os
import shutil
import sys
import time
from detectron2 import model_zoo
from detectron2.config import get_cfg
from detectron2.data.datasets import register_coco_instances
from detectron2.engine import DefaultPredictor, DefaultTrainer
from detectron2.utils.visualizer import Visualizer
from letterbox_utils import DSSLettersDataset, get_img_file_path, \
  parse_file_name, SINGLE_LETTERS_ONLY, LABEL_LOOKUP, TRAINING_SET
from scipy import stats
from train_by_labels import is_in_row, process, get_row
from verify import get_isa_text, process_image

TRAIN_IDS = ['2', '11', '24', '36', '45']
VAL_IDS = ['7', '17', '27', '37', '47']
ANNO_IDS = {}
DATASET_BASE = 'detection/dataset'
ANNOTATIONS = f'{DATASET_BASE}/annotations'
IMAGES_BASE = f'{DATASET_BASE}/images'
preprocessor = {"bf": 7, "blur": "median", "blur_size": 3, "threshold": 135,
                "threshold_type": 0}
config = "COCO-Detection/faster_rcnn_R_50_FPN_3x.yaml"
'''[43.5, 46.69, 46.99, 55.53, 49.27, 52.37, 52.35, 54.93, 54.22, 52.82, 54.66, 53.41, 56.79, 56.81, 55.66, 59.74, 61.05, 51.43, 58.42, 59.94, 58.52, 58.58, 60.79, 50.9]
min: 43.5 max: 61.05 mean: 54.39 median: 54.80 mode: 55.0 std: 4.57 Z-Low: 45.43 Z-High: 63.35'''

# R101 > R50 for accuracy
# FPN helps small objects
# config = "COCO-Detection/faster_rcnn_R_101_FPN_3x.yaml"

# 👉 Much higher accuracy, but slower
# config = "COCO-Detection/faster_rcnn_X_101_32x8d_FPN_3x.yaml"
'''[49.9, 52.61, 53.5, 64.06, 59.35, 58.35, 62.1, 61.46, 63.04, 61.97, 61.15, 61.68, 64.27, 62.65, 61.36, 64.68, 68.42, 61.36, 68.71, 63.41, 62.82, 66.67, 69.74, 59.39]
min: 49.9 max: 69.74 mean: 61.78 median: 62.03 mode: 60.0 std: 4.67 Z-Low: 52.62 Z-High: 70.93'''

cfg = get_cfg()
cfg.merge_from_file(model_zoo.get_config_file(config))
cfg.MODEL.WEIGHTS = model_zoo.get_checkpoint_url(config)

cfg.MODEL.ROI_HEADS.BATCH_SIZE_PER_IMAGE = 128 # or 256
cfg.MODEL.ROI_HEADS.NUM_CLASSES = len(LABEL_LOOKUP) - 1  # <-- number of letters

cfg.OUTPUT_DIR = "detection/output"

cfg.MODEL.ANCHOR_GENERATOR.SIZES = [[4, 8, 16, 32]]
cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = 0.3 # or 0.2
cfg.MODEL.DEVICE = 'cpu'


def append_data(conf, sample):
  sample = process(sample)
  if sample is None:
    return

  filename = f'{sample["fragment"]}-{sample["srow"]}-{sample["erow"]}'
  h, w = sample['image'].shape[:2]
  conf["images"].append(
      {"id": filename, "file_name": filename + '.jpg', "height": h, "width": w})
  cv2.imwrite(f'{IMAGES_BASE}/{conf["type"]}/{filename}.jpg', sample['image'])

  for letter_box in sample["boxes"]:
    if len(letter_box['value']) > 1 or letter_box['value'] < 'א' or letter_box['value'] > 'ת':
      continue
    x, y = letter_box['x1'], letter_box['y1']
    width, height = letter_box['x2'] - x, letter_box['y2'] - y
    letter_id = f'{filename}-{x}-{y}'
    if letter_id in ANNO_IDS:
      raise ValueError(
          f'Duplicate id: {letter_id} detected. LetterBox: {letter_box}')
    ANNO_IDS[letter_id] = 1
    conf["annotations"].append(
        {"id": letter_id, "image_id": filename, "category_id": ord(letter_box["value"]) - ord('א'),
         "bbox": [x, y, width, height], "area": width * height, "iscrowd": 0})


def setup_samples(preprocessor=None):
  if os.path.exists(IMAGES_BASE):
    shutil.rmtree(IMAGES_BASE)
  os.makedirs(f'{DATASET_BASE}/annotations', exist_ok=True)
  os.makedirs(f'{IMAGES_BASE}/train', exist_ok=True)
  os.makedirs(f'{IMAGES_BASE}/val', exist_ok=True)

  train_conf = {"type": 'train', "images": [], "annotations": [], "categories": []}
  val_conf = {"type": 'val', "images": [], "annotations": [], "categories": []}
  for c in range(len(LABEL_LOOKUP)):
    train_conf["categories"].append({"id": c, "name": LABEL_LOOKUP[c]})
    val_conf["categories"].append({"id": c, "name": LABEL_LOOKUP[c]})

  image_start = time.time()
  for frag in TRAINING_SET:
    conf = train_conf if parse_file_name(frag)[2] not in VAL_IDS else val_conf
    row_30 = get_row(frag, 30)
    for r in range(1, 33):
      # append_data(conf, {'fragment': frag, 'srow': r, 'erow': r, 'preprocessor': preprocessor})
      # if r % 3 == 1:
        # append_data(conf, {'fragment': frag, 'srow': r, 'erow': r+2, 'preprocessor': preprocessor})
      if r % 7 == 1:
        append_data(conf, {'fragment': frag, 'srow': r, 'erow': r+6, 'preprocessor': preprocessor})
      # if r % 10 == 1 and row_30 is not None:
        # append_data(conf, {'fragment': frag, 'srow': r, 'erow': r+9, 'preprocessor': preprocessor})
      # if r % 14 == 1 and row_30 is None:
        # append_data(conf, {'fragment': frag, 'srow': r, 'erow': r+13, 'preprocessor': preprocessor})

  print(f'Files creation time: {time.time() - image_start} seconds')

  train_conf.pop('type')
  val_conf.pop('type')
  with open(f"{ANNOTATIONS}/train.json", "w", encoding="utf-8") as f:
    json.dump(train_conf, f, indent=True)
  with open(f"{ANNOTATIONS}/val.json", "w", encoding="utf-8") as f:
    json.dump(val_conf, f, indent=True)


def setup_data(preprocessor):
  if os.path.exists(IMAGES_BASE):
    shutil.rmtree(IMAGES_BASE)
  os.makedirs(f'{DATASET_BASE}/annotations', exist_ok=True)
  os.makedirs(f'{IMAGES_BASE}/train', exist_ok=True)
  os.makedirs(f'{IMAGES_BASE}/val', exist_ok=True)

  train_conf = {"images": [], "annotations": [], "categories": []}
  val_conf = {"images": [], "annotations": [], "categories": []}
  for c in range(len(LABEL_LOOKUP)):
    train_conf["categories"].append({"id": c, "name": LABEL_LOOKUP[c]})
    val_conf["categories"].append({"id": c, "name": LABEL_LOOKUP[c]})

  files = {}
  letter_id = 0
  dataset = DSSLettersDataset(filter=SINGLE_LETTERS_ONLY)
  for _, label, letter_box in dataset:
    filename = letter_box['filename']
    image_id = parse_file_name(filename)[2]
    if filename not in files:
      files[filename] = image_id
    conf = train_conf if image_id not in VAL_IDS else val_conf

    x, y = letter_box['x1'], letter_box['y1']
    width, height = letter_box['x2'] - x, letter_box['y2'] - y
    conf["annotations"].append(
        {"id": letter_id, "image_id": image_id, "category_id": label,
         "bbox": [x, y, width, height], "area": width * height, "iscrowd": 0})
    letter_id += 1

  for filename, id in files.items():
    conf, path = (train_conf, f'{IMAGES_BASE}/train') \
      if id not in VAL_IDS else (val_conf, f'{IMAGES_BASE}/val')
    file_path = get_img_file_path(filename, 9)
    img = process_image(cv2.imread(file_path), preprocessor)[0]
    h, w = img.shape[:2]
    conf["images"].append(
        {"id": id, "file_name": filename + '.jpg', "height": h, "width": w})
    os.makedirs(path, exist_ok=True)
    cv2.imwrite(f'{path}/{filename}.jpg', img)

  with open(f"{ANNOTATIONS}/train.json", "w", encoding="utf-8") as f:
    json.dump(train_conf, f, indent=True)
  with open(f"{ANNOTATIONS}/val.json", "w", encoding="utf-8") as f:
    json.dump(val_conf, f, indent=True)


def train(iters, preprocessor, samples=False):
  setup_samples(preprocessor) if samples else setup_data(preprocessor)

  register_coco_instances(
      "dss_train",
      {},
      f"{ANNOTATIONS}/train.json",
      f"{IMAGES_BASE}/train"
  )

  register_coco_instances(
      "dss_val",
      {},
      f"{ANNOTATIONS}/val.json",
      f"{IMAGES_BASE}/val"
  )

  cfg.DATASETS.TRAIN = ("dss_train",)
  cfg.DATASETS.TEST = ("dss_val",)

  cfg.SOLVER.IMS_PER_BATCH = 1
  cfg.SOLVER.BASE_LR = 0.00025
  # cfg.SOLVER.STEPS = (12000, 16000)
  # cfg.SOLVER.GAMMA = 0.1

  cfg.DATALOADER.NUM_WORKERS = 2

  cfg.SOLVER.MAX_ITER = iters # 5000 or 20000 recommended

  trainer = DefaultTrainer(cfg)
  trainer.resume_or_load(resume=False)
  trainer.train()


def evaluate(test_id, display=True, model="model_final.pth",
    preprocessor=None):
  cfg.MODEL.WEIGHTS = f'{cfg.OUTPUT_DIR}/{model}'
  cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = 0.60
  cfg.MODEL.RPN.PRE_NMS_TOPK_TEST = 12000
  cfg.MODEL.RPN.POST_NMS_TOPK_TEST = 6000
  cfg.TEST.DETECTIONS_PER_IMAGE = 2000

  start_time = time.time()
  predictor = DefaultPredictor(cfg)
  img_file = f'../images/isaiah/columns/column_9_{test_id}.jpg'
  image = process_image(cv2.imread(img_file), preprocessor)[0]
  outputs = predictor(image)
  print(outputs)
  print(f'Prediction took {time.time() - start_time} seconds')

  instances = outputs["instances"].to("cpu")
  if len(instances.pred_boxes) == 0:
    return 0

  print(instances.scores.min())
  print(instances.scores.max())

  boxes = instances.pred_boxes.tensor.numpy()
  classes = instances.pred_classes.numpy()

  test_file = f'isaiah-column-{test_id}'
  letter_boxes = []
  for box, cls in zip(boxes, classes):
    x1, y1, x2, y2 = map(int, box)

    letter_boxes.append({
      "filename": test_file,
      "type": "Letter",
      "x1": x1,
      "y1": y1,
      "x2": x2,
      "y2": y2,
      "value": LABEL_LOOKUP[cls]
    })

  dataset = DSSLettersDataset(
      fragments=[test_file],
      filter=lambda letter_box: letter_box['type'] == 'Row')
  row_boxes = []
  for _, _, row_box in dataset:
    row_box['_letterBoxes'] = []
    row_box['_text'] = ''
    row_boxes.append(row_box)

  added_letters = 0
  for letter_box in sorted(letter_boxes, key=lambda x:x['x2'], reverse=True):
    for row_box in row_boxes:
      if is_in_row(row_box, letter_box):
        if len(row_box['_letterBoxes']) > 0 and (
            row_box['_letterBoxes'][-1]['x1'] - letter_box['x2'] >= 5):
          row_box['_text'] += ' '
        row_box['_text'] += letter_box['value']
        row_box['_letterBoxes'].append(letter_box)
        added_letters += 1
        break
  print(f'{added_letters} letters added')

  target_text = get_isa_text(test_id)
  pred_text = ''
  for row_box in row_boxes:
    pred_text += row_box['_text'] + '\n'

  ld = Levenshtein.distance(target_text, pred_text)
  percent = round((len(target_text) - ld) * 100 / len(target_text), 2)
  print(f"{test_id} Diff: {ld}, {percent}%")

  if display:
    print('Pred Text:\n', pred_text)
    print('\nTarget Text:\n', target_text)
    v = Visualizer(image[:, :, ::-1], scale=1.0)
    out = v.draw_instance_predictions(outputs["instances"].to("cpu"))
    plt.imshow(out.get_image()[:, :, ::-1])
    plt.show()

  return percent


def verify(model="model_final.pth", preprocessor=None):
  percents = []
  for c in [2, 4, 7, 9, 11, 12, 13, 14, 16, 17, 18, 20, 24, 26, 27, 29, 36, 37,
            40, 44, 45, 47, 48, 53]:
    percents.append(evaluate(c, False, model, preprocessor=preprocessor))
  print(percents)
  percents = np.array(percents)
  mean, std = percents.mean(), percents.std()
  print('min:', percents.min(), 'max:', percents.max(),
        f'mean: {mean:.2f} median: {np.median(percents):.2f}',
        'mode:', stats.mode(np.round(percents / 5) * 5).mode, f'std: {std:.2f}',
        f'Z-Low: {mean - std * 1.96:.2f} Z-High: {mean + std * 1.96:.2f}')


if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument('--preprocess', action='store_true')
  parser.add_argument('--iters', type=int, default=5000)
  parser.add_argument('--samples', action='store_true')

  args = parser.parse_args()
  pp = preprocessor if args.preprocess else {}
  iters = args.iters
  samples = args.samples
  if samples:
    cfg.INPUT.MIN_SIZE_TRAIN = (512,) # or (1024,) or  (1280,)
    cfg.INPUT.MAX_SIZE_TRAIN = 1280 # or 2500 or 3000
    cfg.INPUT.MIN_SIZE_TEST = 512
    cfg.INPUT.MAX_SIZE_TEST = 1280
  else:
    cfg.INPUT.MIN_SIZE_TRAIN = (800,) # or (1024,) or  (1280,)
    cfg.INPUT.MAX_SIZE_TRAIN = 1600 # or 2500 or 3000
    cfg.INPUT.MIN_SIZE_TEST = 800
    cfg.INPUT.MAX_SIZE_TEST = 1600

  train(iters, preprocessor=pp, samples=samples)
  # verify('model_final_50_5000.pth', preprocessor=pp)
  verify('model_final.pth', preprocessor=pp)
  evaluate(48, True, preprocessor=pp)