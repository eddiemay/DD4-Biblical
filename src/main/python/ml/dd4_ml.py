import torch
import torch.nn as nn
from torch.utils.data import DataLoader


class DD4PyTorchModel(nn.Module):
  def __init__(self, train_loader:DataLoader=None,
      val_loader:DataLoader=None, loss_function=None, in_features=None,
      hidden_features=None, out_features=None, checkpoint_path=None):
    super().__init__()
    if torch.cuda.is_available():
      self.device = torch.device('cuda')
    elif torch.backends.mps.is_available():
      self.device = torch.device('mps')
    else:
      self.device = torch.device('cpu')
    print('using device:', self.device)

    self.batch_size = train_loader.batch_size
    self.loss_function = loss_function
    self.train_loader = train_loader
    self.val_loader = val_loader
    self.flatten = nn.Flatten()
    self.layers = nn.Sequential(
        nn.Linear(in_features, hidden_features),
        nn.ReLU(),
        nn.Linear(hidden_features, out_features)
    )
    self.to(self.device)
    self.best_val_accuracy = 0
    self.checkpoint_path = checkpoint_path

  def forward(self, x):
    return self.layers(self.flatten(x))

  def train_epoch(self, epoch, optimizer):
    self.train()
    running_loss = 0.0
    correct = 0
    total = 0

    for batch_idx, (data, target) in enumerate(self.train_loader):
      data, target = data.to(self.device), target.to(self.device)

      optimizer.zero_grad()
      output = self(data)
      loss = self.loss_function(output, target)
      loss.backward()
      optimizer.step()

      # Track progress
      running_loss += loss.item()
      _, predicted = output.max(1)
      total += target.size(0)
      correct += predicted.eq(target).sum().item()

      # Print every 100 batches
      if batch_idx % 100 == 0 and batch_idx > 0:
        avg_loss = running_loss / 100
        accuracy = 100. * correct / total
        print(f' [{batch_idx * self.batch_size}/{len(self.train_loader.dataset)} '
              f'Loss: {avg_loss:.3f} | Accuracy: {accuracy:.1f}%')
        running_loss = 0.0

  def evaluate(self):
    self.eval()
    correct = 0
    total = 0
    batches = 0
    # running_val_loss = 0

    with torch.no_grad():
      for inputs, targets in self.val_loader:
        inputs, targets = inputs.to(self.device), targets.to(self.device)
        outputs = self(inputs)
        _, predicted = outputs.max(1)
        total += targets.size(0)
        correct += predicted.eq(targets).sum().item()
        # val_loss = self.loss_function(outputs, targets)
        # batches += 1
        # running_val_loss += val_loss.item()

    # avg_val_loss = running_val_loss / batches
    return 100. * correct / total

  def train_model(self, epochs, optimizer):
    # Training loop
    for epoch in range(1, epochs + 1):
      print('\nEpoch: ', epoch)
      self.train_epoch(epoch, optimizer)
      accuracy = self.evaluate()
      print(f'Validation Accuracy: {accuracy:.2f}%')
      # --- Checkpoint ---
      if self.checkpoint_path and accuracy > self.best_val_accuracy:
        print(f"Validation improved from {self.best_val_accuracy:.2f}% → {accuracy:.2f}%. Saving model.")
        self.best_val_accuracy = accuracy
        torch.save({
          'epoch': epoch,
          'model_state_dict': self.state_dict(),
          'optimizer_state_dict': optimizer.state_dict(),
          'val_accuracy': accuracy
        }, self.checkpoint_path)