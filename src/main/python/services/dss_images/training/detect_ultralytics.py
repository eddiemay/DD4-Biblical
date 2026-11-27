import argparse
import cv2
import Levenshtein
import numpy as np
import os
import shutil
import time
from letterbox_utils import DSSLettersDataset, get_img_file_path, LABEL_LOOKUP, \
	SINGLE_LETTERS_ONLY, TRAINING_SET, VAL_SET, TEST_SET, get_frag_text, is_in_row, \
	process_image, intersection_over_union, COMMUNITY_SET, ISAIAH_SET, WAR_SET
from scipy import stats
from ultralytics import YOLO

BEST_MODEL =\
	'/opt/homebrew/runs/detect/runs/train/deadsea_hebrew_v1/weights/best.pt'
preprocessor = {"gray": True, "blur": "gaussian", "blur_size": 3}


def print_stats(title, values):
	print(f'{title}: {values}')
	npa = np.array(values)
	mean, std = npa.mean(), npa.std()
	print(f'{title} min: {npa.min():.2f}, max: {npa.max():.2f}',
				f'mean: {mean:.2f} median: {np.median(npa):.2f}',
				'mode:', stats.mode(np.round(npa / 5) * 5).mode, f'std: {std:.2f}',
				f'90% ({mean - std * 1.645:.2f} - {mean + std * 1.645:.2f})')


def resize(filename, img, start_x, end_x, target_x, max_x, start_y, end_y, target_y, max_y):
	y_buffer = max(target_y - (end_y - start_y), 0)
	x_buffer = max(target_x - (end_x - start_x), 0)
	y_start, y_end = int(max(start_y - y_buffer / 2, 0)), int(min(end_y + y_buffer / 2, max_y))
	x_start, x_end = int(max(start_x - x_buffer / 2, 0)), int(min(end_x + x_buffer / 2, max_x))
	letter_h, letter_w = y_end - y_start, x_end - x_start
	print(f'{filename} Image h,w: ({max_y}, {max_x}), Letter h,w: ({end_y - start_y}, {end_x - start_x}), Result: ({letter_h}, {letter_w})')
	return img[y_start:y_end,x_start:x_end], x_start, y_start


def setup_data_yolo(preprocessor):
	"""
	Converts Detectron2/COCO-style scroll dataset to YOLO format.
	Preserves your crop/resize logic but outputs YOLO-compatible images + annotations.
	"""
	# YOLO expects this exact directory structure:
	BASE_DIR = 'dataset_ultralytics'
	IMG_DIR = os.path.join(BASE_DIR, 'images')
	LAB_DIR = os.path.join(BASE_DIR, 'labels')

	# Create YOLO directory structure
	for split in ['train', 'val']:
		img_dir, label_dir = os.path.join(IMG_DIR, split), os.path.join(LAB_DIR, split)
		if os.path.exists(img_dir): shutil.rmtree(img_dir)
		if os.path.exists(label_dir): shutil.rmtree(label_dir)
		os.makedirs(img_dir, exist_ok=True)
		os.makedirs(label_dir, exist_ok=True)

	# Group annotations by filename (same logic as your original)
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
	heights, widths = [], []

	for filename, file in files.items():
		is_val = filename in VAL_SET
		split = 'val' if is_val else 'train'
		img_path = os.path.join(IMG_DIR, split)
		lbl_path = os.path.join(LAB_DIR, split)

		file_path = get_img_file_path(filename, 9)
		img = process_image(cv2.imread(file_path), preprocessor)[0]
		h, w = img.shape[:2]
		heights.append(h)
		widths.append(w)

		# Save resized image to YOLO structure
		cv2.imwrite(f'{img_path}/{filename}.jpg', img)

		# Generate YOLO .txt annotation
		txt_path = os.path.join(lbl_path, f'{filename}.txt')
		with open(txt_path, 'w') as f:
			for letter_box in file["letter_boxes"]:
				x, y = letter_box['x1'], letter_box['y1']
				width, height = letter_box['x2'] - x, letter_box['y2'] - y

				# Apply crop/resize offset from your original code
				x_adj = x
				y_adj = y

				# Safety clamp to prevent negative or out-of-bounds values (breaks YOLO training)
				x_adj = max(0, x_adj)
				y_adj = max(0, y_adj)
				width = max(0, width)
				height = max(0, height)

				# YOLO format: <class_id> <x_center> <y_center> <width> <height> (normalized 0-1)
				x_center = (x_adj + width / 2) / w
				y_center = (y_adj + height / 2) / h
				w_norm = width / w
				h_norm = height / h

				# Clamp normalized values to [0, 1]
				x_center = max(0.0, min(1.0, x_center))
				y_center = max(0.0, min(1.0, y_center))
				w_norm = max(0.0, min(1.0, w_norm))
				h_norm = max(0.0, min(1.0, h_norm))

				class_id = letter_box["label"]  # Assumes 0-indexed, matching data.yaml
				f.write(f"{class_id} {x_center:.6f} {y_center:.6f} {w_norm:.6f} {h_norm:.6f}\n")

		letter_id += 1

	print_stats("Heights", heights)
	print_stats("Widths", widths)
	print(f"✅ YOLO dataset ready at: {BASE_DIR}/")


def train(epochs:int=250, train_size:int=1024, batch_size:int=9, patience:int=50):
	# Load pre-trained YOLOv8 model (m for balance of speed/accuracy)
	model = YOLO('yolov8m.pt')

	print("🚀 Starting training for Dead Sea Scrolls Hebrew letter detection...")
	setup_data_yolo(None)
	# os.makedirs('runs/train/deadsea_hebrew_v1/weights', exist_ok=True)

	# Start training with domain-optimized hyperparameters
	results = model.train(
			data='dataset_ultralytics/data.yaml',           # Path to your dataset configuration file
			epochs=epochs,              # Increased epochs for fine-grained letter classification
			imgsz=train_size,           # High resolution to capture small/partial scroll fragments
			batch=batch_size,           # Batch size (adjust based on GPU VRAM; use 4 if OOM)
			device='mps',               # GPU device ID (use 'cpu' for testing, or '0,1' for multi-GPU)
			workers=batch_size*2,       # Data loading workers (match CPU cores for faster loading)
			project='runs/train',       # Output directory for checkpoints and logs
			name='deadsea_hebrew_v1',   # Run name for tracking experiments
			exist_ok=True,              # Overwrite existing runs with the same name
			pretrained=True,            # Load ImageNet pre-trained weights for transfer learning
			verbose=True,               # Print detailed training progress and metrics
			seed=42,                    # Fix random seed for reproducibility
			deterministic=False,        # Allow non-deterministic ops for speed (set True for strict reproducibility)
			single_cls=False,           # Use multi-class (set True if you want to detect "any letter" as one class)
			rect=False,                 # Rectangular training (saves VRAM, can speed up training)
			fraction=1.0,               # Fraction of dataset to use (1.0 = full dataset)
			save=True,                  # Save training checkpoints and final weights
			save_period=-1,             # Save checkpoint every N epochs (-1 = disable periodic saves)
			val=True,                   # Run validation after each epoch
			plots=True,                 # Generate confusion matrix, training curves, and result plots
			cache=True,                 # Cache images in RAM for faster training (use 'ram' if <10GB dataset)
			amp=True,                   # Automatic Mixed Precision (speeds up training, reduces VRAM usage)
			close_mosaic=15,            # Disable mosaic augmentation in last N epochs (critical for small/partial letters)
			mosaic=1.0,                 # Mosaic augmentation probability (combines 4 images, good for context)
			mixup=0.05,                 # Mixup augmentation probability (blends 2 images, handles degradation)
			degrees=10,                 # Random rotation range (handles scroll fragment orientation)
			translate=0.1,              # Random translation range (10% image size)
			scale=0.1,                  # Random scaling range (0.1 = 10% zoom in/out)
			shear=0.0,                  # Random shear range (0.0 = disabled)
			perspective=0.0,            # Random perspective transform (0.0 = disabled)
			flipud=0.0,                 # Vertical flip probability (0.5 = 50%)
			fliplr=0.0,                 # Horizontal flip probability (0.5 = 50%)
			bgr=0.0,                    # Random BGR color channel swap (0.0 = disabled)
			hsv_h=0.015,                # Random hue augmentation (handles faded ink/papyrus tones)
			hsv_s=0.7,                  # Random saturation augmentation
			hsv_v=0.4,                  # Random value/brightness augmentation
			augment=True,               # Enable all augmentations (set False for strict inference-only prep)
			optimizer='AdamW',          # Optimizer (AdamW often converges better for fine-tuning)
			lr0=0.001,                  # Initial learning rate (lower for transfer learning stability)
			weight_decay=0.0005,        # L2 regularization (prevents overfitting on small datasets)
			momentum=0.937,             # Momentum for SGD/AdamW (standard YOLO value)
			cos_lr=False,               # Cosine learning rate scheduler (False = linear decay)
			patience=patience,          # Early stopping patience (epochs without improvement before stopping)
	)

	# Note: 'fl_gamma' was removed from the public Ultralytics API in v8.0+.
	# Focal loss is now handled internally by the loss function. Use 'label_smooth'
	# and 'close_mosaic' for similar stability, or pass 'class_weights' in data.yaml for imbalance.

	print(f"✅ Training complete. Best weights saved to: {results.save_dir}/weights/best.pt")
	return results


def sort_detections_dsc(detections, line_gap_factor=0.5):
	"""Sort detections top-to-bottom, then right-to-left within each line."""
	if not detections:
		return detections

	# Adaptive threshold based on average box height (prevents over/under-merging)
	avg_height = sum((d["bbox"][3] - d["bbox"][1]) for d in detections) / len(detections)
	line_threshold = avg_height * line_gap_factor

	# 1. Group into lines by y-center
	detections.sort(key=lambda d: (d["bbox"][1] + d["bbox"][3]) / 2)

	lines = []
	current_line = [detections[0]]

	for d in detections[1:]:
		prev_d = current_line[-1]
		prev_y_center = (prev_d["bbox"][1] + prev_d["bbox"][3]) / 2
		curr_y_center = (d["bbox"][1] + d["bbox"][3]) / 2

		if abs(curr_y_center - prev_y_center) <= line_threshold:
			current_line.append(d)
		else:
			lines.append(current_line)
			current_line = [d]
	lines.append(current_line)

	# 2. Sort each line right-to-left (descending x1)
	final_order = []
	for line in lines:
		line.sort(key=lambda d: d["bbox"][0], reverse=True)
		final_order.extend(line)

	return final_order


def predict(model, fragment, preprocessor=None):
	start_time = time.time()
	img_file = get_img_file_path(fragment, 9)
	image = process_image(cv2.imread(img_file), preprocessor)[0]
	# if len(image.shape) == 2:
		# image = cv2.cvtColor(image, cv2.COLOR_GRAY2BGR)
	results = model(image, conf=0.60, iou=0.2, max_det=2000)
	print(f'Prediction took {time.time() - start_time:.1f} seconds')

	letter_boxes = []
	for box in results[0].boxes:
		x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
		letter_boxes.append({
			"filename": fragment,
			"type": "Letter",
			"x1": x1,
			"y1": y1,
			"x2": x2,
			"y2": y2,
			"value": LABEL_LOOKUP[int(box.cls[0])],
			"_score": float(box.conf[0])
		})

	# from predict_letters import predict_letters
	# predict_letters(letter_boxes)
	nms = []
	for box in sorted(letter_boxes, key=lambda b:b["_score"], reverse=True):
		keep = True
		for kept in nms:
			if intersection_over_union(box, kept) > 0.25:
				keep = False
				break
		if keep:
			nms.append(box)

	return image, results, letter_boxes, nms


def evaluate(model, fragment:str, display=True, preprocessor=None, override=False):
	image, results, letter_boxes, nms = predict(model, fragment, preprocessor)

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
		if letter_box['value'] == letter_box.get('_predicted'):
			matching_predictions += 1
		for row_box in row_boxes:
			if is_in_row(row_box, letter_box):
				row_lbs = row_box['_letterBoxes']
				if len(row_lbs) > 0 and (row_lbs[-1]['x1'] - letter_box['x2'] >= 5):
					row_box['_text'] += ' '
					row_box['_predict_text'] += ' '
				row_box['_text'] += letter_box['value']
				row_box['_predict_text'] += letter_box.get('_predicted', '')
				if letter_box['value'] == letter_box.get('_predicted'):
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

		# Extract detections
		detections = []
		if len(results) > 0 and results[0].boxes is not None:
			for box in results[0].boxes:
				detections.append({
					"class_id": int(box.cls[0]),
					"class_name": results[0].names[int(box.cls[0])],
					"confidence": float(box.conf[0]),
					"bbox": box.xyxy[0].tolist()  # [x1, y1, x2, y2]
				})

		# Sort top-to-bottom, then right-to-left
		detections = sort_detections_dsc(detections, line_gap_factor=0.4)  # 0.3-0.6 recommended for scrolls
		results[0].show()
		# for det in detections:
			# print(det)
		print(f"✅ Detected {len(detections)} letters")

	return percent, rp_percent, rm_percent, ru_percent, nms_percent, nms_rp_percent


def verify(model, fragments, preprocessor=None, non_labeled_only=False, refresh=False):
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
			result = evaluate(model, scroll, False, preprocessor=preprocessor)
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
		print(f'{scroll["scroll"]} {scroll["nms_percent"]}%, '
					f'{scroll["nms_rp_percent"]}% labeled: {scroll["labeled"]}')

	print_stats("Percents", percents)
	print_stats("RP Percents", rp_percents)
	print_stats("RM Percents", rm_percents)
	print_stats("RU Percents", ru_percents)
	print_stats("NMS Percents", nms_percents)
	print_stats("NMS RP Percents", nms_rp_percents)


if __name__ == '__main__':
	parser = argparse.ArgumentParser()
	subparsers = parser.add_subparsers(dest='command', required=True)

	# train command
	train_parser = subparsers.add_parser('train')
	train_parser.add_argument('--epochs', type=int, default=2048)
	train_parser.add_argument('--train_size', type=int, default=1024)
	train_parser.add_argument('--patience', type=int, default=0)
	train_parser.add_argument('--batch_size', type=int, default=8)

	# evaluate command
	evaluate_parser = subparsers.add_parser('evaluate')
	evaluate_parser.add_argument('fragment')
	evaluate_parser.add_argument('--model', type=str, default=BEST_MODEL)
	evaluate_parser.add_argument('--display', action='store_true')
	evaluate_parser.add_argument('--override', action='store_true')

	# verify command
	verify_parser = subparsers.add_parser('verify')
	verify_parser.add_argument("set")
	verify_parser.add_argument('--model', type=str, default=BEST_MODEL)
	verify_parser.add_argument('--non_labeled_only', action='store_true')

	parser.add_argument('--preprocess', action='store_true')

	args = parser.parse_args()
	pp = None if args.preprocess else preprocessor

	if args.command == 'train' or args.command == 'resume':
		train(args.epochs, args.train_size, args.batch_size, args.patience)

	if args.command == 'evaluate':
		model = YOLO(args.model)
		evaluate(model, args.fragment, args.display, args.override)

	if args.command == 'verify':
		model = YOLO(args.model)
		set = {'train': TRAINING_SET, 'val': VAL_SET, 'test': TEST_SET,
					 'community': COMMUNITY_SET, 'isa': ISAIAH_SET, 'war': WAR_SET}[args.set]
		verify(model, set, None, args.non_labeled_only)
	# yolo export model=runs/train/deadsea_hebrew_v1/weights/best.pt format=onnx int8