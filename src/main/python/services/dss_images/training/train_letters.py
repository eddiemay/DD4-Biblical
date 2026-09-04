import argparse
import os
import pandas as pd
import time
import torch
import torch.nn as nn
import torchvision.transforms as transforms
from letterbox_utils import DSSLettersDataset, SINGLE_LETTERS_ONLY, Resize, \
	PadToSize, ToPilImage, mean, std, test_transform, ALL, process_image, \
	TRAINING_SET, VAL_SET, TEST_SET, ISAIAH_SET, WAR_SET, COMMUNITY_SET
from dd4_ml import DD4PyTorchModel, visualize_augmentations, conv_block
from torch.utils.data import DataLoader

pd.set_option("display.max_columns", None)
pd.set_option("display.width", None)
torch.manual_seed(42)
checkpoint_path = 'letter_model.pth'

train_transform = transforms.Compose([
	Resize(20, 40),
	ToPilImage(),
	PadToSize(20, 40, 0),
	transforms.RandomAffine(degrees=20, translate=(0.05, 0.05), scale=(0.7, 1.3)),
	transforms.RandomPerspective(distortion_scale=0.2, p=0.3),
	transforms.ColorJitter(brightness=0.3, contrast=0.3),
	transforms.GaussianBlur(3, sigma=(0.1, 1.5)),
	transforms.Grayscale(),
	transforms.ToTensor(),
	transforms.Normalize(mean, std),
])


def verify(title, dataset, loader=None):
	start = time.time()
	loader = loader or DataLoader(dataset, batch_size=2048)
	loss, accuracy = model.evaluate(loader)
	print(f'{title} Loss: {loss:.2f}, Accuracy: {accuracy:.2f}%, '
				f'Items: {len(dataset)} Time: {time.time() - start:.1f} seconds')


if __name__ == '__main__':
	parser = argparse.ArgumentParser()
	parser.add_argument('--train', action='store_true')
	args = parser.parse_args()
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
			checkpoint_path=checkpoint_path, min_val_accuracy=95
	)

	num_params = sum(p.numel() for p in model.parameters())
	print(f"Parameters: {num_params:,}")
	param_size = sum(p.numel() * p.element_size() for p in model.parameters())
	buffer_size = sum(b.numel() * b.element_size() for b in model.buffers())
	size_mb = (param_size + buffer_size) / 1024 ** 2
	print(f"Model size: {size_mb:.2f} MB")

	if args.train or not os.path.exists(checkpoint_path):
		train_start_time = time.time()
		num_epochs = 120
		optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3)
		scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, num_epochs)
		best_val_accuracy, _, _ = model.train_model(num_epochs, optimizer, scheduler)
		if best_val_accuracy > 0:  # If we replaced the save model we should export.
			model.export("../letter_model.onnx", "image", "letter")
		print(f'Training time {(time.time() - train_start_time):.1f} seconds')

	model.reload(checkpoint_path)

	eval_start = time.time()
	verify('Train', DSSLettersDataset(TRAINING_SET, SINGLE_LETTERS_ONLY, test_transform))
	verify('Val', val_dataset, val_loader)
	verify('Test', test_dataset)
	verify('Isa', DSSLettersDataset(ISAIAH_SET, SINGLE_LETTERS_ONLY, test_transform))
	verify('War', DSSLettersDataset(WAR_SET, SINGLE_LETTERS_ONLY, test_transform))
	verify('Community', DSSLettersDataset(COMMUNITY_SET, SINGLE_LETTERS_ONLY, test_transform))
	verify('All', DSSLettersDataset(ALL, SINGLE_LETTERS_ONLY, test_transform))
	print(f"Total Eval took: {time.time() - eval_start:.1f} seconds")