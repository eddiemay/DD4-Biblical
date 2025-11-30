import time
import torch
import torch.nn as nn
import torchvision
import torchvision.transforms as transforms
from torch.utils.data import DataLoader
from dd4_ml import DD4PyTorchModel

# Data preprocessing
transform = transforms.Compose([
  transforms.ToTensor(),
  transforms.Normalize((0.1307,), (0.3081,))
])

# Load MNIST dataset
train_dataset = torchvision.datasets.MNIST(
    root='../../../../data/', train=True, download=True, transform=transform)
val_dataset = torchvision.datasets.MNIST(
    root='../../../../data/', train=False, download=True, transform=transform)


if __name__ == '__main__':
  model = DD4PyTorchModel(
      train_loader=DataLoader(train_dataset, batch_size=256, shuffle=True),
      val_loader=DataLoader(val_dataset, batch_size=1000, shuffle=False),
      loss_function=nn.CrossEntropyLoss(),
      in_features=784, hidden_features=128, out_features=10
  )

  train_start_time = time.time()
  model.train_model(10, torch.optim.Adam(model.parameters(), lr=0.001))
  print(f'Total time {(time.time() - train_start_time)} seconds')
