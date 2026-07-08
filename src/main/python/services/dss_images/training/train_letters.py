import os
import pandas as pd
import time
import torch
import torch.nn as nn
import torchvision.transforms as transforms
from letterbox_utils import (
	DSSLettersDataset, SINGLE_LETTERS_ONLY, Resize, TRAINING_SET, VAL_SET, TEST_SET, \
	ISAIAH_SET, PadToSize, ToPilImage, mean, std, test_transform, ALL, process_image)
from src.main.python.ml.dd4_ml import DD4PyTorchModel, visualize_augmentations, DD4Subset, conv_block
from torch.utils.data import DataLoader

pd.set_option("display.max_columns", None)
pd.set_option("display.width", None)
torch.manual_seed(42)
checkpoint_path = 'letter_model.pth'


# bf7-median3-THRESH_BINARY_135
class Preprocess:
	def __init__(self, params):
		self.params = params

	def __call__(self, img):
		return process_image(img, self.params)[0]


train_transform = transforms.Compose([
	Resize(32, 64),
	ToPilImage(),
	PadToSize(32, 64, 0),
	transforms.RandomAffine(degrees=20, translate=(0.05, 0.05), scale=(0.7, 1.3)),
	transforms.RandomPerspective(distortion_scale=0.2, p=0.3),
	transforms.ColorJitter(brightness=0.3, contrast=0.3),
	transforms.GaussianBlur(3, sigma=(0.1, 1.5)),
	transforms.Grayscale(),
	transforms.ToTensor(),
	transforms.Normalize(mean, std),
])

if __name__ == '__main__':
	train = True
	train_dataset = DSSLettersDataset(filter=SINGLE_LETTERS_ONLY, transform=train_transform)
	val_dataset = DSSLettersDataset(VAL_SET, SINGLE_LETTERS_ONLY, test_transform)
	test_dataset = DSSLettersDataset(TEST_SET, SINGLE_LETTERS_ONLY, test_transform)

	print(f'Train: {len(train_dataset)} items')
	print(f'Val: {len(val_dataset)} items')
	print(f'Test: {len(test_dataset)} items')

	for name, ds in [('Train', train_dataset), ('Val', val_dataset)]:
		for idx in range(0):
			visualize_augmentations(name, ds, idx, mean, std)

	train_loader = DataLoader(train_dataset, batch_size=128, shuffle=True)
	val_loader = DataLoader(val_dataset, batch_size=1000, shuffle=False)

	layers = []
	channels = [1, 32, 64, 128, 256]
	for i in range(len(channels) - 1):
		layers += conv_block(channels[i], channels[i + 1])
	layers += [
		nn.AdaptiveAvgPool2d((1, 1)),
		nn.Flatten(),
		nn.Linear(256, 256),
		nn.ReLU(),
		nn.Dropout(0.2),
		nn.Linear(256, len(train_dataset.classes))
	]

	loss_function = nn.CrossEntropyLoss(label_smoothing=0.1)

	model = DD4PyTorchModel(
			train_loader=train_loader, val_loader=val_loader,
			loss_function=loss_function, layers=nn.Sequential(*layers),
			checkpoint_path=checkpoint_path, min_val_accuracy=97
	)

	num_params = sum(p.numel() for p in model.parameters())
	print(f"Parameters: {num_params:,}")
	param_size = sum(p.numel() * p.element_size() for p in model.parameters())
	buffer_size = sum(b.numel() * b.element_size() for b in model.buffers())
	size_mb = (param_size + buffer_size) / 1024 ** 2
	print(f"Model size: {size_mb:.2f} MB")

	if train or not os.path.exists(checkpoint_path):
		train_start_time = time.time()
		num_epochs = 120
		optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3)
		scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer,
																													 T_max=num_epochs)
		best_val_accuracy, _, _ = model.train_model(num_epochs, optimizer,
																								scheduler)
		if best_val_accuracy > 0:  # If we replaced the save model we should export.
			model.export("../letter_model.onnx", "image", "letter")
		print(f'Training time {(time.time() - train_start_time)} seconds')

	model.reload(checkpoint_path)

	eval_start = time.time()
	train_dataset = DSSLettersDataset(TRAINING_SET, SINGLE_LETTERS_ONLY, test_transform)
	train_loader = DataLoader(train_dataset, batch_size=1000)
	loss, accuracy = model.evaluate(train_loader)
	print(f'Train Loss: {loss:.2f}, Train Accuracy: {accuracy:.2f}%, Items: {len(train_dataset)}')
	print(f"Eval took: {time.time() - eval_start} seconds")

	start = time.time()
	loss, accuracy = model.evaluate(val_loader)
	print(f'Val Loss: {loss:.2f}, Val Accuracy: {accuracy:.2f}%, Items: {len(val_dataset)}')
	print(f"Eval took: {time.time() - start} seconds")

	start = time.time()
	test_loader = DataLoader(test_dataset, batch_size=1000)
	loss, accuracy = model.evaluate(test_loader)
	print(f'Test Loss: {loss:.2f}, Test Accuracy: {accuracy:.2f}%, Items: {len(test_dataset)}')
	print(f"Eval took: {time.time() - start} seconds")

	start = time.time()
	isa_dataset = DSSLettersDataset(ISAIAH_SET, SINGLE_LETTERS_ONLY, test_transform)
	isa_loader = DataLoader(isa_dataset, batch_size=1000)
	loss, accuracy = model.evaluate(isa_loader)
	print(f'Isa Loss: {loss:.2f}, All Accuracy: {accuracy:.2f}%, Items: {len(isa_dataset)}')
	print(f"Eval took: {time.time() - start} seconds")

	start = time.time()
	all_dataset = DSSLettersDataset(ALL, SINGLE_LETTERS_ONLY, test_transform)
	all_loader = DataLoader(all_dataset, batch_size=1000)
	loss, accuracy = model.evaluate(all_loader)
	print(f'All Loss: {loss:.2f}, All Accuracy: {accuracy:.2f}%, Items: {len(all_dataset)}')
	print(f"Eval took: {time.time() - start} seconds")

	print(f"Total Eval took: {time.time() - eval_start} seconds")