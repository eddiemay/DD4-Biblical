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
from detectron2.data import MetadataCatalog
from detectron2.data.datasets import register_coco_instances
from detectron2.engine import DefaultPredictor, DefaultTrainer
from detectron2.utils.visualizer import Visualizer
from label_fragment import LETTERBOX_BY_FRAGMENT_URL, \
	LETTERBOX_BATCH_CREATE_URL, LETTERBOX_BATCH_DELETE_URL, send_json_req
from letterbox_utils import DSSLettersDataset, get_img_file_path, ISAIAH_SET, \
	parse_file_name, TRAINING_SET, VAL_SET, get_isa_text, get_y_at_x, is_in_row, process_image
from scipy import stats
from urllib import request
from utility import intersection_over_union

DATASET_BASE = 'detect_lines/dataset'
ANNOTATIONS = f'{DATASET_BASE}/annotations'
IMAGES_BASE = f'{DATASET_BASE}/images'
preprocessor = {"gray": True, "blur": "gaussian", "blur_size": 3}

config = "COCO-Keypoints/keypoint_rcnn_R_50_FPN_3x.yaml"
threshold = .7

cfg = get_cfg()
cfg.merge_from_file(model_zoo.get_config_file(config))
cfg.MODEL.WEIGHTS = model_zoo.get_checkpoint_url(config)
# cfg.MODEL.ROI_HEADS.POSITIVE_FRACTION = 0.5
cfg.MODEL.ROI_HEADS.NUM_CLASSES = 1
cfg.MODEL.ROI_KEYPOINT_HEAD.NUM_KEYPOINTS = 4

cfg.OUTPUT_DIR = "detect_lines/output"

cfg.MODEL.ANCHOR_GENERATOR.ASPECT_RATIOS = [[0.03, 0.05, 0.08]]
cfg.MODEL.ANCHOR_GENERATOR.SIZES = [[64, 128, 256, 512, 1024]]
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


def setup_data(preprocessor):
	if os.path.exists(IMAGES_BASE):
		shutil.rmtree(IMAGES_BASE)
	os.makedirs(f'{DATASET_BASE}/annotations', exist_ok=True)
	os.makedirs(f'{IMAGES_BASE}/train', exist_ok=True)
	os.makedirs(f'{IMAGES_BASE}/val', exist_ok=True)

	train_conf = {"images": [], "categories": [], "annotations": []}
	val_conf = {"images": [], "categories": [], "annotations": []}
	category = {
		"id": 0,
		"name": "row",
		"supercategory": "row",
		"keypoints": [
			"p1",
			"p2",
			"p3",
			"p4"
		],
		"skeleton": [
			[1,2],
			[2,3],
			[3,4]
		]
	}
	train_conf["categories"].append(category)
	val_conf["categories"].append(category)

	files = {}
	fragments = []
	fragments.extend(TRAINING_SET)
	fragments.extend(VAL_SET)
	dataset = DSSLettersDataset(fragments, filter=lambda lb:lb["type"] == 'Row')
	for _, _, row_box in dataset:
		filename = row_box['filename']
		image_id = parse_file_name(filename)[2]
		row_id = f'{filename}-{row_box["value"]}'
		if filename not in files:
			files[filename] = image_id
		conf = train_conf if filename not in VAL_SET else val_conf

		x, y = row_box['x1'], row_box['y1']
		width, height = row_box['x2'] - x, row_box['y2'] - y
		coords = ensure_four(row_box)["coords"]
		keypoints = [
			coords[0]["x"], coords[0]["y"], 2,
			coords[1]["x"], coords[1]["y"], 2,
			coords[2]["x"], coords[2]["y"], 2,
			coords[3]["x"], coords[3]["y"], 2,
		]
		conf["annotations"].append(
				{"id": row_id, "image_id": image_id, "category_id": 0, "num_keypoints": 4,
				 "keypoints": keypoints,
				 "bbox": [x, y, width, height], "area": width * height, "iscrowd": 0})

	for filename, id in files.items():
		conf, path = (train_conf, f'{IMAGES_BASE}/train') \
			if filename not in VAL_SET else (val_conf, f'{IMAGES_BASE}/val')
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

	# Tell Detectron2 about the keypoints
	for name in ["dss_train", "dss_val"]:
		MetadataCatalog.get(name).keypoint_names = [
			"p1",
			"p2",
			"p3",
			"p4",
		]

		# No left/right swapping for text rows
		MetadataCatalog.get(name).keypoint_flip_map = []

	cfg.DATASETS.TRAIN = ("dss_train",)
	cfg.DATASETS.TEST = ("dss_val",)

	cfg.SOLVER.IMS_PER_BATCH = 1
	cfg.SOLVER.BASE_LR = 0.000125
	# cfg.SOLVER.STEPS = (12000, 16000)
	# cfg.SOLVER.GAMMA = 0.1

	cfg.DATALOADER.NUM_WORKERS = 2

	cfg.SOLVER.MAX_ITER = iters  # 5000 or 20000 recommended

	trainer = DefaultTrainer(cfg)
	trainer.resume_or_load(resume=resume)
	trainer.train()


def predict(predictor, column, display=True, preprocessor=None):
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

	boxes = instances.pred_boxes.tensor.numpy()
	keypoints = instances.pred_keypoints.numpy()
	classes = instances.pred_classes.numpy()
	scores = instances.scores.numpy()

	fragment = f'isaiah-column-{column}'
	row_boxes = []
	for box, coords, cls, score in zip(boxes, keypoints, classes, scores):
		x1, y1, x2, y2 = map(int, box)
		row_boxes.append({
			"filename": fragment,
			"type": "Row",
			"x1": x1,
			"y1": y1 + y_offset,
			"x2": x2,
			"y2": y2 + y_offset,
			"coords": coords,
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
			if intersection_over_union(box, kept) > 0.225:
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


def evaluate(predictor, column, display=True, preprocessor=None,
		override=False):
	fragment = f'isaiah-column-{column}'
	outputs, letter_boxes, nms = predict(predictor, column, display, preprocessor)

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

	target_text = get_isa_text(column)
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

	ld = Levenshtein.distance(target_text, pred_text)
	percent = round((len(target_text) - ld) * 100 / len(target_text), 2)
	rp_ld = Levenshtein.distance(target_text, repred_text)
	rp_percent = round((len(target_text) - rp_ld) * 100 / len(target_text), 2)
	rm_ld = Levenshtein.distance(target_text, remove_mismatch_text)
	rm_percent = round((len(target_text) - rm_ld) * 100 / len(target_text), 2)
	ru_ld = Levenshtein.distance(target_text, remove_union_text)
	ru_percent = round((len(target_text) - ru_ld) * 100 / len(target_text), 2)
	nms_ld = Levenshtein.distance(target_text, nms_text)
	nms_percent = round((len(target_text) - nms_ld) * 100 / len(target_text), 2)
	nms_rp_ld = Levenshtein.distance(target_text, nms_rp_text)
	nms_rp_percent = round((len(target_text) - nms_rp_ld) * 100 / len(target_text), 2)
	no_space_text = ''.join(target_text.split())
	ld_no_space = Levenshtein.distance(no_space_text, ''.join(nms_rp_text.split()))
	no_space_percent = round((len(no_space_text) - ld_no_space) * 100 / len(no_space_text), 2)
	print(
			f'{fragment} Diff: {ld} {percent}%, Repredict Diff: {rp_ld} {rp_percent}%,',
			f'Remove Miss Diff: {rm_ld} {rm_percent}%, Remove Union Text: {ru_ld} {ru_percent}%,',
			f'NMS Diff: {nms_ld} {nms_percent}%, NMS RP Diff: {nms_rp_ld} {nms_rp_percent}%,',
			f'No Space Diff: {ld_no_space} {no_space_percent}%',
			f'Prediction Diff: {len(letter_boxes) - matching_predictions} {matching_predictions * 100 / len(letter_boxes):.2f}%')

	if display:
		print('\nTarget Text:\n', target_text)
		print('Pred Text:\n', pred_text)
		print('Remove Missmatch Text:\n', remove_mismatch_text)
		print('Remove Union Text:\n', remove_union_text)

	return percent, rp_percent, rm_percent, ru_percent, nms_percent, nms_rp_percent, no_space_percent


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


def verify(predictor, preprocessor=None, non_labeled_only=False, refresh=False):
	scrolls = []
	percents = []
	rp_percents = []
	rm_percents = []
	ru_percents = []
	nms_percents = []
	nms_rp_percents = []
	no_space_percents = []

	counts = {}
	dataset = DSSLettersDataset(fragments=ISAIAH_SET)
	for _, _, metadata in dataset:
		count = counts.get(metadata['filename'])
		if count == None:
			counts[metadata['filename']] = 0
		counts[metadata['filename']] += 1

	for c in range(54):
		c = c + 1
		scroll = f'isaiah-column-{c}'
		if not non_labeled_only or counts[scroll] < 500:
			if non_labeled_only and refresh:
				dataset = DSSLettersDataset(fragments=[scroll], overrides=[scroll])
				if len(dataset) > 500:
					continue
			result = evaluate(predictor, c, False, preprocessor=preprocessor)
			percents.append(result[0])
			rp_percents.append(result[1])
			rm_percents.append(result[2])
			ru_percents.append(result[3])
			nms_percents.append(result[4])
			nms_rp_percents.append(result[5])
			no_space_percents.append((result[6]))

			scrolls.append({
				"scroll": scroll,
				"nms_rp_percent": result[5],
				"no_space_percent": result[6],
				"labeled": counts[scroll] > 500
			})

	for scroll in sorted(scrolls, key=lambda s: s["no_space_percent"]):
		print(
			f'{scroll["scroll"]} {scroll["no_space_percent"]}% labeled: {scroll["labeled"]}')

	print(percents)
	percents = np.array(percents)
	mean, std = percents.mean(), percents.std()
	print('min:', percents.min(), 'max:', percents.max(),
				f'mean: {mean:.2f} median: {np.median(percents):.2f}',
				'mode:', stats.mode(np.round(percents / 5) * 5).mode, f'std: {std:.2f}',
				f'Z-Low: {mean - std * 1.96:.2f} Z-High: {mean + std * 1.96:.2f}')

	print(rp_percents)
	percents = np.array(rp_percents)
	mean, std = percents.mean(), percents.std()
	print('min:', percents.min(), 'max:', percents.max(),
				f'mean: {mean:.2f} median: {np.median(percents):.2f}',
				'mode:', stats.mode(np.round(percents / 5) * 5).mode, f'std: {std:.2f}',
				f'Z-Low: {mean - std * 1.96:.2f} Z-High: {mean + std * 1.96:.2f}')

	print(rm_percents)
	percents = np.array(rm_percents)
	mean, std = percents.mean(), percents.std()
	print('min:', percents.min(), 'max:', percents.max(),
				f'mean: {mean:.2f} median: {np.median(percents):.2f}',
				'mode:', stats.mode(np.round(percents / 5) * 5).mode, f'std: {std:.2f}',
				f'Z-Low: {mean - std * 1.96:.2f} Z-High: {mean + std * 1.96:.2f}')

	print(ru_percents)
	percents = np.array(ru_percents)
	mean, std = percents.mean(), percents.std()
	print('min:', percents.min(), 'max:', percents.max(),
				f'mean: {mean:.2f} median: {np.median(percents):.2f}',
				'mode:', stats.mode(np.round(percents / 5) * 5).mode, f'std: {std:.2f}',
				f'Z-Low: {mean - std * 1.96:.2f} Z-High: {mean + std * 1.96:.2f}')

	print(nms_percents)
	percents = np.array(nms_percents)
	mean, std = percents.mean(), percents.std()
	print('min:', percents.min(), 'max:', percents.max(),
				f'mean: {mean:.2f} median: {np.median(percents):.2f}',
				'mode:', stats.mode(np.round(percents / 5) * 5).mode, f'std: {std:.2f}',
				f'Z-Low: {mean - std * 1.96:.2f} Z-High: {mean + std * 1.96:.2f}')

	print(nms_rp_percents)
	percents = np.array(nms_rp_percents)
	mean, std = percents.mean(), percents.std()
	print('min:', percents.min(), 'max:', percents.max(),
				f'mean: {mean:.2f} median: {np.median(percents):.2f}',
				'mode:', stats.mode(np.round(percents / 5) * 5).mode, f'std: {std:.2f}',
				f'Z-Low: {mean - std * 1.96:.2f} Z-High: {mean + std * 1.96:.2f}')

	print(no_space_percents)
	percents = np.array(no_space_percents)
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
	parser.add_argument('--max_size', type=int, default=1280)
	parser.add_argument('--resume', action='store_true')
	parser.add_argument('--train', action='store_true')
	parser.add_argument('--batch_size_per_image', type=int, default=64)

	args = parser.parse_args()
	pp = preprocessor if args.preprocess else preprocessor
	cfg.MODEL.ROI_HEADS.BATCH_SIZE_PER_IMAGE = args.batch_size_per_image
	cfg.INPUT.MIN_SIZE_TRAIN = (768,)  # (1024,) or (1280,)
	cfg.INPUT.MAX_SIZE_TRAIN = args.max_size  # 2043 or 1600
	cfg.INPUT.MIN_SIZE_TEST = 768
	cfg.INPUT.MAX_SIZE_TEST = args.max_size

	if args.train or args.resume:
		train(args.iters, preprocessor=pp, resume=args.resume)

	cfg.MODEL.WEIGHTS = f'{cfg.OUTPUT_DIR}/model_final.pth'
	# cfg.MODEL.WEIGHTS = f'{cfg.OUTPUT_DIR}/model_final_32x84_1280_2043_10000.pth'
	cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = threshold
	cfg.MODEL.ROI_HEADS.NMS_THRESH_TEST = 0.3
	cfg.MODEL.RPN.PRE_NMS_TOPK_TRAIN = 1000
	cfg.MODEL.RPN.POST_NMS_TOPK_TRAIN = 200
	cfg.MODEL.RPN.PRE_NMS_TOPK_TEST = 500
	cfg.MODEL.RPN.POST_NMS_TOPK_TEST = 100
	cfg.TEST.DETECTIONS_PER_IMAGE = 60
	predictor = DefaultPredictor(cfg)

	predict(predictor, 11, True, pp)

	# evaluate(predictor, 1, False, preprocessor=pp, override=True)
	# evaluate(predictor, 28, False, preprocessor=pp, override=True)
	# evaluate(predictor, 54, False, preprocessor=pp, override=True)
	# verify(predictor, preprocessor=pp, non_labeled_only=False)
			# preprocessor={"bf": 7, "blur": "median", "blur_size": 3, "threshold": 135, "threshold_type": 2})
	# evaluate(predictor, 23, True, preprocessor=pp, override=True)
	# evaluate(predictor, 43, True, preprocessor=pp, override=True)
	# label_fragment(predictor, 30, preprocessor=pp)
