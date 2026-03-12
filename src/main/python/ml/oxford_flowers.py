import os
import requests
import scipy
import time
import torch
import torch.nn as nn
from PIL import Image
from dd4_ml import (DD4PyTorchModel, evaluate, random_split, train_model,
                    load_mobilenet_v3_small, visualize_augmentations)
from pathlib import Path
from torch.utils.data import DataLoader, Dataset
from torchvision import transforms
from urllib.parse import urlparse

path = '../../../../data/flower_data'
# Determine the number of classes from the training dataset.
num_classes = 102


def download_file(url, filename):
  try:
    print(f"Downloading {url} to {filename}")
    # Make a GET request to the URL (use stream=True for large files)
    response = requests.get(url, stream=True)

    # Check if the request was successful (status code 200)
    if response.status_code == 200:
      # Open the local file in binary write mode ('wb')
      with open(filename, 'wb') as file:
        # Write the content in chunks to efficiently handle large files
        for chunk in response.iter_content(chunk_size=1024):
          if chunk: # filter out keep-alive new chunks
            file.write(chunk)
      print(f"File successfully downloaded and saved as {filename}")
    else:
      print(f"Failed to retrieve the file. Status code: {response.status_code}")
  except requests.exceptions.RequestException as e:
    print(f"An error occurred: {e}")


def download_dataset():
  """Download the Oxford 102 Flowers dataset"""
  # Create directory
  os.makedirs(path, exist_ok=True)
  for url in ["https://www.robots.ox.ac.uk/~vgg/data/flowers/102/102flowers.tgz",
              "https://www.robots.ox.ac.uk/~vgg/data/flowers/102/imagelabels.mat"]:
    download_file(url, os.path.join(path, Path(urlparse(url).path).name))


class OxfordFlowersDataset(Dataset):
  def __init__(self):
    self.img_dir = os.path.join(path, 'jpg')

    if not os.path.exists(os.path.join(path, 'imagelabels.mat')):
      download_dataset()

    labels_mat = scipy.io.loadmat(os.path.join(path, 'imagelabels.mat'))

    self.labels = labels_mat['labels'][0] - 1
    self.classes = torch.unique(torch.tensor(self.labels))
    print('Unique flower count:', self.classes)

  # How many total samples
  def __len__(self):
    return len(self.labels)

  # How to get image and label number 'idx'
  def __getitem__(self, idx):
    # Build the image filename
    img_name = f'image_{idx + 1:05d}.jpg'
    img_path = os.path.join(self.img_dir, img_name)

    # Loads the image
    image = Image.open(img_path)
    label = self.labels[idx]
    return image, label


mean=[0.485, 0.456, 0.406]
std=[0.229, 0.224, 0.225]

test_transform = transforms.Compose([
  transforms.Resize(256), # Resize image to 256 pixels tall, keeping the aspect ratio
  transforms.CenterCrop(224), # Extract 224x224 center square
  transforms.ToTensor(),
  transforms.Normalize(mean=mean, std=std)
])

train_transform = transforms.Compose([
  transforms.RandomHorizontalFlip(p=0.5),
  transforms.RandomVerticalFlip(p=0.5),
  transforms.RandomRotation(degrees=30),
  transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2),
  transforms.RandomResizedCrop(224),
  transforms.ToTensor(),
  transforms.Normalize(mean=mean, std=std)
])

dataset = OxfordFlowersDataset()
print(f'Dataset {len(dataset)} items')

# Split into train/val/test: 70/15/15
train_size = int(0.7 * len(dataset))
val_size = int(0.15 * len(dataset))
test_size = len(dataset) - train_size - val_size

train_dataset, val_dataset, test_dataset = random_split(
    dataset,
    [train_size, val_size, test_size],
    [train_transform, test_transform, test_transform])
print(f'Train: {len(train_dataset)} items')
print(f'Val: {len(val_dataset)} items')
print(f'Test: {len(test_dataset)} items')

# Create DataLoaders with appropriate settings
train_loader = DataLoader(train_dataset, batch_size=64, shuffle=True)
val_loader = DataLoader(val_dataset, batch_size=64, shuffle=False)
test_loader = DataLoader(test_dataset, batch_size=64, shuffle=False)

# Verify everything works
print(f'Train: {len(train_loader)} batches')
print(f'Val: {len(val_loader)} batches')
print(f'Test: {len(test_loader)} batches')

# Quick test - get one batch from each
for name, loader in [('Train', train_loader), ('Val', val_loader), ('Test', test_loader)]:
  images, labels = next(iter(loader))
  print(f'{name} batch: {images.shape}')

if __name__ == '__main__':
  train = False
  for name, ds in [('Train', train_dataset), ('Val', val_dataset)]:
    for idx in range(2):
      visualize_augmentations(name, ds, idx, mean, std)

  loss_function = nn.CrossEntropyLoss()
  model = DD4PyTorchModel(
      train_loader=train_loader,
      val_loader=val_loader,
      loss_function=loss_function,
      layers = nn.Sequential(
          nn.Linear(3*224*224, 1024),
          nn.ReLU(),
          nn.Linear(1024, 512),
          nn.ReLU(),
          nn.Linear(512, 256),
          nn.ReLU(),
          nn.Linear(256, num_classes)
      ),
      checkpoint_path='oxford_flowers.pt'
  )

  if train:
    train_start_time = time.time()
    # model.train_model(3, torch.optim.Adam(model.parameters(), lr=0.001))
    print(f'PyTorchModel train time {(time.time() - train_start_time)} seconds')

    train_start_time = time.time()
    train_loader.classes = dataset.classes
    model = load_mobilenet_v3_small("mobilenet_v3_small-047dcff4.pth", len(train_loader.classes))
    num_epochs = 64
    optimizer = torch.optim.AdamW(model.parameters(), lr=1e-4)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=num_epochs)

    best_val_accuracy, best_epoch, best_model_state = train_model(
        model, num_epochs, train_loader, val_loader, loss_function, optimizer, scheduler)

    current_best = torch.load('oxford_flowers_mobilenet_v3_small.pt', map_location='cpu')['val_accuracy']
    print(f"\n--- Best model: {best_val_accuracy:.2f}% validation accuracy, achieved at epoch {best_epoch} ---")
    if best_val_accuracy > current_best:
      print(f'Best increased from {current_best} to {best_val_accuracy}. Saving model.')
      # Load the state of the best model.
      model.load_state_dict(best_model_state)
      torch.save({
        'epoch': best_epoch,
        'model_state_dict': best_model_state,
        'optimizer_state_dict': optimizer.state_dict(),
        'val_accuracy': best_val_accuracy
      }, 'oxford_flowers_mobilenet_v3_small.pt')
    print(f'Course 2 train time {(time.time() - train_start_time)} seconds')

  evaluate_start = time.time()
  reloaded = load_mobilenet_v3_small("oxford_flowers_mobilenet_v3_small.pt", num_classes, True)
  test_loss, test_accuracy = evaluate(reloaded, test_loader, loss_function)
  print(f"Test Loss: {test_loss:.4f}, Test Accuracy: {test_accuracy:.2f}%")
  test_loss, test_accuracy = evaluate(reloaded, val_loader, loss_function)
  print(f"Val Loss: {test_loss:.4f}, Val Accuracy: {test_accuracy:.2f}%")
  print(f'Evaluate time {(time.time() - evaluate_start)} seconds')