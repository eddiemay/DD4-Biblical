import argparse
import cv2
import json
import matplotlib.pyplot as plt
import numpy as np
import os
import shutil
import time
from detectron2 import model_zoo
from detectron2.config import get_cfg
from detectron2.data import MetadataCatalog
from detectron2.data.datasets import register_coco_instances
from detectron2.engine import DefaultPredictor, DefaultTrainer
from detectron2.evaluation import COCOEvaluator
from detectron2.utils.visualizer import Visualizer
from label_fragment import LETTERBOX_BY_FRAGMENT_URL, \
	LETTERBOX_BATCH_CREATE_URL, LETTERBOX_BATCH_DELETE_URL, send_json_req
from letterbox_utils import DSSLettersDataset, get_img_file_path, VAL_SET, \
	parse_file_name, TRAINING_SET, TEST_SET, get_y_at_x, is_in_row, process_image
from scipy import stats
from urllib import request
from utility import intersection_over_union

DATASET_BASE = 'detect_lines/dataset'
ANNOTATIONS = f'{DATASET_BASE}/annotations'
IMAGES_BASE = f'{DATASET_BASE}/images'
preprocessor = {"gray": True, "blur": "gaussian", "blur_size": 3}

config = "COCO-InstanceSegmentation/mask_rcnn_R_50_FPN_3x.yaml"
threshold = .7

cfg = get_cfg()
cfg.merge_from_file(model_zoo.get_config_file(config))
cfg.MODEL.WEIGHTS = model_zoo.get_checkpoint_url(config)
# cfg.MODEL.ROI_HEADS.POSITIVE_FRACTION = 0.5
cfg.MODEL.ROI_HEADS.NUM_CLASSES = 1

cfg.OUTPUT_DIR = "detect_lines/output"

cfg.MODEL.ANCHOR_GENERATOR.ASPECT_RATIOS = [[0.04, 0.08, 0.16, 0.20]]
cfg.MODEL.ANCHOR_GENERATOR.SIZES = [[32, 64, 128, 256]]
cfg.MODEL.DEVICE = 'cpu'


def ensure_four(row_box):
	coords = row_box["coords"]
	if len(coords) == 3:
		# Insert a new coord between the first and second
		mid_x = (coords[0]["x"] + coords[1]["x"]) / 2
		coords.insert(1, {"x": mid_x, "y": get_y_at_x(row_box, mid_x)})

	if len(coords) == 2:
		# Insert two coords between the beginning and end.
		quarter = (coords[1]["x"] - coords[0]["x"]) / 4
		x = coords[0]["x"] + quarter
		coords.insert(1, {"x": x, "y": get_y_at_x(row_box, x)})
		x = coords[1]["x"] + quarter
		coords.insert(2, {"x": x, "y": get_y_at_x(row_box, x)})

	return row_box


def set_rows(row_boxes, letter_boxes):
	for letter_box in sorted(letter_boxes, key=lambda x:x['x2'], reverse=True):
		for row_box in row_boxes:
			if is_in_row(row_box, letter_box):
				row_box["_letterBoxes"].append(letter_box)
				if row_box.get("_minYBox") is None or letter_box["y1"] < row_box.get("_minYBox")["y1"]:
					row_box['_minYBox'] = letter_box
				break


def get_segmentation(row_box):
	if row_box.get("segmentation") is not None:
		segmentation = row_box["segmentation"]
	else:
		segmentation = []
		segmentation.extend(row_box["coords"])

		first_box = row_box["_letterBoxes"][0]
		segmentation.append({"x": first_box["x2"] + 4, "y": first_box["y1"] - 4})

		min_y_box = row_box["_minYBox"]
		segmentation.append({"x": min_y_box["x2"], "y": min_y_box["y1"] - 4})

		last_box = row_box["_letterBoxes"][-1]
		segmentation.append({"x": last_box["x1"] - 4, "y": last_box["y1"] - 4})

	x1, x2 = min(segmentation[0]["x"], segmentation[-1]["x"]), 0
	y1, y2 = segmentation[-1]["y"], segmentation[0]["y"]
	segs = []
	for segment in segmentation:
		if segment["x"] > x2:
			x2 = segment["x"]
		if segment["y"] < y1:
			y1 = segment["y"]
		if segment["y"] > y2:
			y2 = segment["y"]
		segs.extend([segment["x"], segment["y"]])

	return segs, [x1, y1, x2 - x1, y2 - y1]


def setup_data(preprocessor):
	if os.path.exists(IMAGES_BASE):
		shutil.rmtree(IMAGES_BASE)
	os.makedirs(f'{DATASET_BASE}/annotations', exist_ok=True)
	os.makedirs(f'{IMAGES_BASE}/train', exist_ok=True)
	os.makedirs(f'{IMAGES_BASE}/val', exist_ok=True)

	train_conf = {"images": [], "categories": [], "annotations": []}
	val_conf = {"images": [], "categories": [], "annotations": []}
	category = {"id": 0, "name": "row", "supercategory": "text"}
	train_conf["categories"].append(category)
	val_conf["categories"].append(category)

	files = {}
	fragments = []
	fragments.extend(TRAINING_SET)
	fragments.extend(VAL_SET)
	dataset = DSSLettersDataset(fragments)
	for _, _, letter_box in dataset:
		filename = letter_box['filename']
		if filename not in files:
			files[filename] = {"rows": [], "letters": []}
		if letter_box["type"] == 'Row':
			letter_box["_letterBoxes"] = []
			files[filename]["rows"].append(letter_box)
		else:
			files[filename]["letters"].append(letter_box)

	for filename, file in files.items():
		conf = train_conf if filename not in VAL_SET else val_conf
		image_id = parse_file_name(filename)[2]
		row_boxes = file["rows"]
		set_rows(file["rows"], file["letters"])

		pts = []
		for row_box in row_boxes:
			row_id = f'{filename}-{row_box["value"]}'
			segmentation, bbox = get_segmentation(row_box)
			conf["annotations"].append(
					{"id": row_id, "image_id": image_id, "category_id": 0,
					 "segmentation": [segmentation],
					 "bbox": bbox, "area": bbox[2] * bbox[3], "iscrowd": 0})
			pts.append(np.array(segmentation, dtype=np.int32).reshape((-1, 2)))

		path = f'{IMAGES_BASE}/train' if conf == train_conf else f'{IMAGES_BASE}/val'
		file_path = get_img_file_path(filename, 9)
		img = process_image(cv2.imread(file_path), preprocessor)[0]
		h, w = img.shape[:2]
		conf["images"].append(
				{"id": image_id, "file_name": filename + '.jpg', "height": h, "width": w})
		os.makedirs(path, exist_ok=True)
		cv2.imwrite(f'{path}/{filename}.jpg', img)

		# preview = img.copy()
		# cv2.polylines(preview, pts=pts, isClosed=True, color=(0, 255, 0), thickness=2)
		overlay = img.copy()

		# cv2.fillPoly(overlay,pts,(0, 255, 0))
		# preview = cv2.addWeighted(img, 0.7, overlay, 0.3, 0)
		# cv2.imshow("Segmentation", preview)
		# cv2.waitKey(0)

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


def train(iters, preprocessor, resume=False):
	setup_data(preprocessor)

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

	MetadataCatalog.get("dss_val").set(thing_classes=["row"])
	MetadataCatalog.get("dss_train").set(thing_classes=["row"])

	cfg.DATASETS.TRAIN = ("dss_train",)
	cfg.DATASETS.TEST = ("dss_val",)

	cfg.SOLVER.IMS_PER_BATCH = 1
	cfg.SOLVER.BASE_LR = 0.000250
	cfg.SOLVER.STEPS = (8000, 10000)
	cfg.SOLVER.GAMMA = 0.1

	cfg.DATALOADER.NUM_WORKERS = 1

	cfg.SOLVER.MAX_ITER = iters  # 5000 or 20000 recommended

	print('Training with conf:', cfg)
	trainer = Trainer(cfg)
	trainer.resume_or_load(resume=resume)
	trainer.train()


def predict(predictor, fragment, display=True, preprocessor=None):
	column = fragment if isinstance(fragment, int) else parse_file_name(fragment)[2]
	start_time = time.time()
	img_file = f'../images/isaiah/columns/column_9_{column}.jpg'
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

	# print(instances)
	boxes = instances.pred_boxes.tensor.numpy()
	# keypoints = instances.pred_keypoints.numpy()
	classes = instances.pred_classes.numpy()
	scores = instances.scores.numpy()

	fragment = f'isaiah-column-{column}'
	row_boxes = []
	for box, cls, score in zip(boxes, classes, scores):
		x1, y1, x2, y2 = map(int, box)
		row_boxes.append({
			"filename": fragment,
			"type": "Row",
			"x1": x1,
			"y1": y1 + y_offset,
			"x2": x2,
			"y2": y2 + y_offset,
			# "coords": coords,
			"_score": float(score)
		})

	row = 1
	for box in sorted(row_boxes, key=lambda b: b["y1"]):
		box["value"] = row
		row += 1

	nms = []
	for box in sorted(row_boxes, key=lambda b: b["_score"], reverse=True):
		keep = True
		for kept in nms:
			if intersection_over_union(box, kept) > 0.5:
				keep = False
				break
		if keep:
			nms.append(box)

	if display:
		v = Visualizer(image[:, :, ::-1], scale=1.0)
		out = v.draw_instance_predictions(outputs["instances"].to("cpu"))
		plt.imshow(out.get_image()[:, :, ::-1])
		plt.show()

	return outputs, row_boxes, nms


def evaluate(predictor, fragment, display=True, preprocessor=None, override=False):
	outputs, pred_boxes, pred_nms = predict(predictor, fragment, display, preprocessor)

	dataset = DSSLettersDataset(
			fragments=[fragment], overrides=[fragment] if override else [])
	row_boxes = []
	letter_boxes = []
	for _, _, box in dataset:
		if box["type"] == 'Row':
			box["_letterBoxes"] = []
			row_boxes.append(box)
		else:
			letter_boxes.append(box)

	set_rows(row_boxes, letter_boxes)

	fn, fp, tp = 0, 0, 0
	for row_box in row_boxes:
		max_iou, max_pred_box = 0, None
		for pred_box in pred_boxes:
			iou = intersection_over_union(row_box, pred_box)
			if iou > max_iou:
				max_iou, max_pred_box = iou, pred_box
		if max_iou > .5:
			tp += 1
			row_box["_taken"], max_pred_box["_taken"] = True, True
		else:
			fn += 1

	for pred_box in pred_boxes:
		if pred_box.get("_taken") is True:
			continue
		max_iou, max_row_box = 0, None
		for row_box in row_boxes:
			iou = intersection_over_union(pred_box, row_box)
			if iou > max_iou:
				max_iou, max_row_box = iou, row_box
		if max_iou > .5:
			tp += 1
			pred_box["_taken"], max_row_box["_taken"] = True, True
		else:
			fp += 1

	precision, recall = tp / (tp + fp), tp / (tp + fn)
	f1_score = 2 * precision * recall / (precision + recall + 0.00001)

	if display:
		print(f'FP {fp}, FN {fn}, TP {tp}, Precision {precision}, Recall {recall}, F1 Score {f1_score}')

	return fp, fn, tp, precision, recall, f1_score


def verify(predictor, fragments, preprocessor=None, refresh=False):
	scrolls = []
	fps, fns, tps, precisions, recalls, f1_scores = [], [], [], [], [], []
	for fragment in fragments:
		result = evaluate(predictor, fragment, False, preprocessor, refresh)
		fps.append(result[0])
		fns.append(result[1])
		tps.append(result[2])
		precisions.append(result[3])
		recalls.append(result[4])
		f1_scores.append(result[5])

		scrolls.append({
			"scroll": fragment,
			"precision": result[3],
			"recall": result[4],
			"f1_score": result[5],
		})

	for scroll in sorted(scrolls, key=lambda s:s["f1_score"]):
		print(f'{scroll["scroll"]} precision:{scroll["precision"]} '
					f'recall:{scroll["recall"]:.3f} f1_score: {scroll["f1_score"]:.3f}')

	print()
	print(precisions)
	percents = np.array(precisions) * 100
	mean, std = percents.mean(), percents.std()
	print('Precision min:', percents.min(), 'max:', percents.max(),
				f'mean: {mean:.2f} median: {np.median(percents):.2f}',
				'mode:', stats.mode(np.round(percents / 5) * 5).mode, f'std: {std:.2f}',
				f'Z-Low: {mean - std * 1.96:.2f} Z-High: {mean + std * 1.96:.2f}')

	print(recalls)
	percents = np.array(recalls) * 100
	mean, std = percents.mean(), percents.std()
	print('Recall min:', percents.min(), 'max:', percents.max(),
				f'mean: {mean:.2f} median: {np.median(percents):.2f}',
				'mode:', stats.mode(np.round(percents / 5) * 5).mode, f'std: {std:.2f}',
				f'Z-Low: {mean - std * 1.96:.2f} Z-High: {mean + std * 1.96:.2f}')

	print(f1_scores)
	percents = np.array(f1_scores) * 100
	mean, std = percents.mean(), percents.std()
	print('F1 Score min:', percents.min(), 'max:', percents.max(),
				f'mean: {mean:.2f} median: {np.median(percents):.2f}',
				'mode:', stats.mode(np.round(percents / 5) * 5).mode, f'std: {std:.2f}',
				f'Z-Low: {mean - std * 1.96:.2f} Z-High: {mean + std * 1.96:.2f}')


def label_fragment(predictor, column, preprocessor=None):
	fragment = f'isaiah-column-{column}'
	_, _, nms_letter_boxes = predict(predictor, column, preprocessor=preprocessor)

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
	force_train = False
	parser = argparse.ArgumentParser()
	parser.add_argument('--preprocess', action='store_true')
	parser.add_argument('--iters', type=int, default=12000)
	parser.add_argument('--samples', action='store_true')
	parser.add_argument('--max_size', type=int, default=2270)
	parser.add_argument('--resume', action='store_true')
	parser.add_argument('--train', action='store_true')
	parser.add_argument('--batch_size_per_image', type=int, default=256)

	args = parser.parse_args()
	pp = preprocessor if args.preprocess else preprocessor
	cfg.MODEL.ROI_HEADS.BATCH_SIZE_PER_IMAGE = args.batch_size_per_image
	cfg.INPUT.MIN_SIZE_TRAIN = (1280,)  # (1024,) or (1280,)
	cfg.INPUT.MAX_SIZE_TRAIN = args.max_size  # 2043 or 1600
	cfg.INPUT.MIN_SIZE_TEST = 1280
	cfg.INPUT.MAX_SIZE_TEST = args.max_size
	cfg.INPUT.RANDOM_FLIP = "none"
	cfg.TEST.EVAL_PERIOD = 500
	cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = threshold
	cfg.MODEL.RPN.PRE_NMS_TOPK_TRAIN = 1000
	cfg.MODEL.RPN.POST_NMS_TOPK_TRAIN = 500
	cfg.MODEL.RPN.PRE_NMS_TOPK_TEST = 300
	cfg.MODEL.RPN.POST_NMS_TOPK_TEST = 100
	cfg.TEST.DETECTIONS_PER_IMAGE = 60

	if args.train or args.resume or force_train:
		train(args.iters, preprocessor=pp, resume=args.resume)

	cfg.MODEL.WEIGHTS = f'{cfg.OUTPUT_DIR}/model_final.pth'
	cfg.MODEL.ROI_HEADS.NMS_THRESH_TEST = 0.3
	predictor = DefaultPredictor(cfg)

	verify(predictor, TRAINING_SET, preprocessor=pp)
	verify(predictor, VAL_SET, preprocessor=pp)
	verify(predictor, TEST_SET, preprocessor=pp)
