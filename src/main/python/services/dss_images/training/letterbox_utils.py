import cv2
import json
import os
import numpy as np
import torchvision.transforms as transforms
from PIL import Image as PilImage
from typing import TypedDict
from torch.utils.data import Dataset
from urllib import request
from utility import romanize, unfinalize


def compute_test_set(full, train, val):
	return [scroll for scroll in full if scroll not in set(train) | set(val)]

letter_box_file = 'letter_boxes.jsonl'
API_BASE = 'https://dd4-biblical.appspot.com/_api/'
LETTERBOX_BY_FRAGMENT_URL =(
		API_BASE + 'letterBoxs/v1/list?filter=filename={}&pageSize=0&orderBy=y1')
TEXT_MAP =  {'cal': '4QCalendrical.txt', 'community': '1Q_Community_Rule.txt',
	'isaiah': '1Q_Isaiah_a.txt', 'temple': '1Q_Temple.txt', 'war': '1Q_War_Scroll.txt'}
COMMUNITY_SET = list(map(lambda c: f'community-column-{c + 1}', range(11)))
COMMUNITY_TRAIN_SET = list(map(lambda c: f'community-column-{c}', [1, 3, 4, 9])) # 6
COMMUNITY_VAL_SET = list(map(lambda c: f'community-column-{c}', [2, 10])) # 5
COMMUNITY_TEST_SET = compute_test_set(COMMUNITY_SET, COMMUNITY_TRAIN_SET, COMMUNITY_VAL_SET)
ISAIAH_SET = list(map(lambda c: f'isaiah-column-{c + 1}', range(54)))
ISAIAH_TRAIN_SET = list(map(lambda c: f'isaiah-column-{c}',
														[2, 7, 11, 14, 18, 24, 28, 31, 36, 38, 44, 48, 50, 53]))
ISAIAH_VAL_SET = list(map(lambda c: f'isaiah-column-{c}', [4, 12, 17, 29, 37, 45, 52]))
ISAIAH_TEST_SET = compute_test_set(ISAIAH_SET, ISAIAH_TRAIN_SET, ISAIAH_VAL_SET)
TEMPLE_SET = list(map(lambda c: f'temple-column-{c + 1}', range(67)))
WAR_SET = list(map(lambda c: f'war-column-{c + 1}', range(15)))
WAR_TRAIN_SET = list(map(lambda c: f'war-column-{c}', [2, 4, 6, 7, 9, 12, 14]))
WAR_VAL_SET = list(map(lambda c: f'war-column-{c}', [1, 8, 11, 13]))
WAR_TEST_SET = compute_test_set(WAR_SET, WAR_TRAIN_SET, WAR_VAL_SET)
ALL = (
		ISAIAH_SET + WAR_SET + COMMUNITY_SET +
		['4QCalendrical-4Q320-Frag1', '4QCalendrical-4Q320-Frag2', '4QCalendrical-4Q320-Frag3',
		  ])
TRAINING_SET = ISAIAH_TRAIN_SET + WAR_TRAIN_SET + COMMUNITY_TRAIN_SET
VAL_SET = ISAIAH_VAL_SET + WAR_VAL_SET + COMMUNITY_VAL_SET
TEST_SET = ISAIAH_TEST_SET + WAR_TEST_SET + COMMUNITY_TEST_SET
SINGLE_LETTERS_ONLY = lambda lb:lb['type'] == 'Letter' and len(lb['value']) == 1
mean, std = (0.5,), (0.5,)
LABEL_LOOKUP = [chr(c) for c in range(ord('א'), ord('ת') + 1)] + ['?']
THRESHOLD_NAMES = {
	cv2.THRESH_BINARY: 'THRESH_BINARY',
	cv2.THRESH_BINARY_INV: 'THRESH_BINARY_INV',
	cv2.THRESH_TRUNC: 'THRESH_TRUNC',
	cv2.THRESH_TOZERO: 'THRESH_TOZERO',
	cv2.THRESH_TOZERO_INV: 'THRESH_TOZERO_INV',
	cv2.THRESH_MASK: 'THRESH_MASK',
	cv2.THRESH_OTSU: 'THRESH_OTSU',
	cv2.THRESH_TRIANGLE: 'THRESH_TRIANGLE'}


class Resize:
	def __init__(self, target_w, target_h):
		self.target_w = target_w
		self.target_h = target_h

	def __call__(self, img):
		h, w = img.shape[:2]
		scale = min(self.target_w / w, self.target_h / h)
		return cv2.resize(
				img, (round(w * scale), round(h * scale)),
				interpolation=cv2.INTER_CUBIC if scale > 1 else cv2.INTER_AREA)


class ToPilImage:
	def __call__(self, img: np.ndarray) -> PilImage:
		return PilImage.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))


class PadToSize:
	def __init__(self, target_w, target_h, fill=0):
		self.target_w = target_w
		self.target_h = target_h
		self.fill = fill

	def __call__(self, img):
		w, h = img.size
		pad_w = self.target_w - w
		pad_h = self.target_h - h
		padding = (pad_w // 2, pad_h // 2, pad_w - pad_w // 2, pad_h - pad_h // 2)
		return transforms.functional.pad(img, padding, fill=self.fill)


test_transform = transforms.Compose([
	Resize(20, 40),
	ToPilImage(),
	PadToSize(20, 40, 0),
	transforms.GaussianBlur(3, sigma=(0.1, 1.5)),
	transforms.Grayscale(),
	transforms.ToTensor(),
	transforms.Normalize(mean, std)
])


class LetterBox(TypedDict):
	id: int
	filename: str
	type: str
	x1: int
	y1: int
	x2: int
	y2: int
	value: str
	creationTime: int
	creationUsername: str
	lastModifiedTime: int
	lastModifiedUsername: str


class DSSLettersDataset(Dataset):
	def __init__(self, fragments: list[str] = None,
			filter: callable(LetterBox) = None, transform: callable(any) = None,
			overrides: list[str] = None, res: int = 9):
		self.transform = transform or (lambda x: x)
		self.res = res
		self.metadata: list[LetterBox] = []
		self.labels: list[int] = []
		for letter_box in read_database(fragments or TRAINING_SET, overrides or [],
																		filter):
			self.metadata.append(letter_box)
			value = letter_box['value']
			self.labels.append(ord(value) - ord('א') if len(
				value) == 1 and 'א' <= value <= 'ת' else 27)

		self.classes = LABEL_LOOKUP
		self.images: list = [None] * len(self.metadata)

	def __len__(self) -> int:
		return len(self.metadata)

	def __getitem__(self, idx: int) -> (any, int, dict):
		img = self.images[idx]
		if img is None:
			img = get_image(self.metadata[idx], self.res)
			self.images[idx] = img
		return self.transform(img), self.labels[idx], self.metadata[idx]


def parse_file_name(file_name):
	# Split the filename into scroll and the rest at the first hyphen
	parts = file_name.split('-', 1)
	if len(parts) != 2:
		return None  # Invalid format

	scroll, rest = parts[0], parts[1]

	is_column = rest.startswith('column-')
	if is_column:
		fragment_or_colnum = int(rest.split('-')[-1])
	else:
		# For fragments, take the entire rest as the fragment
		fragment_or_colnum = rest

	return scroll, is_column, fragment_or_colnum


def get_img_file_path(filename, res):
	scroll, is_column, fragment = parse_file_name(filename)
	if filename.startswith('war-column'):
		res = 8
	elif filename.startswith('community-column'):
		res = 7
	return f"../images/{scroll}/columns/column_{res}_{fragment}.jpg" if is_column \
		else f"../images/{scroll}/columns/{fragment}.jpg"


file_img_cache: dict[str, np.ndarray] = {}


def get_image(letter_box: dict, res: int = 9) -> np.ndarray:
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


def read_database(fragments: list[str], overrides: list[str],
		filter: callable(LetterBox)) -> list[LetterBox]:
	override_map = {f: True for f in overrides}
	db = {}
	if os.path.exists(letter_box_file):
		with open(letter_box_file, "r", encoding="utf-8") as f:
			for line in f:
				letter_box: LetterBox = json.loads(line)
				filename = letter_box['filename']
				if override_map.get(filename) is None:
					if db.get(filename) is None:
						db[filename] = []
					db[filename].append(letter_box)

	filtered = []
	change_detected = False
	for fragment in fragments:
		if db.get(fragment) is None:
			change_detected = True
			letterbox_url = LETTERBOX_BY_FRAGMENT_URL.format(fragment)
			print('Sending request: ', letterbox_url)
			with request.urlopen(letterbox_url) as url:
				response = json.load(url)
				db[fragment] = response.get('items', [])

		for letter_box in db[fragment]:
			if filter is None or filter(letter_box):
				filtered.append(letter_box)

	if change_detected:
		# Open the file for write.
		print('Writing file: ', letter_box_file)
		with open(letter_box_file, "w", encoding="utf-8") as f:
			for key in sorted(db.keys()):
				letterboxes = db[key]
				# Dump each letterbox into the file.
				for letterbox in letterboxes:
					letterbox = {
						key: value
						for key, value in letterbox.items()
						if not key.endswith(("Time", "Username"))
					}
					json.dump(letterbox, f)
					f.write("\n")

	return filtered


def get_frag_text(fragment: str) -> str:
	scroll, is_column, frag = parse_file_name(fragment)
	txt_file = f'../books/{TEXT_MAP.get(scroll)}'
	txt = ''
	roman_numeral = romanize(frag)
	with open(txt_file, 'r') as f:
		lines = f.readlines()
		l = 0
		while not lines[l].startswith(f'Col. {roman_numeral}'):
			l += 1
		while l + 1 < len(lines) and not lines[l + 1].startswith('Col. '):
			l += 1
			txt += lines[l].strip() + '\n'
	return unfinalize(txt)


def process_image(img, params):
	name = ''
	params = params or {}
	if params.get('crop'):
		img = img[params["crop"][0]:params["crop"][0] + params["crop"][1], :]
	if params.get('gray') is not None and params['gray']:
		img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
		name += 'gray'
	if params.get('bf') is not None:
		img = cv2.bilateralFilter(img, params['bf'], 75, 75)
		name += f'-bf{params['bf']}'
	if params.get('blur') == 'median':
		img = cv2.medianBlur(img, params['blur_size'])
		name += f'-median{params['blur_size']}'
	elif params.get('blur') == 'gaussian':
		img = cv2.GaussianBlur(img, [params['blur_size'], params['blur_size']],
													 sigmaX=30, sigmaY=300)
		name += f'-gaussian{params['blur_size']}'
	if params.get('threshold_type') is not None:
		threshold_type = params['threshold_type']
		img = cv2.threshold(img, params['threshold'], 255, threshold_type)[1]
		name += f'-{THRESHOLD_NAMES[threshold_type]}_{params['threshold']}'

	return img, name


def get_y_at_x(row_box, x):
	coords = row_box['coords']
	ci = 0
	while coords[ci + 1]['x'] <= x:
		ci += 1
	slope = (coords[ci + 1]['y'] - coords[ci]['y']) / (coords[ci + 1]['x'] - coords[ci]['x'])
	return (x - coords[ci]['x']) * slope + coords[ci]['y']


def is_in_row(row_box, letter_box):
	if (row_box['filename'] != letter_box['filename']
			or row_box['y2'] < letter_box['y2']
			or row_box['x1'] > letter_box['x1']
			or row_box['x2'] < letter_box['x2']):
		return False

	return get_y_at_x(row_box, letter_box['x1']) >= letter_box['y2']


row_map = {}
def get_row(filename, row):
	if not row_map:
		print('creating row map')
		letter_boxes = []
		row_boxes = []
		dataset = DSSLettersDataset()
		for _, _, letter_box in dataset:
			if letter_box['type'] == 'Row':
				letter_box['_letterBoxes'] = []
				row_boxes.append(letter_box)
				letter_box['id'] = f'{letter_box["filename"]}-{letter_box["value"]}'
				row_map[letter_box['id']] = letter_box
			elif letter_box['type'] == 'Letter':
				letter_boxes.append(letter_box)
		print(f'{len(row_boxes)} total rows')
		print(f'{len(letter_boxes)} total letters')
		row_boxes = sorted(row_boxes, key=lambda b: b['y2'])

		added_letters = 0
		for letter_box in letter_boxes:
			for row_box in row_boxes:
				if is_in_row(row_box, letter_box):
					row_box['_letterBoxes'].append(letter_box)
					added_letters += 1
					break

		print(f'{added_letters} letters added')
		row_boxes = sorted(row_boxes, key=lambda r: r['id'])
		for row_box in row_boxes:
			print(row_box['id'] + ":", len(row_box['_letterBoxes']))
			row_box['_letterBoxes'] = sorted(
					row_box['_letterBoxes'], key=lambda b: b['x2'], reverse=True)

	row_box = row_map.get(f'{filename}-{row}')
	if row_box is None and 1 < row < 31:
		print(f'Found none for {filename} Row: {row}')

	return row_box


def calc_area(box):
	return max(0, box["x2"] - box["x1"]) * max(0, box["y2"] - box["y1"])


def intersection_over_union(box1, box2):
	if box1 is None:
		return 0.0

	ix1 = max(box1["x1"], box2["x1"])
	iy1 = max(box1["y1"], box2["y1"])
	ix2 = min(box1["x2"], box2["x2"])
	iy2 = min(box1["y2"], box2["y2"])

	intersection = max(0, ix2 - ix1) * max(0, iy2 - iy1)

	area1 = calc_area(box1)
	area2 = calc_area(box2)

	union = area1 + area2 - intersection

	return intersection / union if union > 0 else 0.0


def calc_bbox_stats(target_boxes, pred_boxes, iou_threshold=.5):
	fn, fp, tp = 0, 0, 0
	for bbox in target_boxes:
		best_iou, best_pred = 0, None
		for pred_box in pred_boxes:
			if pred_box.get("_taken"):
				continue

			iou = intersection_over_union(bbox, pred_box)
			if iou > best_iou:
				best_iou = iou
				best_pred = pred_box

		value_match = bbox.get("value") is None or bbox["value"] == best_pred["value"]
		if best_iou >= iou_threshold and value_match:
			tp += 1
			best_pred["_taken"] = True
		else:
			fn += 1

	for pred_box in pred_boxes:
		if not pred_box.get("_taken"):
			fp += 1

	precision, recall = round(tp * 100 / (tp + fp), 2), round(tp * 100 / (tp + fn), 2)
	f1_score = round(2 * precision * recall / (precision + recall + 0.00001), 2)

	return fp, fn, tp, precision, recall, f1_score


if __name__ == '__main__':
	# Filter to letters, exclude rows and words.
	dataset = DSSLettersDataset(filter=SINGLE_LETTERS_ONLY)
	print(f'Training Dataset {len(dataset)} letters')
	for i in range(3):
		image, label, md = dataset[i]
		cv2.imshow(f"{md['value']} {md['filename']} ({md['x1']},{md['y1']})",
							 image)
		cv2.waitKey(2000)

	imageDataset = DSSLettersDataset()
	print(f'Training Dataset {len(imageDataset)} letters and rows')
	for i in range(3):
		image, label, md = imageDataset[i]
		cv2.imshow(f"{md['value']} {md['filename']} ({md['x1']},{md['y1']})",
							 image)
		cv2.waitKey(2000)

	imageDataset = DSSLettersDataset(fragments=ALL, filter=SINGLE_LETTERS_ONLY)
	print(f'All Dataset {len(imageDataset)} letters')
	for i in range(3):
		image, label, metadata = imageDataset[i]
		cv2.imshow(f"{md['value']} {md['filename']} ({md['x1']},{md['y1']})",
							 image)
		cv2.waitKey(2000)

	multiLetter = DSSLettersDataset(
			fragments=ALL,
			filter=lambda lb: lb['type'] == 'Letter' and len(lb['value']) > 1)
	print(f'Multi Letter Sets: {len(multiLetter)}')
	for i in range(len(multiLetter)):
		image, label, md = multiLetter[i]
		print(f"{md['value']} {md['filename']} ({md['x1']},{md['y1']})")
		cv2.imshow(f"{md['value']} {md['filename']} ({md['x1']},{md['y1']})",
							 image)
		cv2.waitKey(2000)

	image = get_image(
			{"filename": "isaiah-column-2", "type": "Letter", "x1": 76, "y1": 326,
			 "x2": 94, "y2": 348, "value": "\u05e9"})
	cv2.imshow('Direct load', image)
	cv2.waitKey(2000)

	image = get_image(
			{"filename": "4QCalendrical-4Q320-Frag1", "type": "Letter", "x1": 76,
			 "y1": 326, "x2": 94, "y2": 348, "value": "\u05e9"})
	cv2.imshow('Direct load, 4Q320', image)
	cv2.waitKey(3000)
