import copy
import matplotlib.pyplot as plt
import torch
import torch.nn as nn
from torch.utils.data import DataLoader, Dataset
from torchvision import models as tv_models


class DD4PyTorchModel(nn.Module):
  def __init__(self, train_loader:DataLoader=None,
      val_loader:DataLoader=None, loss_function=None, layers=None,
      in_features=None, hidden_features=None, out_features=None, model=None,
      checkpoint_path=None, min_val_accuracy=0):
    super().__init__()
    self.device = get_best_device()

    self.batch_size = train_loader.batch_size
    self.loss_function = loss_function
    self.train_loader = train_loader
    self.val_loader = val_loader
    self.model = model
    self.layers = layers if layers else nn.Sequential(
        nn.Flatten(),
        nn.Linear(in_features, hidden_features),
        nn.ReLU(),
        nn.Linear(hidden_features, out_features)
    )
    self.to(self.device)
    self.checkpoint_path = checkpoint_path
    self.min_val_accuracy = min_val_accuracy

  def forward(self, x):
    return self.layers(x)

  def train_model(self, epochs, optimizer, scheduler=None):
    return train_model(
        self if self.model is None else self.model, epochs, self.train_loader,
        self.val_loader, self.loss_function, optimizer, scheduler,
        self.checkpoint_path, self.min_val_accuracy)

  def evalulate(self, val_loader=None):
    return evaluate(self if self.model is None else self.model,
                    val_loader or self.val_loader, self.loss_function)

  def reload(self, filepath:str):
    reload(self, filepath)


class DD4Subset(Dataset):
  def __init__(self, subset, transform):
    self.subset = subset
    self.transform = transform

  def __len__(self):
    return len(self.subset)

  def __getitem__(self, idx):
    batch = self.subset[idx]
    image = batch[0]
    label = batch[1]
    if self.transform:
      image = self.transform(image)
    return (image, label) if len(batch) == 2 else (image, label, batch[2])


def conv_block(in_c, out_c):
  return [
    nn.Conv2d(in_c, out_c, 3, padding=1),
    nn.BatchNorm2d(out_c),
    nn.ReLU(),
    nn.MaxPool2d(2)
  ]


def evaluate(model, val_loader, loss_function):
  model.eval()
  correct, total, batches, running_val_loss = 0, 0, 0, 0

  with torch.no_grad():
    for batch in val_loader:
      inputs, targets = batch[0].to(model.device), batch[1].to(model.device)
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

  for batch in train_loader:
    data, target = batch[0].to(model.device), batch[1].to(model.device)

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


def train_model(model, epochs, train_loader, val_loader, loss_function, optimizer,
    scheduler=None, checkpoint_path=None, min_val_accuracy=0):
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
    if val_accuracy > best_val_accuracy and val_accuracy > min_val_accuracy:
      if checkpoint_path is not None:
        print(f" Validation improved from {best_val_accuracy:.2f}% → {val_accuracy:.2f}%. Saving model.")
        torch.save({
          'epoch': epoch,
          'model_state_dict': model.state_dict(),
          'optimizer_state_dict': optimizer.state_dict(),
          'val_accuracy': val_accuracy
        }, checkpoint_path)
      best_val_accuracy = val_accuracy
      best_epoch = epoch
      best_model_state = copy.deepcopy(model.state_dict())

  return best_val_accuracy, best_epoch, best_model_state


def reload(model:nn.Module, filepath:str):
  model.load_state_dict(torch.load(filepath, map_location=model.device)['model_state_dict'])


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


def denormalize(img, mean, std):
  if mean is None or std is None:
    return img

  mean = torch.tensor(mean).view(-1,1,1)
  std = torch.tensor(std).view(-1,1,1)
  return img * std + mean


def visualize_augmentations(name, dataset, idx=0, mean=None, std=None, num_version=8):
  # See what the augmentation actually does to your images
  fig, axes = plt.subplots(2, 4, figsize=(12, 6))
  axes = axes.flatten()

  for i in range(num_version):
    img, label = dataset[idx]

    # Denormalize for display
    img = denormalize(img, mean, std)

    if img.shape[0] == 1: # If gray scale.
      axes[i].imshow(img.squeeze(), cmap='gray')
    else:
      axes[i].imshow(img.permute(1, 2, 0)) # CHW -> HWC
    axes[i].set_title(f'Version {i + 1}')
    axes[i].axis('off')

  plt.suptitle(f'{name} index {idx}, 8 different augmentations')
  plt.tight_layout()
  plt.show()