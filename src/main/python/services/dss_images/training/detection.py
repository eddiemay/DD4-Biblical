import cv2
import json
import matplotlib.pyplot as plt
import time
from detectron2 import model_zoo
from detectron2.config import get_cfg
from detectron2.data.datasets import register_coco_instances
from detectron2.engine import DefaultPredictor, DefaultTrainer
from detectron2.utils.visualizer import Visualizer
from letterbox_utils import DSSLettersDataset, get_img_file_path, parse_file_name, \
  SINGLE_LETTERS_ONLY

TRAIN_IDS = ['2', '11', '24', '36', '45']
VAL_IDS = ['7', '17', '27', '37', '47']
DATASET_BASE = 'detection/dataset'
ANNOTATIONS = f'{DATASET_BASE}/annotations'
IMAGES_BASE = f'{DATASET_BASE}/images'


def setup_data():
  dataset = DSSLettersDataset(filter=SINGLE_LETTERS_ONLY)
  train_conf = {"images": [], "annotations": [], "categories": []}
  val_conf = {"images": [], "annotations": [], "categories": []}
  for c in range(len(dataset.classes)):
    train_conf["categories"].append({"id": c, "name": dataset.classes[c]})
    val_conf["categories"].append({"id": c, "name": dataset.classes[c]})

  files = {}
  letter_id = 0
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
    conf, path = (train_conf, f'{IMAGES_BASE}/train') if id not in VAL_IDS else (val_conf, f'{IMAGES_BASE}/val')
    file_path = get_img_file_path(filename, 9)
    img = cv2.imread(file_path)
    h, w = img.shape[:2]
    conf["images"].append({"id": id, "file_name": filename + '.jpg', "height": h, "width": w})
    cv2.imwrite(f'{path}/{filename}.jpg', img)

  with open(f"{ANNOTATIONS}/train.json", "w", encoding="utf-8") as f:
    json.dump(train_conf, f, indent=True)
  with open(f"{ANNOTATIONS}/val.json", "w", encoding="utf-8") as f:
    json.dump(val_conf, f, indent=True)


def run(train=False, iters=500):
  register_coco_instances(
      "dss_train",
      {},
      f"./{ANNOTATIONS}/train.json",
      f"./{IMAGES_BASE}/train"
  )

  register_coco_instances(
      "dss_val",
      {},
      f"./{ANNOTATIONS}/val.json",
      f"./{IMAGES_BASE}/val"
  )

  num_classes = 27
  config = "COCO-Detection/faster_rcnn_R_50_FPN_3x.yaml"

  # R101 > R50 for accuracy
  # FPN helps small objects
  # config = "COCO-Detection/faster_rcnn_R_101_FPN_3x.yaml"

  # 👉 Much higher accuracy, but slower
  # config "COCO-Detection/faster_rcnn_X_101_32x8d_FPN_3x.yaml"

  cfg = get_cfg()
  cfg.merge_from_file(model_zoo.get_config_file(config))

  cfg.DATASETS.TRAIN = ("dss_train",)
  cfg.DATASETS.TEST = ("dss_val",)

  cfg.DATALOADER.NUM_WORKERS = 2

  cfg.MODEL.WEIGHTS = model_zoo.get_checkpoint_url(config)

  cfg.SOLVER.IMS_PER_BATCH = 2
  cfg.SOLVER.BASE_LR = 0.00025
  cfg.SOLVER.MAX_ITER = iters # 5000 or 20000 recommended
  # cfg.SOLVER.STEPS = (12000, 16000)
  # cfg.SOLVER.GAMMA = 0.1

  cfg.MODEL.ROI_HEADS.BATCH_SIZE_PER_IMAGE = 128 # or 256
  cfg.MODEL.ROI_HEADS.NUM_CLASSES = num_classes  # <-- your number of letters

  cfg.OUTPUT_DIR = "./output_dss"

  cfg.MODEL.ANCHOR_GENERATOR.SIZES = [[8, 16, 32, 64]] # or [[4, 8, 16, 32]] or [[2, 4, 8, 16]]
  cfg.INPUT.MIN_SIZE_TRAIN = (800, 1024) # or (1024, 1280)
  cfg.INPUT.MAX_SIZE_TRAIN = 1333 # or 1600
  cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = 0.3 # or 0.2
  cfg.MODEL.DEVICE = 'cpu'

  if train:
    trainer = DefaultTrainer(cfg)
    trainer.resume_or_load(resume=False)
    trainer.train()

  cfg.MODEL.WEIGHTS = "./output_dss/model_final_50_5000.pth"
  cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = 0.60
  cfg.MODEL.RPN.PRE_NMS_TOPK_TEST = 12000
  cfg.MODEL.RPN.POST_NMS_TOPK_TEST = 6000
  cfg.TEST.DETECTIONS_PER_IMAGE = 2000

  start_time = time.time()
  predictor = DefaultPredictor(cfg)
  image = cv2.imread('../images/isaiah/columns/column_9_18.jpg')
  outputs = predictor(image)

  print(outputs)
  instances = outputs["instances"].to("cpu")
  print(instances.scores.min())
  print(instances.scores.max())
  print(f'Prediction took {time.time() - start_time} seconds')

  v = Visualizer(image[:, :, ::-1], scale=1.0)
  out = v.draw_instance_predictions(outputs["instances"].to("cpu"))
  plt.imshow(out.get_image()[:, :, ::-1])
  plt.show()


if __name__ == '__main__':
  setup_data()
  run(False)