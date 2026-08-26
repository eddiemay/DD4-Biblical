import argparse
import cv2
import json
import Levenshtein
import matplotlib.pyplot as plt
import numpy as np
import os
import shutil
import time
from detectron2 import model_zoo
from detectron2.config import get_cfg
from detectron2.data.datasets import register_coco_instances
from detectron2.engine import DefaultPredictor, DefaultTrainer
from detectron2.engine.hooks import BestCheckpointer
from detectron2.evaluation import COCOEvaluator
from detectron2.utils.visualizer import Visualizer
from label_fragment import LETTERBOX_BY_FRAGMENT_URL, \
	LETTERBOX_BATCH_CREATE_URL, LETTERBOX_BATCH_DELETE_URL, send_json_req
from letterbox_utils import DSSLettersDataset, get_img_file_path, LABEL_LOOKUP, \
	SINGLE_LETTERS_ONLY, TRAINING_SET, VAL_SET, WAR_SET, get_frag_text, is_in_row, \
	process_image, intersection_over_union
from scipy import stats
from train_by_labels import process
from urllib import request

ANNO_IDS = {}
DATASET_BASE = 'detect_letters/dataset'
ANNOTATIONS = f'{DATASET_BASE}/annotations'
IMAGES_BASE = f'{DATASET_BASE}/images'
preprocessor = {"gray": True, "blur": "gaussian", "blur_size": 3}
# config = "COCO-Detection/faster_rcnn_R_50_FPN_3x.yaml"
'''
[43.5, 46.69, 46.99, 55.53, 49.27, 52.37, 52.35, 54.93, 54.22, 52.82, 54.66, 53.41, 56.79, 56.81, 55.66, 59.74, 61.05, 51.43, 58.42, 59.94, 58.52, 58.58, 60.79, 50.9]
min: 43.5 max: 61.05 mean: 54.39 median: 54.80 mode: 55.0 std: 4.57 Z-Low: 45.43 Z-High: 63.35

800x1600, [4,8,16,32], 5000, 7:56:20
iter: 4999  total_loss: 0.7535  loss_cls: 0.2568  loss_box_reg: 0.2721  loss_rpn_cls: 0.05465  loss_rpn_loc: 0.1655
[46.87, 54.29, 52.2, 60.75, 44.38, 55.86, 59.23, 58.13, 62.25, 59.9, 60.04, 60.85, 62.14, 63.69, 64.1, 64.37, 66.57, 45.53, 59.06, 67.57, 66.83, 65.77, 69.16, 55.96]
min: 44.38 max: 69.16 mean: 59.40 median: 60.39 mode: 60.0 std: 6.69 Z-Low: 46.29 Z-High: 72.51

800x1600, [8,16,32,64], 5000, 20:48:29 (M2 Pro)
iter: 4999  total_loss: 0.7975  loss_cls: 0.2792  loss_box_reg: 0.2903  loss_rpn_cls: 0.05824  loss_rpn_loc: 0.1654
[47.83, 53.03, 50.08, 60.94, 49.27, 58.29, 59.66, 60.81, 62.65, 59.9, 58.66, 58.79, 63.48, 62.24, 59.22, 63.95, 64.96, 46.3, 56.63, 66.25, 64.04, 65.27, 69.53, 54.34]
min: 46.3 max: 69.53 mean: 59.00 median: 59.78 mode: 60.0 std: 6.00 Z-Low: 47.24 Z-High: 70.77

800x1600, [8,16,32,64], 5000, 20:29:13 (M2 Pro)
iter: 4999  total_loss: 0.8785  loss_cls: 0.3392  loss_box_reg: 0.3206  loss_rpn_cls: 0.06343  loss_rpn_loc: 0.169
[37.3, 41.67, 47.72, 52.04, 33.82, 48.87, 47.53, 46.44, 52.57, 54.9, 52.1, 50.38, 54.53, 55.43, 59.96, 61.08, 60.21, 36.6, 48.86, 64.52, 60.96, 53.85, 60.31, 45.29]
min: 33.82 max: 64.52 mean: 51.12 median: 52.07 mode: 50.0 std: 8.06 Z-Low: 35.33 Z-High: 66.92
[06/11 11:31:59 d2.checkpoint.detection_checkpoint]: [DetectionCheckpointer] Loading from detect
'''

# R101 > R50 for accuracy
# FPN helps small objects
# config = "COCO-Detection/faster_rcnn_R_101_FPN_3x.yaml"

# 👉 Much higher accuracy, but slower
config = "COCO-Detection/faster_rcnn_X_101_32x8d_FPN_3x.yaml"
'''
[49.9, 52.61, 53.5, 64.06, 59.35, 58.35, 62.1, 61.46, 63.04, 61.97, 61.15, 61.68, 64.27, 62.65, 61.36, 64.68, 68.42, 61.36, 68.71, 63.41, 62.82, 66.67, 69.74, 59.39]
min: 49.9 max: 69.74 mean: 61.78 median: 62.03 mode: 60.0 std: 4.67 Z-Low: 52.62 Z-High: 70.93

800x1600, [4,8,16,32], 5000, 12:52:47
iter: 4999  total_loss: 0.6978  loss_cls: 0.2279  loss_box_reg: 0.285  loss_rpn_cls: 0.04744  loss_rpn_loc: 0.1606
[55.06, 60.07, 57.32, 68.13, 52.76, 66.17, 66.42, 65.84, 69.04, 65.54, 66.46, 66.09, 70.54, 68.43, 66.32, 71.38, 72.27, 53.06, 66.34, 73.39, 70.99, 71.74, 74.71, 64.75]
min: 52.76 max: 74.71 mean: 65.95 median: 66.38 mode: 65.0 std: 6.03 Z-Low: 54.14 Z-High: 77.77
.55 instead of .60 threshold
[56.02, 60.7, 55.53, 69.53, 57.8, 66.17, 67.76, 66.69, 69.5, 65.15, 65.98, 64.92, 70.18, 67.61, 63.43, 71.8, 72.04, 56.87, 68.61, 71.66, 67.91, 72.97, 75.34, 65.56]
min: 55.53 max: 75.34 mean: 66.24 median: 67.15 mode: 70.0 std: 5.37 Z-Low: 55.71 Z-High: 76.76

800x1600, [4,8,16,32], 5000, 12:52:47, Preprocessed Gray GBlur 3, .55 threshold
[48.86, 54.98, 55.28, 59.67, 44.38, 59.95, 60.33, 61.92, 64.62, 61.91, 63.35, 61.34, 65.06, 65.27, 63.58, 68.88, 65.35, 44.54, 61.19, 70.89, 68.34, 65.83, 68.12, 58.2]
min: 44.38 max: 70.89 mean: 60.91 median: 61.91 mode: 60.0 std: 6.90 Z-Low: 47.38 Z-High: 74.44

800x1600, [4,8,16,32], 5000, 12:52:14, .55 threshold, 512 batch_size_per_image
iter: 4999  total_loss: 1.027  loss_cls: 0.2344  loss_box_reg: 0.5713  loss_rpn_cls: 0.04986  loss_rpn_loc: 0.1622
[58.43, 65.09, 54.15, 70.1, 65.6, 70.02, 71.12, 68.13, 69.96, 66.77, 67.08, 67.75, 73.04, 69.67, 64.1, 74.0, 72.6, 61.32, 72.48, 70.96, 70.7, 71.85, 77.43, 65.25]
min: 54.15 max: 77.43 mean: 68.23 median: 69.81 mode: 70.0 std: 5.06 Z-Low: 58.32 Z-High: 78.14

800x1600, [4,8,16,32]. 10000, 60:05:55, .55 threshold, 1024 batch_size_per_image
iter: 9999  total_loss: 0.9716  loss_cls: 0.06698  loss_box_reg: 0.7344  loss_rpn_cls: 0.03224  loss_rpn_loc: 0.1369
[54.92, 57.49, 36.02, 65.46, 72.87, 68.72, 70.32, 68.97, 67.13, 61.58, 66.11, 68.5, 68.53, 68.36, 56.7, 72.11, 73.72, 63.49, 75.79, 66.39, 64.26, 68.73, 78.38, 64.44]
min: 36.02 max: 78.38 mean: 65.79 median: 67.75 mode: 70.0 std: 8.33 Z-Low: 49.46 Z-High: 82.12
.80 threshold
[63.25, 68.43, 56.75, 74.49, 72.38, 72.51, 75.75, 75.05, 75.03, 70.47, 71.43, 76.57, 74.62, 73.59, 70.47, 76.0, 79.02, 66.44, 79.9, 77.41, 75.29, 76.25, 83.61, 72.68]
min: 56.75 max: 83.61 mean: 73.22 median: 74.56 mode: 75.0 std: 5.45 Z-Low: 62.55 Z-High: 83.90
.85 threshold
[62.9, 69.06, 59.19, 75.19, 70.25, 72.81, 76.11, 75.83, 76.15, 70.6, 73.08, 77.53, 75.41, 75.58, 72.39, 76.55, 79.3, 65.31, 79.5, 78.72, 77.22, 75.75, 83.56, 72.11]
min: 59.19 max: 83.56 mean: 73.75 median: 75.50 mode: 75.0 std: 5.39 Z-Low: 63.19 Z-High: 84.32
.90 threshold
[62.84, 68.99, 60.89, 75.51, 66.42, 71.98, 75.14, 75.83, 76.42, 71.64, 73.5, 77.26, 76.2, 76.07, 73.5, 77.22, 78.96, 62.72, 78.71, 80.11, 79.37, 74.58, 82.67, 70.56]
min: 60.89 max: 82.67 mean: 73.63 median: 75.33 mode: 75.0 std: 5.61 Z-Low: 62.63 Z-High: 84.63

1280x2043, [4,8,16,32]. 5000, 27:06:28, .55 threshold, 1024 batch_size_per_image
 iter: 4999  total_loss: 1.087  loss_cls: 0.1415  loss_box_reg: 0.7596  loss_rpn_cls: 0.03509  loss_rpn_loc: 0.1287
[69.99, 72.33, 58.62, 77.04, 78.54, 75.18, 76.11, 79.49, 78.26, 72.42, 75.43, 78.7, 77.48, 78.68, 73.13, 77.89, 82.03, 76.87, 83.42, 80.39, 78.01, 78.87, 86.65, 75.98]
min: 58.62 max: 86.65 mean: 76.73 median: 77.69 mode: 80.0 std: 5.18 Z-Low: 66.58 Z-High: 86.88
.60 threshold
[71.37, 73.59, 59.92, 78.94, 78.78, 75.77, 76.78, 80.86, 79.58, 73.07, 77.02, 79.67, 78.39, 79.5, 74.76, 79.11, 83.04, 77.23, 84.06, 81.22, 79.08, 79.88, 86.96, 76.92]
min: 59.92 max: 86.96 mean: 77.73 median: 78.86 mode: 80.0 std: 5.04 Z-Low: 67.86 Z-High: 87.60
.70 threshold
[71.78, 75.75, 63.25, 80.79, 78.05, 76.36, 78.92, 80.73, 80.9, 74.82, 78.74, 80.57, 79.73, 80.74, 76.68, 80.45, 83.93, 77.19, 84.26, 82.67, 80.3, 80.38, 87.33, 79.48]
min: 63.25 max: 87.33 mean: 78.91 median: 80.02 mode: 80.0 std: 4.57 Z-Low: 69.95 Z-High: 87.87
.75 threshold
[71.99, 76.24, 63.9, 80.85, 77.86, 76.9, 79.59, 80.6, 81.23, 75.47, 79.02, 80.57, 79.55, 81.5, 77.42, 80.33, 83.87, 76.73, 84.21, 83.58, 81.02, 80.1, 87.33, 78.98]
min: 63.9 max: 87.33 mean: 79.12 median: 79.84 mode: 80.0 std: 4.47 Z-Low: 70.36 Z-High: 87.87
.80 threshold
[70.89, 76.31, 64.96, 80.92, 77.33, 77.07, 79.34, 80.73, 81.03, 75.34, 79.57, 80.63, 79.49, 81.91, 77.79, 80.76, 84.04, 76.24, 84.11, 83.78, 81.59, 79.71, 87.23, 78.29]
min: 64.96 max: 87.23 mean: 79.13 median: 79.64 mode: 80.0 std: 4.45 Z-Low: 70.41 Z-High: 87.84
.85 threshold
[69.72, 74.84, 65.53, 79.64, 75.73, 75.59, 78.43, 79.49, 80.43, 74.43, 79.16, 80.01, 79.25, 81.43, 78.24, 80.09, 83.31, 74.6, 83.71, 83.85, 82.02, 78.87, 86.34, 76.48]
min: 65.53 max: 86.34 mean: 78.38 median: 79.20 mode: 80.0 std: 4.49 Z-Low: 69.58 Z-High: 87.18
.90 threshold
[66.76, 72.26, 64.55, 77.29, 73.59, 74.05, 76.17, 76.88, 79.25, 73.07, 77.16, 79.12, 78.45, 79.64, 77.2, 78.87, 81.53, 71.79, 81.58, 82.54, 81.09, 77.37, 84.5, 73.42]
min: 64.55 max: 84.5 mean: 76.59 median: 77.25 mode: 75.0 std: 4.66 Z-Low: 67.45 Z-High: 85.73

1280x2043, [4,8,16,32]. 5000 + 2000, 27:06 + 11:02, .75 threshold, 1024 batch_size_per_image
iter: 6999  total_loss: 1.009  loss_cls: 0.1029  loss_box_reg: 0.7689  loss_rpn_cls: 0.02253  loss_rpn_loc: 0.1247
[76.81, 78.89, 64.31, 83.65, 81.15, 79.15, 80.8, 83.54, 83.6, 75.99, 80.4, 81.67, 82.53, 82.67, 78.16, 84.04, 85.71, 78.23, 86.14, 84.55, 82.88, 81.66, 89.53, 80.97]
min: 64.31 max: 89.53 mean: 81.13 median: 81.66 mode: 80.0 std: 4.65 Z-Low: 72.02 Z-High: 90.23
.70 threshold
[76.39, 77.49, 62.93, 82.95, 81.4, 78.44, 80.32, 82.23, 83.07, 75.34, 79.64, 81.32, 82.11, 81.57, 76.91, 82.89, 84.77, 78.46, 86.39, 83.51, 81.52, 81.49, 88.9, 79.73]
min: 62.93 max: 88.9 mean: 80.41 median: 81.44 mode: 80.0 std: 4.76 Z-Low: 71.07 Z-High: 89.75
.80 threshold
[76.6, 79.79, 65.53, 83.91, 80.43, 79.44, 81.17, 83.8, 83.99, 76.77, 81.23, 82.29, 82.78, 83.15, 78.68, 84.77, 86.33, 77.87, 86.14, 84.82, 83.6, 81.66, 89.53, 81.22]
min: 65.53 max: 89.53 mean: 81.48 median: 81.97 mode: 80.0 std: 4.51 Z-Low: 72.63 Z-High: 90.33
.85 threshold
[75.91, 79.58, 66.42, 83.65, 79.07, 79.5, 80.87, 83.93, 83.53, 76.64, 81.09, 82.36, 82.41, 83.15, 78.83, 84.9, 85.66, 76.87, 86.04, 85.52, 84.24, 81.38, 88.8, 80.1]
min: 66.42 max: 88.8 mean: 81.27 median: 81.87 mode: 80.0 std: 4.43 Z-Low: 72.59 Z-High: 89.95

1280x2043, [4,8,16,32]. 7000 + 777, 38:08, .80 threshold, 1024 batch_size_per_image
iter: 7776  total_loss: 0.996  loss_cls: 0.09002  loss_box_reg: 0.7619  loss_rpn_cls: 0.0229  loss_rpn_loc: 0.126
[78.32, 82.37, 67.32, 83.78, 82.36, 80.86, 82.94, 83.8, 83.33, 78.33, 82.19, 83.8, 84.66, 84.94, 77.72, 84.53, 86.83, 78.23, 86.53, 85.38, 86.03, 82.72, 90.37, 83.28]
min: 67.32 max: 90.37 mean: 82.53 median: 83.31 mode: 85.0 std: 4.32 Z-Low: 74.06 Z-High: 91.00
.85 threshold
[77.7, 82.37, 67.8, 83.91, 81.4, 80.69, 82.57, 84.19, 83.66, 78.13, 82.4, 84.42, 85.03, 85.76, 78.9, 85.26, 86.94, 77.78, 86.44, 86.35, 85.96, 82.55, 90.94, 83.34]
min: 67.8 max: 90.94 mean: 82.69 median: 83.50 mode: 85.0 std: 4.40 Z-Low: 74.05 Z-High: 91.32
.90 threshold
[75.02, 81.05, 68.13, 83.65, 79.75, 79.98, 81.66, 84.06, 83.14, 77.22, 81.92, 84.22, 84.42, 85.21, 78.83, 84.71, 86.33, 75.42, 86.14, 86.97, 86.03, 81.55, 89.42, 81.66]
min: 68.13 max: 89.42 mean: 81.94 median: 82.53 mode: 85.0 std: 4.55 Z-Low: 73.02 Z-High: 90.85

1280x2043, [4,8,16,32]. 7777 + 2333, 38:08, .90 threshold, 2048 batch_size_per_image
iter: 9999  total_loss: 0.9209  loss_cls: 0.04377  loss_box_reg: 0.726  loss_rpn_cls: 0.02629  loss_rpn_loc: 0.1188
.95 threshold
[76.19, 81.53, 65.04, 84.1, 78.88, 78.91, 81.72, 83.02, 83.79, 75.47, 80.4, 83.74, 83.69, 83.7, 76.83, 84.17, 85.44, 73.56, 86.73, 85.03, 84.38, 80.6, 88.27, 81.72]
min: 65.04 max: 88.27 mean: 81.12 median: 82.37 mode: 85.0 std: 4.91 Z-Low: 71.50 Z-High: 90.74
.90 threshold
[78.05, 81.46, 64.47, 84.41, 81.3, 79.92, 82.75, 83.54, 84.06, 76.18, 80.95, 83.8, 84.24, 83.84, 78.09, 84.84, 86.44, 76.46, 87.43, 84.2, 84.24, 82.72, 89.53, 82.84]
min: 64.47 max: 89.53 mean: 81.91 median: 83.19 mode: 85.0 std: 4.82 Z-Low: 72.45 Z-High: 91.36
.85 threshold
[77.43, 80.56, 63.25, 84.1, 81.88, 79.56, 81.78, 82.69, 83.14, 76.25, 79.92, 83.18, 83.26, 83.08, 76.83, 83.98, 86.33, 77.96, 86.83, 83.44, 83.09, 81.66, 89.74, 83.66]
min: 63.25 max: 89.74 mean: 81.40 median: 82.88 mode: 85.0 std: 4.89 Z-Low: 71.81 Z-High: 90.99
.8 threshold
[76.88, 79.44, 61.46, 82.63, 81.54, 78.85, 81.29, 81.58, 82.35, 75.02, 79.09, 82.43, 82.11, 81.71, 75.8, 82.76, 85.71, 78.23, 86.83, 81.64, 81.88, 81.44, 89.42, 82.41]
min: 61.46 max: 89.42 mean: 80.52 median: 81.61 mode: 80.0 std: 5.07 Z-Low: 70.58 Z-High: 90.46
.75 threshold
[72.68, 75.4, 60.73, 79.26, 78.88, 76.48, 78.12, 78.51, 78.85, 73.98, 76.95, 80.22, 79.49, 79.44, 75.35, 80.39, 84.6, 78.87, 85.3, 78.31, 78.8, 81.72, 88.69, 78.35]
min: 60.73 max: 88.69 mean: 78.31 median: 78.82 mode: 80.0 std: 5.01 Z-Low: 68.49 Z-High: 88.13
Preprocessed to 1280x1920
NMS
[0.0, 71.09, 69.23, 76.86, 74.67, 73.55, 66.5, 76.56, 79.33, 76.4, 78.49, 75.24, 78.12, 79.82, 76.48, 80.63, 75.6, 76.26, 75.88, 79.32, 80.92, 80.5, 77.2, 80.89, 71.38, 77.24, 78.09, 79.78, 80.82, 82.96, 73.78, 75.65, 78.92, 82.0, 80.29, 85.55, 82.95, 83.43, 84.5, 86.78, 75.31, 83.39, 81.54, 81.57, 82.02, 80.16, 84.39, 88.59, 85.15, 79.42, 79.14, 77.19, 79.16, 59.43]
min: 0.0 max: 88.59 mean: 77.04 median: 79.15 mode: 80.0 std: 11.69 Z-Low: 54.12 Z-High: 99.95
no space
[0.0, 76.31, 74.45, 80.99, 78.63, 82.67, 74.71, 82.93, 84.86, 80.62, 82.79, 83.96, 86.1, 86.51, 83.69, 88.56, 84.88, 86.42, 84.89, 88.3, 87.96, 88.26, 87.16, 88.46, 85.66, 88.3, 87.49, 83.07, 89.25, 86.29, 77.76, 80.65, 85.01, 87.83, 87.62, 90.95, 86.56, 87.71, 90.57, 91.27, 84.69, 87.75, 86.69, 88.54, 89.73, 87.36, 88.36, 93.06, 87.4, 82.96, 82.18, 82.03, 82.13, 60.81]
min: 0.0 max: 93.06 mean: 83.22 median: 86.19 mode: 85.0 std: 12.54 Z-Low: 58.64 Z-High: 107.80
No preprocessing
NMS
[0.0, 73.43, 73.19, 80.28, 76.72, 82.22, 72.52, 80.7, 82.38, 79.26, 82.12, 81.22, 83.42, 84.65, 82.27, 84.39, 83.0, 83.78, 83.83, 85.04, 84.35, 84.25, 83.8, 84.91, 82.3, 86.04, 85.42, 80.65, 85.02, 81.38, 75.43, 74.15, 79.16, 82.47, 84.12, 87.28, 81.13, 84.88, 87.49, 86.83, 84.25, 85.48, 85.37, 87.66, 84.81, 85.22, 85.62, 90.79, 83.05, 77.52, 77.98, 75.94, 79.6, 62.06]
min: 0.0 max: 90.79 mean: 80.39 median: 83.03 mode: 85.0 std: 12.03 Z-Low: 56.81 Z-High: 103.96
No space
[0.0, 77.87, 75.69, 83.55, 80.05, 87.31, 78.93, 84.51, 87.45, 82.42, 85.22, 86.17, 88.22, 88.74, 85.69, 89.32, 86.13, 87.83, 89.65, 91.12, 89.25, 89.95, 88.94, 89.39, 89.22, 91.7, 90.33, 82.4, 90.18, 84.34, 78.52, 77.67, 83.31, 86.96, 88.92, 91.02, 84.28, 89.13, 91.35, 89.78, 89.94, 87.75, 88.61, 91.41, 90.72, 89.69, 89.27, 93.58, 85.32, 80.1, 80.3, 80.55, 81.81, 64.16]
min: 0.0 max: 93.58 mean: 84.37 median: 87.60 mode: 90.0 std: 12.70 Z-Low: 59.46 Z-High: 109.27

Preprocessed: cropped to 1280x1920, Gray scale, gblur 3, 7500 iters
NMS
[0.0, 76.88, 72.81, 81.11, 78.03, 82.07, 72.6, 81.7, 84.8, 80.72, 82.7, 82.11, 83.36, 84.91, 82.15, 86.3, 81.7, 82.4, 81.73, 85.32, 85.28, 85.29, 83.36, 85.82, 80.56, 85.21, 84.46, 80.75, 85.99, 81.47, 74.71, 75.36, 80.26, 85.61, 84.52, 89.62, 80.23, 85.21, 87.18, 88.61, 84.87, 84.02, 85.62, 87.39, 88.11, 85.02, 84.84, 91.47, 80.03, 79.02, 77.58, 75.1, 82.16, 64.48]
min: 0.0 max: 91.47 mean: 80.79 median: 82.55 mode: 85.0 std: 12.04 Z-Low: 57.18 Z-High: 104.39
No Space
[0.0, 80.84, 75.41, 85.94, 81.14, 87.88, 79.24, 85.1, 89.07, 83.15, 85.79, 88.62, 87.98, 89.9, 85.84, 90.33, 87.72, 89.15, 88.62, 91.29, 91.64, 90.21, 89.77, 91.11, 88.89, 92.31, 91.47, 82.58, 91.88, 85.68, 78.45, 80.22, 85.7, 90.22, 90.05, 93.88, 83.77, 89.13, 92.0, 92.14, 89.87, 87.09, 89.08, 91.58, 94.14, 90.02, 88.64, 94.94, 81.81, 81.57, 80.55, 79.49, 85.54, 69.04]
min: 0.0 max: 94.94 mean: 85.32 median: 88.62 mode: 90.0 std: 12.77 Z-Low: 60.29 Z-High: 110.35

Preprocessed: copped to 1280x1920, Gray scale, gblur 3, 10000 iters
NMS
[67.04, 78.18, 73.95, 83.76, 79.09, 80.6, 72.44, 84.06, 86.51, 81.5, 85.27, 85.78, 85.74, 85.83, 83.79, 86.56, 81.96, 84.75, 82.38, 87.39, 85.2, 86.4, 84.34, 87.64, 81.52, 86.31, 85.49, 81.76, 87.52, 82.96, 74.77, 77.32, 80.5, 85.09, 85.1, 89.73, 81.9, 85.53, 88.11, 89.36, 85.38, 85.43, 86.88, 88.22, 88.4, 85.22, 86.57, 93.35, 82.04, 78.62, 79.44, 74.85, 82.84, 62.76]
min: 62.76 max: 93.35 mean: 83.02 median: 84.92 mode: 85.0 std: 5.45 Z-Low: 72.33 Z-High: 93.71
No space
[69.11, 83.89, 77.89, 87.98, 81.64, 86.46, 78.61, 87.95, 90.53, 85.07, 87.78, 91.14, 90.49, 90.56, 87.84, 91.08, 86.3, 90.56, 88.53, 92.96, 91.14, 91.1, 89.63, 92.82, 89.3, 92.31, 91.66, 83.68, 92.27, 86.29, 78.8, 81.38, 85.09, 89.93, 89.72, 94.22, 86.22, 89.13, 91.93, 92.69, 90.29, 88.21, 90.28, 91.75, 94.14, 89.44, 89.76, 95.78, 84.49, 81.86, 81.8, 79.18, 86.58, 65.27]
min: 65.27 max: 95.78 mean: 87.31 median: 89.22 mode: 90.0 std: 5.80 Z-Low: 75.93 Z-High: 98.68
'''
threshold = .7

cfg = get_cfg()
cfg.merge_from_file(model_zoo.get_config_file(config))
cfg.MODEL.WEIGHTS = model_zoo.get_checkpoint_url(config)
cfg.merge_from_file("detect_letters/config.yaml")
# cfg.MODEL.ROI_HEADS.POSITIVE_FRACTION = 0.5


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
		if len(letter_box['value']) > 1 or letter_box['value'] < 'א' or letter_box[
			'value'] > 'ת':
			continue
		x, y = letter_box['x1'], letter_box['y1']
		width, height = letter_box['x2'] - x, letter_box['y2'] - y
		letter_id = f'{filename}-{x}-{y}'
		if letter_id in ANNO_IDS:
			raise ValueError(
					f'Duplicate id: {letter_id} detected. LetterBox: {letter_box}')
		ANNO_IDS[letter_id] = 1
		conf["annotations"].append(
				{"id": letter_id, "image_id": filename,
				 "category_id": ord(letter_box["value"]) - ord('א'),
				 "bbox": [x, y, width, height], "area": width * height, "iscrowd": 0})


def setup_samples(preprocessor=None):
	if os.path.exists(IMAGES_BASE):
		shutil.rmtree(IMAGES_BASE)
	os.makedirs(f'{DATASET_BASE}/annotations', exist_ok=True)
	os.makedirs(f'{IMAGES_BASE}/train', exist_ok=True)
	os.makedirs(f'{IMAGES_BASE}/val', exist_ok=True)

	train_conf = {"type": 'train', "images": [], "annotations": [],
								"categories": []}
	val_conf = {"type": 'val', "images": [], "annotations": [], "categories": []}
	for c in range(len(LABEL_LOOKUP)):
		train_conf["categories"].append({"id": c, "name": LABEL_LOOKUP[c]})
		val_conf["categories"].append({"id": c, "name": LABEL_LOOKUP[c]})

	image_start = time.time()
	for frag in TRAINING_SET:
		for r in range(1, 33):
			if r % 7 == 1:
				append_data(train_conf, {'fragment': frag, 'srow': r, 'erow': r + 6,
													 'preprocessor': preprocessor})

	for frag in VAL_SET:
		for r in range(1, 33):
			if r % 7 == 1:
				append_data(val_conf, {'fragment': frag, 'srow': r, 'erow': r + 6,
																 'preprocessor': preprocessor})

	print(f'Files creation time: {time.time() - image_start} seconds')

	train_conf.pop('type')
	val_conf.pop('type')
	with open(f"{ANNOTATIONS}/train.json", "w", encoding="utf-8") as f:
		json.dump(train_conf, f, indent=True)
	with open(f"{ANNOTATIONS}/val.json", "w", encoding="utf-8") as f:
		json.dump(val_conf, f, indent=True)


def resize(filename, img, start_x, end_x, target_x, max_x, start_y, end_y, target_y, max_y):
	y_buffer = max(target_y - (end_y - start_y), 0)
	x_buffer = max(target_x - (end_x - start_x), 0)
	y_start, y_end = int(max(start_y - y_buffer / 2, 0)), int(min(end_y + y_buffer / 2, max_y))
	x_start, x_end = int(max(start_x - x_buffer / 2, 0)), int(min(end_x + x_buffer / 2, max_x))
	letter_h, letter_w = y_end - y_start, x_end - x_start
	print(f'{filename} Image h,w: ({max_y}, {max_x}), Letter h,w: ({end_y - start_y}, {end_x - start_x}), Result: ({letter_h}, {letter_w})')
	return img[y_start:y_end,x_start:x_end], x_start, y_start


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
	fragments = []
	fragments.extend(TRAINING_SET)
	fragments.extend(VAL_SET)
	dataset = DSSLettersDataset(fragments, SINGLE_LETTERS_ONLY)
	for _, label, letter_box in dataset:
		filename = letter_box['filename']
		if filename not in files:
			files[filename] = {"id": filename, "min_x": None, "max_x": None,
												 "min_y": None, "max_y": None, "letter_boxes": []}
		file = files[filename]
		letter_box["label"] = label
		x, y = letter_box['x1'], letter_box['y1']
		if file["min_x"] is None or file["min_x"] > x:
			file["min_x"] = x
		if file["min_y"] is None or file["min_y"] > y:
			file["min_y"] = y
		if file["max_x"] is None or file["max_x"] < letter_box['x2']:
			file["max_x"] = letter_box['x2']
		if file["max_y"] is None or file["max_y"] < letter_box['y2']:
			file["max_y"] = letter_box['y2']
		file["letter_boxes"].append(letter_box)

	letter_id = 0
	min_sides, max_sides = [], []
	for filename, file in files.items():
		conf, path = (train_conf, f'{IMAGES_BASE}/train') \
			if filename not in VAL_SET else (val_conf, f'{IMAGES_BASE}/val')
		file_path = get_img_file_path(filename, 9)
		img = process_image(cv2.imread(file_path), preprocessor)[0]
		h, w = img.shape[:2]
		if file["max_y"] - file["min_y"] > file["max_x"] - file["min_x"]:
			min_side, max_side = file["max_x"] - file["min_x"], file["max_y"] - file["min_y"]
			img, x_start, y_start = resize(
					filename, img,
					file["min_x"], file["max_x"], file["max_x"] - file["min_x"] + 20, w,
					file["min_y"], file["max_y"], cfg.INPUT.MAX_SIZE_TRAIN, h)
		else:
			max_side, min_side = file["max_x"] - file["min_x"], file["max_y"] - file["min_y"]
			img, x_start, y_start = resize(
					filename, img,
					file["min_x"], file["max_x"], cfg.INPUT.MAX_SIZE_TRAIN, w,
					file["min_y"], file["max_y"], file["max_y"] - file["min_y"] + 20, h)

		min_sides.append(min_side)
		max_sides.append(max_side)
		h, w = img.shape[:2]
		conf["images"].append(
				{"id": filename, "file_name": filename + '.jpg', "height": h, "width": w})

		for letter_box in file["letter_boxes"]:
			x, y = letter_box['x1'], letter_box['y1']
			width, height = letter_box['x2'] - x, letter_box['y2'] - y
			conf["annotations"].append(
					{"id": letter_id, "image_id": filename, "category_id": letter_box["label"],
					 "bbox": [x - x_start, y - y_start, width, height],
					 "area": width * height, "iscrowd": 0})
			letter_id += 1

		os.makedirs(path, exist_ok=True)
		cv2.imwrite(f'{path}/{filename}.jpg', img)

	print_stats("Min Sides", min_sides)
	print_stats("Max Sides", max_sides)

	with open(f"{ANNOTATIONS}/train.json", "w", encoding="utf-8") as f:
		json.dump(train_conf, f, indent=True)
	with open(f"{ANNOTATIONS}/val.json", "w", encoding="utf-8") as f:
		json.dump(val_conf, f, indent=True)


class Trainer(DefaultTrainer):
	@classmethod
	def build_evaluator(cls, cfg, dataset_name, output_folder=None):
		if output_folder is None:
			output_folder = os.path.join(cfg.OUTPUT_DIR, "inference")

		return COCOEvaluator(dataset_name, output_dir=output_folder)

	def build_hooks(self):
		hooks = super().build_hooks()

		hooks.insert(
				-1,
				BestCheckpointer(
						self.cfg.TEST.EVAL_PERIOD,
						self.checkpointer,
						"bbox/AP75",      # metric to maximize
						mode="max"
				)
		)

		return hooks


def train(iters, preprocessor, samples=False, resume=False):
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

	cfg.SOLVER.MAX_ITER = iters  # 5000 or 20000 recommended

	trainer = Trainer(cfg)
	trainer.resume_or_load(resume=resume)
	trainer.train()


def predict(predictor, fragment, preprocessor=None):
	start_time = time.time()
	img_file = get_img_file_path(fragment, 9)
	image = process_image(cv2.imread(img_file), preprocessor)[0]
	if len(image.shape) == 2:
		image = cv2.cvtColor(image, cv2.COLOR_GRAY2BGR)
	outputs = predictor(image)
	# print(outputs)
	print(f'Prediction took {time.time() - start_time:.1f} seconds')

	instances = outputs["instances"].to("cpu")
	if len(instances.pred_boxes) == 0:
		return None, None

	y_offset = preprocessor["crop"][0] if preprocessor.get("crop") is not None else 0

	boxes = instances.pred_boxes.tensor.numpy()
	classes = instances.pred_classes.numpy()
	scores = instances.scores.numpy()

	letter_boxes = []
	for box, cls, score in zip(boxes, classes, scores):
		x1, y1, x2, y2 = map(int, box)
		letter_boxes.append({
			"filename": fragment,
			"type": "Letter",
			"x1": x1,
			"y1": y1 + y_offset,
			"x2": x2,
			"y2": y2 + y_offset,
			"value": LABEL_LOOKUP[cls],
			"_score": float(score)
		})

	from predict_letters import predict_letters
	predict_letters(letter_boxes)
	nms = []
	for box in sorted(letter_boxes, key=lambda b:b["_score"], reverse=True):
		keep = True
		for kept in nms:
			if intersection_over_union(box, kept) > 0.25:
				keep = False
				break
		if keep:
			nms.append(box)

	return image, outputs, letter_boxes, nms


def evaluate(predictor, fragment, display=True, preprocessor=None, override=False):
	image, outputs, letter_boxes, nms = predict(predictor, fragment, preprocessor)

	dataset = DSSLettersDataset(
			fragments=[fragment], overrides=[fragment] if override else [],
			filter=lambda letter_box: letter_box['type'] == 'Row')
	row_boxes = []
	for _, _, row_box in dataset:
		row_box['_letterBoxes'] = []
		row_box['_nmsBoxes'] = []
		row_box['_text'] = ''
		row_box['_predict_text'] = ''
		row_box['_remove_mismatch_text'] = ''
		row_box['_remove_union_text'] = ''
		row_box['_nms_text'] = ''
		row_box['_nms_rp_text'] = ''
		row_boxes.append(row_box)

	added_letters = 0
	matching_predictions = 0
	for letter_box in sorted(letter_boxes, key=lambda x: x['x2'], reverse=True):
		if letter_box['value'] == letter_box['_predicted']:
			matching_predictions += 1
		for row_box in row_boxes:
			if is_in_row(row_box, letter_box):
				row_lbs = row_box['_letterBoxes']
				if len(row_lbs) > 0 and (row_lbs[-1]['x1'] - letter_box['x2'] >= 5):
					row_box['_text'] += ' '
					row_box['_predict_text'] += ' '
				row_box['_text'] += letter_box['value']
				row_box['_predict_text'] += letter_box['_predicted']
				if letter_box['value'] == letter_box['_predicted']:
					if row_box.get('_last_non_removed') != None and \
							row_box['_last_non_removed']['x1'] - letter_box['x2'] >= 5:
						row_box['_remove_mismatch_text'] += ' '
					row_box['_remove_mismatch_text'] += letter_box['value']
					row_box['_last_non_removed'] = letter_box

				iou = intersection_over_union(row_box.get('_prev_letter'), letter_box)
				if iou < .25:
					# .125 [0.0, 80.18, 67.56, 84.25, 76.85, 77.81, 68.54, 75.56, 86.51, 76.72, 80.62, 83.29, 84.28, 86.35, 78.7, 86.82, 78.84, 84.89, 79.35, 87.11, 81.78, 82.65, 80.41, 86.91, 79.97, 87.55, 81.5, 77.02, 86.24, 77.91, 69.16, 71.27, 74.36, 79.56, 80.48, 88.06, 78.28, 82.41, 85.49, 88.07, 79.58, 81.62, 81.17, 88.57, 87.61, 81.27, 83.89, 90.42, 79.28, 74.41, 72.52, 71.6, 84.9, 59.23]
					# min: 0.0 max: 90.42 mean: 78.88 median: 80.55 mode: 80.0 std: 12.44 Z-Low: 54.49 Z-High: 103.26
					# .25 [0.0, 80.8, 67.71, 85.16, 76.58, 77.74, 68.54, 76.21, 87.4, 76.66, 80.72, 83.71, 84.83, 86.74, 79.11, 87.35, 78.72, 85.37, 78.27, 86.91, 82.09, 83.14, 80.47, 87.58, 79.52, 87.76, 81.64, 77.75, 87.15, 78.54, 69.38, 72.02, 75.52, 79.67, 80.87, 88.78, 79.0, 83.38, 85.65, 88.51, 79.92, 82.09, 81.98, 88.98, 87.75, 81.54, 84.17, 91.26, 79.89, 75.16, 73.72, 71.6, 85.4, 59.74]
					# min: 0.0 max: 91.26 mean: 79.26 median: 80.84 mode: 80.0 std: 12.51 Z-Low: 54.74 Z-High: 103.78
					# .33 [0.0, 80.52, 66.95, 84.88, 76.32, 77.66, 68.13, 76.03, 87.15, 76.3, 80.62, 83.29, 84.58, 86.54, 79.17, 87.09, 78.2, 85.51, 78.27, 86.63, 81.62, 82.44, 80.31, 87.16, 79.26, 87.28, 81.64, 77.79, 86.91, 78.39, 69.32, 72.25, 75.7, 79.44, 80.29, 88.78, 78.87, 83.16, 85.49, 88.61, 79.64, 81.98, 81.73, 88.22, 87.39, 80.88, 84.28, 91.2, 80.03, 75.39, 73.62, 71.85, 85.46, 59.84]
					# min: 0.0 max: 91.2 mean: 79.08 median: 80.57 mode: 80.0 std: 12.47 Z-Low: 54.64 Z-High: 103.51
					if row_box.get('_prev_letter') != None and row_box['_prev_letter'][
						'x1'] - letter_box['x2'] >= 5:
						row_box['_remove_union_text'] += ' '
					row_box['_remove_union_text'] += letter_box['value']
					row_box['_prev_letter'] = letter_box
				row_lbs.append(letter_box)
				added_letters += 1
				break
	print(f'{added_letters} letters added')

	added_letters = 0
	for letter_box in sorted(nms, key=lambda x: x['x2'], reverse=True):
		for row_box in row_boxes:
			if is_in_row(row_box, letter_box):
				row_lbs = row_box['_nmsBoxes']
				if len(row_lbs) > 0 and (row_lbs[-1]['x1'] - letter_box['x2'] >= 5):
					row_box['_nms_text'] += ' '
					row_box['_nms_rp_text'] += ' '
				row_box['_nms_text'] += letter_box['value']
				row_box['_nms_rp_text'] += letter_box["_predicted"]
				row_lbs.append(letter_box)
				added_letters += 1
				break
	print(f'NMS {added_letters} letters added')

	target_text = ''.join(get_frag_text(fragment).split())
	no_space = ''.join(target_text.split())
	no_space_len = len(no_space)
	pred_text = ''
	repred_text = ''
	remove_mismatch_text = ''
	remove_union_text = ''
	nms_text = ''
	nms_rp_text = ''
	for row_box in row_boxes:
		pred_text += row_box['_text'] + '\n'
		repred_text += row_box['_predict_text'] + '\n'
		remove_mismatch_text += row_box['_remove_mismatch_text'] + '\n'
		remove_union_text += row_box['_remove_union_text'] + '\n'
		nms_text += row_box['_nms_text'] + '\n'
		nms_rp_text += row_box['_nms_rp_text'] + '\n'

	ld = Levenshtein.distance(no_space, ''.join(pred_text.split()))
	percent = round((no_space_len - ld) * 100 / no_space_len, 2)
	rp_ld = Levenshtein.distance(no_space, ''.join(repred_text.split()))
	rp_percent = round((no_space_len - rp_ld) * 100 / no_space_len, 2)
	rm_ld = Levenshtein.distance(no_space, ''.join(remove_mismatch_text.split()))
	rm_percent = round((no_space_len - rm_ld) * 100 / no_space_len, 2)
	ru_ld = Levenshtein.distance(no_space, ''.join(remove_union_text.split()))
	ru_percent = round((no_space_len - ru_ld) * 100 / no_space_len, 2)
	nms_ld = Levenshtein.distance(no_space, ''.join(nms_text.split()))
	nms_percent = round((no_space_len - nms_ld) * 100 / no_space_len, 2)
	nms_rp_ld = Levenshtein.distance(no_space, ''.join(nms_rp_text.split()))
	nms_rp_percent = round((no_space_len - nms_rp_ld) * 100 / no_space_len, 2)
	print(
			f'{fragment} Diff: {ld} {percent}%, Repredict Diff: {rp_ld} {rp_percent}%,',
			f'Remove Miss Diff: {rm_ld} {rm_percent}%, Remove Union Text: {ru_ld} {ru_percent}%,',
			f'NMS Diff: {nms_ld} {nms_percent}%, NMS RP Diff: {nms_rp_ld} {nms_rp_percent}%,',
			f'Prediction Diff: {len(letter_boxes) - matching_predictions} {matching_predictions * 100 / len(letter_boxes):.2f}%')

	if display:
		print('\nTarget Text:\n', target_text)
		print('Pred Text:\n', pred_text)
		print('Remove Missmatch Text:\n', remove_mismatch_text)
		print('Remove Union Text:\n', remove_union_text)
		v = Visualizer(image[:, :, ::-1], scale=1.0)
		out = v.draw_instance_predictions(outputs["instances"].to("cpu"))
		plt.imshow(out.get_image()[:, :, ::-1])
		plt.show()

	return percent, rp_percent, rm_percent, ru_percent, nms_percent, nms_rp_percent


def print_stats(title, values):
	print(f'{title}: {values}')
	npa = np.array(values)
	mean, std = npa.mean(), npa.std()
	print(f'{title} min: {npa.min():.2f}, max: {npa.max():.2f}',
				f'mean: {mean:.2f} median: {np.median(npa):.2f}',
				'mode:', stats.mode(np.round(npa / 5) * 5).mode, f'std: {std:.2f}',
				f'90% ({mean - std * 1.645:.2f} - {mean + std * 1.645:.2f})')


def verify(predictor, fragments, preprocessor=None, non_labeled_only=False, refresh=False):
	scrolls = []
	percents = []
	rp_percents = []
	rm_percents = []
	ru_percents = []
	nms_percents = []
	nms_rp_percents = []

	counts = {}
	dataset = DSSLettersDataset(fragments=fragments)
	for _, _, metadata in dataset:
		count = counts.get(metadata['filename'])
		if count == None:
			counts[metadata['filename']] = 0
		counts[metadata['filename']] += 1

	for scroll in fragments:
		if not non_labeled_only or counts[scroll] < 500:
			if non_labeled_only and refresh:
				dataset = DSSLettersDataset(fragments=[scroll], overrides=[scroll])
				if len(dataset) > 500:
					continue
			result = evaluate(predictor, scroll, False, preprocessor=preprocessor)
			percents.append(result[0])
			rp_percents.append(result[1])
			rm_percents.append(result[2])
			ru_percents.append(result[3])
			nms_percents.append(result[4])
			nms_rp_percents.append(result[5])
		scrolls.append({
			"scroll": scroll,
			"nms_percent": result[4],
			"nms_rp_percent": result[5],
			"labeled": counts[scroll] > 500
		})

	for scroll in sorted(scrolls, key=lambda s: s["nms_rp_percent"]):
		print(
				f'{scroll["scroll"]} {scroll["nms_percent"]}%, {scroll["nms_rp_percent"]}% labeled: {scroll["labeled"]}')

	print_stats("Percents", percents)
	print_stats("RP Percents", rp_percents)
	print_stats("RM Percents", rm_percents)
	print_stats("RU Percents", ru_percents)
	print_stats("NMS Percents", nms_percents)
	print_stats("NMS RP Percents", nms_rp_percents)


def label_fragment(predictor, fragment, preprocessor=None):
	image, outputs, _, nms_letter_boxes = predict(
			predictor, fragment, preprocessor=preprocessor)

	v = Visualizer(image[:, :, ::-1], scale=1.0)
	out = v.draw_instance_predictions(outputs["instances"].to("cpu"))
	plt.imshow(out.get_image()[:, :, ::-1])
	plt.show()

	for letter_box in nms_letter_boxes:
		letter_box['value'] = letter_box['_predicted']

	# Get the list of existing letter boxes, if there are any.
	letterbox_url = LETTERBOX_BY_FRAGMENT_URL.format(fragment)
	print('Sending request: ', letterbox_url)
	row_ids = []
	letter_ids = []
	with request.urlopen(letterbox_url) as url:
		response = json.load(url)
		print('Response: ', response)
		letterboxes = response.get('items')
		if letterboxes is not None:
			for letterbox in letterboxes:
				if letterbox['type'] == 'Row':
					row_ids.append(letterbox['id'])
				elif letterbox['type'] == 'Letter':
					letter_ids.append(letterbox['id'])
		else:
			print(f'No existing letter boxes for {fragment}, continuing...')

	# Delete old letter boxes and create the new ones.
	send_json_req(LETTERBOX_BATCH_DELETE_URL, {'items': letter_ids})
	send_json_req(LETTERBOX_BATCH_CREATE_URL, {'items': nms_letter_boxes})


if __name__ == '__main__':
	parser = argparse.ArgumentParser()
	parser.add_argument('--preprocess', action='store_true')
	parser.add_argument('--iters', type=int, default=5000)
	parser.add_argument('--samples', action='store_true')
	parser.add_argument('--max_size', type=int, default=1920)
	parser.add_argument('--resume', action='store_true')
	parser.add_argument('--train', action='store_true')

	args = parser.parse_args()
	pp = preprocessor if args.preprocess else preprocessor

	if args.train or args.resume:
		train(args.iters, preprocessor=pp, resume=args.resume)

	cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = threshold
	# print(cfg)
	predictor = DefaultPredictor(cfg)

	# evaluate(predictor, "war-column-15", True, preprocessor=pp, override=True)
	verify(predictor, WAR_SET, preprocessor=pp, non_labeled_only=False)
	# label_fragment(predictor, "war-column-15", preprocessor=pp)

# No preprocessing
# [76.52, 80.76, 82.27, 83.61, 84.27, 83.63, 82.17, 80.79, 81.23, 78.92, 83.93, 84.94, 77.83]
# min: 76.52 max: 84.94 mean: 81.61 median: 82.17 mode: 80.0 std: 2.50 Z-Low: 76.70 Z-High: 86.51
# Gray, G-Blur, Crop
# [74.6, 76.62, 76.48, 75.74, 80.76, 77.14, 71.25, 79.83, 83.16, 78.98, 80.29, 83.32, 79.09]
# min: 71.25 max: 83.32 mean: 78.25 median: 78.98 mode: 75.0 std: 3.28 Z-Low: 71.83 Z-High: 84.67
# Gray, Crop
# [75.07, 76.74, 78.17, 77.04, 81.62, 80.52, 74.61, 79.49, 82.72, 78.68, 81.52, 83.27, 79.09]
# min: 74.61 max: 83.27 mean: 79.12 median: 79.09 mode: 80.0 std: 2.66 Z-Low: 73.90 Z-High: 84.34
# Crop
# [76.45, 76.8, 80.87, 77.47, 82.24, 81.23, 76.94, 80.07, 82.92, 79.47, 81.65, 83.75, 79.94]
# min: 76.45 max: 83.75 mean: 79.98 median: 80.07 mode: 80.0 std: 2.35 Z-Low: 75.39 Z-High: 84.58
# Gray, G-Blur
# [76.32, 77.98, 79.52, 80.94, 83.26, 83.52, 81.01, 78.62, 80.37, 78.61, 82.82, 83.65, 76.08]
# min: 76.08 max: 83.65 mean: 80.21 median: 80.37 mode: 80.0 std: 2.52 Z-Low: 75.26 Z-High: 85.16

'''War Scroll 1280x1920
No Space Percents: [0.0, 49.41, 45.65, 51.78, 51.15, 57.84, 59.3, 63.41, 61.56, 53.99, 59.68, 57.49, 60.84, 57.59, 53.9]
No Space Percents min: 0.00, max: 63.41 mean: 52.24 median: 57.49 mode: 60.0 std: 14.76 Z-Low: 23.32 Z-High: 81.16
'''
'''War Scroll 1920x1920
No Space Percents: [0.0, 52.13, 53.27, 55.89, 55.77, 63.71, 67.84, 53.76, 53.45, 57.98, 54.97, 41.96, 49.28, 50.49, 34.4]
No Space Percents min: 0.00, max: 67.84 mean: 49.66 median: 53.45 mode: 55.0 std: 15.30 Z-Low: 19.67 Z-High: 79.65
'''
'''War Scroll 1880x1920
No Space Percents: [0.0, 51.95, 55.03, 55.89, 56.5, 63.8, 68.11, 53.26, 54.0, 57.43, 53.8, 41.96, 49.17, 50.39, 36.49]
No Space Percents min: 0.00, max: 68.11 mean: 49.85 median: 53.80 mode: 55.0 std: 15.21 Z-Low: 20.05 Z-High: 79.66
'''

