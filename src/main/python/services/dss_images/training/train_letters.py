import time
import torch
import torch.nn as nn
import torchvision.transforms as transforms
from letterbox_utils import DSSLettersDataset, SINGLE_LETTERS_ONLY, ToImage, ToPilImage
from src.main.python.ml.dd4_ml import DD4PyTorchModel, random_split, \
  visualize_augmentations, DD4Subset, conv_block, load_mobilenet_v3_small, train_model
from torch.utils.data import DataLoader
from verify import process_image


# bf7-median3-THRESH_BINARY_135
class Preprocess:
  def __init__(self, params):
    self.params = params

  def __call__(self, img):
    return process_image(img, self.params)[0]


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


mean, std = (0.5,), (0.5,)
transform = transforms.Compose([
  # ToImage(),
  # Preprocess({"bf": 7, "blur": "median", "blur_size": 3, "threshold": 135, "threshold_type": 0}),
  # Preprocess({"bf": 35, "blur": "median", "blur_size": 3}),
  ToPilImage(),
  PadToSize(40, 80, 0),
  transforms.CenterCrop([40, 80]),
  transforms.Grayscale(),
])

train_transform = transforms.Compose([
  transforms.RandomRotation(degrees=10),
  transforms.RandomAffine(degrees=0, translate=(0.05, 0.05), scale=(0.9, 1.1)),
  transforms.RandomHorizontalFlip(),
  transforms.RandomPerspective(distortion_scale=0.2, p=0.3),
  transforms.ColorJitter(brightness=0.3, contrast=0.3),
  transforms.GaussianBlur(3, sigma=(0.1,1.5)),
  transforms.ToTensor(),
  transforms.Normalize(mean, std)
])

test_transform = transforms.Compose([
  transforms.ToTensor(),
  transforms.Normalize(mean, std)
])


if __name__ == '__main__':
  dataset = DSSLettersDataset(filter=SINGLE_LETTERS_ONLY, transform=transform)
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
    for idx in range(2):
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
    nn.Dropout(0.6),
    nn.Linear(256, len(dataset.classes))
  ]

  loss_function = nn.CrossEntropyLoss(label_smoothing=0.1)

  model = DD4PyTorchModel(
      train_loader=train_loader, val_loader=val_loader,
      loss_function=loss_function, layers=nn.Sequential(*layers),
      # in_features=3200, hidden_features=256, out_features=len(dataset.classes)
      # model=load_mobilenet_v3_small('../../../ml/mobilenet_v3_small-047dcff4.pth', len(dataset.classes)),
      checkpoint_path='letter_model.pth'
  )

  train_start_time = time.time()
  num_epochs = 10
  optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3)
  scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=num_epochs)
  model.train_model(num_epochs, optimizer, scheduler)
  print(f'Total time {(time.time() - train_start_time)} seconds')

  loss, accuracy = model.evalulate(DataLoader(test_dataset, batch_size=1000, shuffle=False))
  print(f'Test Loss: {loss:.2f}, Test Accuracy: {accuracy:.2f}%')
  loss, accuracy = model.evalulate(DataLoader(
      DD4Subset(dataset, test_transform), batch_size=1000, shuffle=False))
  print(f'Full Loss: {loss:.2f}, Full Accuracy: {accuracy:.2f}%')
