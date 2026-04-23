import os
import pandas as pd
import time
import torch
import torch.nn as nn
import torchvision.transforms as transforms
from letterbox_utils import DSSLettersDataset, SINGLE_LETTERS_ONLY, ToPilImage, \
  PadToSize, mean, std, test_transform
from src.main.python.ml.dd4_ml import DD4PyTorchModel, random_split, \
  visualize_augmentations, DD4Subset, conv_block
from torch.utils.data import DataLoader
from verify import process_image

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
  ToPilImage(),
  PadToSize(44, 84, 0),
  transforms.CenterCrop([40, 80]),
  transforms.RandomRotation(degrees=10),
  transforms.RandomAffine(degrees=0, translate=(0.05, 0.05), scale=(0.9, 1.1)),
  # transforms.RandomHorizontalFlip(),
  transforms.RandomPerspective(distortion_scale=0.2, p=0.3),
  transforms.ColorJitter(brightness=0.3, contrast=0.3),
  transforms.GaussianBlur(3, sigma=(0.1, 1.5)),
  transforms.Grayscale(),
  transforms.ToTensor(),
  transforms.Normalize(mean, std),
])


if __name__ == '__main__':
  train = False
  dataset = DSSLettersDataset(filter=SINGLE_LETTERS_ONLY)
  print(f'Dataset {len(dataset)} items')

  # Split into train/val/test: 80/15/5
  train_size = int(0.80 * len(dataset))
  val_size = int(0.15 * len(dataset))
  test_size = len(dataset) - train_size - val_size

  train_dataset, val_dataset, test_dataset = random_split(
      dataset,[train_size, val_size, test_size],
      [train_transform, test_transform, test_transform])
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
  for i in range(len(channels) -1):
    layers += conv_block(channels[i], channels[i+1])
  layers += [
    nn.AdaptiveAvgPool2d((1,1)),
    nn.Flatten(),
    nn.Linear(256, 256),
    nn.ReLU(),
    nn.Dropout(0.2),
    nn.Linear(256, len(dataset.classes))
  ]

  loss_function = nn.CrossEntropyLoss(label_smoothing=0.1)

  model = DD4PyTorchModel(
      train_loader=train_loader, val_loader=val_loader,
      loss_function=loss_function, layers=nn.Sequential(*layers),
      checkpoint_path=checkpoint_path, min_val_accuracy=97.71
  )

  if train or not os.path.exists(checkpoint_path):
    train_start_time = time.time()
    num_epochs = 80
    optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=num_epochs)
    best_val_accuracy, _, _ = model.train_model(num_epochs, optimizer, scheduler)
    if best_val_accuracy > 0: # If we replaced the save model we should export.
      model.export("letter_model.onnx", "image", "letter")
    print(f'Training time {(time.time() - train_start_time)} seconds')

  model.reload(checkpoint_path)
  test_loader = DataLoader(test_dataset, batch_size=1000, shuffle=False)
  loss, accuracy = model.evalulate(test_loader)
  print(f'Test Loss: {loss:.2f}, Test Accuracy: {accuracy:.2f}%')
  full_loader = DataLoader(
      DD4Subset(dataset, test_transform), batch_size=1000, shuffle=False)
  loss, accuracy = model.evalulate(full_loader)
  print(f'Full Loss: {loss:.2f}, Full Accuracy: {accuracy:.2f}%')
