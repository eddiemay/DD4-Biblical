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
	SINGLE_LETTERS_ONLY, TRAINING_SET, VAL_SET, TEST_SET, get_frag_text, is_in_row, \
	process_image, intersection_over_union, COMMUNITY_SET, WAR_SET
from scipy import stats
from train_by_labels import process
from urllib import request

ANNO_IDS = {}
DATASET_BASE = 'detect_letters/dataset'
ANNOTATIONS = f'{DATASET_BASE}/annotations'
IMAGES_BASE = f'{DATASET_BASE}/images'
preprocessor = {"gray": True, "blur": "gaussian", "blur_size": 3}

# config = "COCO-Detection/faster_rcnn_R_101_FPN_3x.yaml"
# 👉 Much higher accuracy, but slower
config = "COCO-Detection/faster_rcnn_X_101_32x8d_FPN_3x.yaml"
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
					if row_box.get('_prev_letter') != None and row_box['_prev_letter']['x1'] - letter_box['x2'] >= 5:
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

	target_text = get_frag_text(fragment)
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
	subparsers = parser.add_subparsers(dest='command', required=True)

	# train command
	train_parser = subparsers.add_parser('train')
	train_parser.add_argument('--iters', type=int, default=7500)
	train_parser.add_argument('--samples', action='store_true')

	# resume command
	resume_parser = subparsers.add_parser('resume')
	resume_parser.add_argument('--iters', type=int, default=7500)

	# evaluate command
	evaluate_parser = subparsers.add_parser('evaluate')
	evaluate_parser.add_argument('fragment')
	evaluate_parser.add_argument('--display', action='store_true')
	evaluate_parser.add_argument('--override', action='store_true')

	# verify command
	verify_parser = subparsers.add_parser('verify')
	verify_parser.add_argument("set")
	verify_parser.add_argument('--non_labeled_only', action='store_true')

	# label command
	label_parser = subparsers.add_parser('label')
	label_parser.add_argument('fragment')

	parser.add_argument('--preprocess', action='store_true')

	args = parser.parse_args()
	pp = preprocessor if args.preprocess else preprocessor

	if args.command == 'train' or args.command == 'resume':
		train(args.iters, preprocessor=pp, resume=args.command == 'resume')

	cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = threshold
	# print(cfg)
	predictor = DefaultPredictor(cfg)

	if args.command == 'evaluate':
		evaluate(predictor, args.fragment, args.display, pp, args.override)

	if args.command == 'verify':
		set = {'training': TRAINING_SET, 'val': VAL_SET, 'test': TEST_SET,
					 'community': COMMUNITY_SET, 'war': WAR_SET}[args.set]
		verify(predictor, set, pp, args.non_labeled_only)

	if args.command == 'label':
		label_fragment(predictor, args.fragment, preprocessor=pp)
