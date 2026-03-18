import matplotlib.pyplot as plt
import pandas as pd
import time
import torch
import torch.nn as nn
import torchvision.transforms as transforms
from letterbox_utils import DSSLettersDataset, SINGLE_LETTERS_ONLY, ToPilImage
from src.main.python.ml.dd4_ml import DD4PyTorchModel, random_split, \
  visualize_augmentations, DD4Subset, conv_block
from sklearn.metrics import confusion_matrix
from torch.utils.data import DataLoader
from verify import process_image
from letterbox_stats import VISUALIZE_PAGE_SIZE

pd.set_option("display.max_columns", None)
pd.set_option("display.width", None)
torch.manual_seed(42)
mean, std = (0.5,), (0.5,)

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


test_transform = transforms.Compose([
  ToPilImage(),
  PadToSize(40, 80, 0),
  transforms.CenterCrop([40, 80]),
  transforms.GaussianBlur(3, sigma=(0.1, 1.5)),
  transforms.Grayscale(),
  transforms.ToTensor(),
  transforms.Normalize(mean, std)
])

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


def visualize_incorrect(title, df):
  # See what the augmentation actually does to your images
  fig, axes = plt.subplots(int(VISUALIZE_PAGE_SIZE / 4), 4, figsize=(12, 6))
  axes = axes.flatten()

  for i, row in enumerate(df.itertuples()):
    img = row.image
    if img.shape[0] == 1: # If gray scale.
      axes[i].imshow(img.squeeze(), cmap='gray')
    else:
      axes[i].imshow(img.permute(1, 2, 0))
    axes[i].set_title(f"L:{row.target} P:{row.prediction} {row.filename} ({row.x1},{row.y1}) {row.confidence:.2f}")

  for i in range(VISUALIZE_PAGE_SIZE):
    axes[i].axis('off')

  plt.suptitle(title)
  plt.tight_layout()
  plt.show()


if __name__ == '__main__':
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
      checkpoint_path='letter_model.pth', best_val_accuracy=97.71
  )

  train_start_time = time.time()
  num_epochs = 80
  optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3)
  scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=num_epochs)
  model.train_model(num_epochs, optimizer, scheduler)
  print(f'Training time {(time.time() - train_start_time)} seconds')

  model.reload('letter_model.pth')
  test_loader = DataLoader(test_dataset, batch_size=1000, shuffle=False)
  loss, accuracy = model.evalulate(test_loader)
  print(f'Test Loss: {loss:.2f}, Test Accuracy: {accuracy:.2f}%')
  full_loader = DataLoader(
      DD4Subset(dataset, test_transform), batch_size=1000, shuffle=False)
  loss, accuracy = model.evalulate(full_loader)
  print(f'Full Loss: {loss:.2f}, Full Accuracy: {accuracy:.2f}%')

  model.eval()
  label_lookup = list(dataset.classes)
  rows = []
  with torch.no_grad():
    for inputs, targets, metadata in full_loader:
      inputs, targets = inputs.to(model.device), targets.to(model.device)
      outputs = model(inputs)
      preds = outputs.argmax(dim=1)

      for i in range(len(inputs)):
        rows.append({
          "target": label_lookup[targets[i].item()],
          "prediction": label_lookup[preds[i].item()],
          "correct": preds[i].item() == targets[i].item(),
          "confidence": outputs[i].softmax(dim=0).max().item(),
          "filename": metadata["filename"][i],
          "x1": metadata["x1"][i].item(),
          "y1": metadata["y1"][i].item(),
          "image": inputs[i].cpu()
        })

  df = pd.DataFrame(rows)

  cm = confusion_matrix(df["target"], df["prediction"])
  cm_df = pd.DataFrame(cm)
  print(cm_df)
  # cm_norm = cm.astype("float") / cm.sum(axis=1)[:, None]
  # cm_norm_df = pd.DataFrame(cm_norm, index=dataset.classes, columns=dataset.classes)
  # print(cm_norm_df)

  incorrect = df[df['correct'] == False]
  print(f'Found: {len(incorrect)} incorrect out of {len(df)}')

  # by letter
  groups = incorrect.groupby("target")
  for target, group in groups:
    print(f'{target} incorrect: {len(group)}')

  # by filename
  groups = incorrect.groupby("filename")
  for fn, group in groups:
    print(f'{fn} incorrect: {len(group)}')

  incorrect.sort_values('filename')
  # Filter out wav/yod confusion before displaying, too many of those.
  incorrect = incorrect[~(
      ((incorrect['target'] == 'ו') & (incorrect['prediction'] == 'י')) |
      ((incorrect['target'] == 'י') & (incorrect['prediction'] == 'ו'))
  )]
  for start in range(0, len(incorrect), VISUALIZE_PAGE_SIZE):
    page = incorrect.iloc[start:start + VISUALIZE_PAGE_SIZE]
    visualize_incorrect(f"InCorrect labels {start + 1}–{start + len(page)} of {len(incorrect)}", page)
