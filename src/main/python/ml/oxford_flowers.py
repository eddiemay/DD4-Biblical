import os
import requests
import scipy
import time
import torch
import torch.nn as nn
import torchvision.transforms as transforms
from PIL import Image
from torch.utils.data import DataLoader, Dataset, random_split
from urllib.parse import urlparse
from pathlib import Path
from dd4_ml import DD4PyTorchModel


path = '../../../../data/flower_data'

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
  def __init__(self, subset=None, transform=None):
    self.img_dir = os.path.join(path, 'jpg')
    self.subset = subset
    self.transform = transform

    if not os.path.exists(os.path.join(path, 'imagelabels.mat')):
      download_dataset()

    labels_mat = scipy.io.loadmat(os.path.join(path, 'imagelabels.mat'))

    self.labels = labels_mat['labels'][0] - 1
    print('Unique flower count:', torch.unique(torch.tensor(self.labels)))

  # How many total samples
  def __len__(self):
    return len(self.subset) if self.subset else len(self.labels)

  # How to get image and label number 'idx'
  def __getitem__(self, idx):
    # Build the image filename
    img_name = f'image_{idx + 1:05d}.jpg'
    img_path = os.path.join(self.img_dir, img_name)

    # Loads the image
    image = Image.open(img_path)
    label = self.labels[idx]

    if self.transform:
      image = self.transform(image)
    return image, label

test_transform = transforms.Compose([
  transforms.Resize(256), # Resize image to 256 pixels tall, keeping the aspect ratio
  transforms.CenterCrop(224), # Extract 224x224 center square
  transforms.ToTensor(),
  transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

train_transform = transforms.Compose([
  transforms.RandomHorizontalFlip(p=0.5),
  transforms.RandomRotation(degrees=10),
  transforms.ColorJitter(brightness=0.2),
  transforms.Resize(256), # Resize image to 256 pixels tall, keeping the aspect ratio
  transforms.CenterCrop(224), # Extract 224x224 center square
  transforms.ToTensor(),
  transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

dataset = OxfordFlowersDataset(transform=test_transform)

# Split into train/val/test: 70/15/15
train_size = int(0.7 * len(dataset))
val_size = int(0.15 * len(dataset))
test_size = len(dataset) - train_size - val_size

train_dataset, val_dataset, test_dataset = random_split(
    dataset, [train_size, val_size, test_size])

# Create DataLoaders with appropriate settings
train_loader = DataLoader(
    OxfordFlowersDataset(subset=train_dataset, transform=train_transform),
    batch_size=32, shuffle=True)
val_loader = DataLoader(val_dataset, batch_size=32, shuffle=False)
test_loader = DataLoader(test_dataset, batch_size=32, shuffle=False)

# Verify everything works
print(f'Train: {len(train_loader)} batches')
print(f'Val: {len(val_loader)} batches')
print(f'Test: {len(test_loader)} batches')

# Quick test - get one batch from each
for name, loader in [('Train', train_loader), ('Val', val_loader), ('Test', test_loader)]:
  images, labels = next(iter(loader))
  print(f'{name} batch: {images.shape}')

if __name__ == '__main__':
  model = DD4PyTorchModel(
      train_loader=train_loader,
      val_loader=val_loader,
      loss_function=nn.CrossEntropyLoss(),
      in_features=3*224*224, hidden_features=128, out_features=102,
      checkpoint_path='oxford_flower.pt'
  )

  train_start_time = time.time()
  model.train_model(5, torch.optim.Adam(model.parameters(), lr=0.001))
  print(f'Total time {(time.time() - train_start_time)} seconds')