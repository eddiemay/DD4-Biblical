from detectron2 import model_zoo
from detectron2.config import get_cfg
from detectron2.data.datasets import register_coco_instances
from detectron2.engine import DefaultPredictor, DefaultTrainer
from detectron2.utils.visualizer import Visualizer


register_coco_instances(
    "dss_train",
    {},
    "./dataset/annotations/train.json",
    "./dataset/images/train"
)

register_coco_instances(
    "dss_val",
    {},
    "./dataset/annotations/val.json",
    "./dataset/images/val"
)

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
cfg.SOLVER.MAX_ITER = 5000

cfg.MODEL.ROI_HEADS.BATCH_SIZE_PER_IMAGE = 128
cfg.MODEL.ROI_HEADS.NUM_CLASSES = num_classes  # <-- your number of letters

cfg.OUTPUT_DIR = "./output_dss"

cfg.MODEL.ANCHOR_GENERATOR.SIZES = [[8, 16, 32, 64]]
cfg.INPUT.MIN_SIZE_TRAIN = (800, 1024)
cfg.INPUT.MAX_SIZE_TRAIN = 1333
cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = 0.3


trainer = DefaultTrainer(cfg)
trainer.resume_or_load(resume=False)
trainer.train()

cfg.MODEL.WEIGHTS = "./output_dss/model_final.pth"
cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = 0.5

predictor = DefaultPredictor(cfg)

outputs = predictor(image)

v = Visualizer(image[:, :, ::-1], scale=1.0)
out = v.draw_instance_predictions(outputs["instances"].to("cpu"))

plt.imshow(out.get_image()[:, :, ::-1])