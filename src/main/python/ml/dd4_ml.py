import copy
import torch
import torch.nn as nn
from torch.utils.data import DataLoader, Dataset
from torchvision import models as tv_models


class DD4PyTorchModel(nn.Module):
  def __init__(self, train_loader:DataLoader=None,
      val_loader:DataLoader=None, loss_function=None, layers=None,
      in_features=None, hidden_features=None, out_features=None,
      checkpoint_path=None):
    super().__init__()
    self.device = get_best_device()

    self.batch_size = train_loader.batch_size
    self.loss_function = loss_function
    self.train_loader = train_loader
    self.val_loader = val_loader
    self.flatten = nn.Flatten()
    self.layers = layers if layers else nn.Sequential(
        nn.Linear(in_features, hidden_features),
        nn.ReLU(),
        nn.Linear(hidden_features, out_features)
    )
    self.to(self.device)
    self.checkpoint_path = checkpoint_path

  def forward(self, x):
    return self.layers(self.flatten(x))

  def train_model(self, epochs, optimizer):
    return train_model(self, epochs, self.train_loader, self.val_loader, self.loss_function, optimizer)

  def evalulate(self):
    return evaluate(self, self.val_loader, self.loss_function)


class DD4Subset(Dataset):
  def __init__(self, subset, transform):
    self.subset = subset
    self.transform = transform

  # How many total samples
  def __len__(self):
    return len(self.subset)

  # How to get image and label number 'idx'
  def __getitem__(self, idx):
    image, label = self.subset[idx]
    if self.transform:
      image = self.transform(image)
    return image, label


def evaluate(model, val_loader, loss_function):
  model.eval()
  correct, total, batches, running_val_loss = 0, 0, 0, 0

  with torch.no_grad():
    for inputs, targets in val_loader:
      inputs, targets = inputs.to(model.device), targets.to(model.device)
      outputs = model(inputs)
      _, predicted = outputs.max(1)
      total += targets.size(0)
      correct += predicted.eq(targets).sum().item()
      val_loss = loss_function(outputs, targets)
      batches += 1
      running_val_loss += val_loss.item()

  avg_val_loss = running_val_loss / batches
  return avg_val_loss, 100. * correct / total


def train_epoch(model, train_loader, loss_function, optimizer):
  model.train()
  running_loss, correct, total, batches = 0, 0, 0, 0

  for data, target in train_loader:
    data, target = data.to(model.device), target.to(model.device)

    optimizer.zero_grad()
    output = model(data)
    loss = loss_function(output, target)
    loss.backward()
    optimizer.step()

    # Track progress
    running_loss += loss.item()
    batches += 1
    _, predicted = output.max(1)
    total += target.size(0)
    correct += predicted.eq(target).sum().item()

  return running_loss / batches


def train_model(model, epochs, train_loader, val_loader, loss_function, optimizer, scheduler=None):
  best_val_accuracy, best_epoch = 0, 0
  best_model_state = None
  # Training loop
  for epoch in range(1, epochs + 1):
    epoch_loss = train_epoch(model, train_loader, loss_function, optimizer)
    val_loss, val_accuracy = evaluate(model, val_loader, loss_function)
    print(f"Epoch [{epoch}/{epochs}], Train Loss: {epoch_loss:.4f}, Val Loss: {val_loss:.4f}, Val Accuracy: {val_accuracy:.2f}%")

    if scheduler is not None:
      # Update the learning rate scheduler.
      scheduler.step()

    # --- Checkpoint ---
    if val_accuracy > best_val_accuracy:
      best_val_accuracy = val_accuracy
      best_epoch = epoch
      best_model_state = copy.deepcopy(model.state_dict())
      if model.checkpoint_path is not None:
        print(f" Validation improved from {best_val_accuracy:.2f}% → {val_accuracy:.2f}%. Saving model.")
        torch.save({
          'epoch': best_epoch,
          'model_state_dict': best_model_state,
          'optimizer_state_dict': optimizer.state_dict(),
          'val_accuracy': best_val_accuracy
        }, model.checkpoint_path)

  return best_val_accuracy, best_epoch, best_model_state


def random_split(dataset, lengths, transforms):
  splits = torch.utils.data.random_split(dataset, lengths)
  subsets = []
  for split, transform in zip(splits, transforms):
    subsets.append(DD4Subset(split, transform))
  return subsets


def get_best_device():
  if torch.cuda.is_available():
    device = torch.device('cuda')
  elif torch.backends.mps.is_available():
    device = torch.device('mps')
  else:
    device = torch.device('cpu')
  print('using device:', device)
  return device


def load_mobilenet_v3_small(weights_path:str, num_classes:int, post_load:bool=False):
  device = get_best_device()
  model = tv_models.mobilenet_v3_small(weights=None)
  # Load a pre-trained state dictionary from a local file.
  state_dict = torch.load(weights_path, map_location=device)
  if not post_load:
    # Load the state dictionary into the model.
    model.load_state_dict(state_dict)
  # Get the number of input features for the final classifier layer.
  in_features = model.classifier[3].in_features
  # Replace the final classifier layer to match the number of classes in the dataset.
  model.classifier[3] = nn.Linear(in_features, num_classes)
  if post_load:
    # Load the state dictionary into the model.
    model.load_state_dict(state_dict['model_state_dict'])
  model.to(device)
  model.device = device

  return model