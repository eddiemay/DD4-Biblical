import matplotlib.pyplot as plt
import numpy as np
import onnxruntime as ort
import pandas as pd
from PIL import Image as PilImage
from label_fragment import send_json_req, LETTERBOX_BATCH_CREATE_URL
from letterbox_stats import VISUALIZE_PAGE_SIZE
from letterbox_utils import DSSLettersDataset, SINGLE_LETTERS_ONLY, ToPilImage, get_image, parse_file_name, test_transform
from scipy.special import softmax
from sklearn.metrics import confusion_matrix
from src.main.python.ml.dd4_ml import DD4Subset
from torch.utils.data import DataLoader


def visualize_missmatch(title, df):
  # See what the augmentation actually does to your images
  fig, axes = plt.subplots(int(VISUALIZE_PAGE_SIZE / 4), 4, figsize=(12, 6))
  axes = axes.flatten()
  toPilImage = ToPilImage()

  for i, row in enumerate(df.itertuples()):
    _, _, frag = parse_file_name(row.filename)
    img = toPilImage(get_image({'filename': row.filename,
                                'x1': row.x1, 'y1': row.y1, 'x2': row.x2, 'y2': row.y2}))
    if isinstance(img, PilImage.Image):
      axes[i].imshow(img)
    elif img.shape[0] == 1: # If gray scale.
      axes[i].imshow(img.squeeze(), cmap='gray')
    else:
      axes[i].imshow(img.permute(1, 2, 0))
    axes[i].set_title(f"L:{row.target} P:{row.prediction} {frag} ({row.x1},{row.y1}) {row.confidence:.2f}")

  for i in range(VISUALIZE_PAGE_SIZE):
    axes[i].axis('off')

  plt.suptitle(title)
  plt.tight_layout()
  plt.show()


def migrate_4Q320():
  dataset = DSSLettersDataset(
      fragments=['4QCalendrical-4Q320-Frag1'], cache_file='4Q320-Frag1.jsonl')
  for img, label, metadata in dataset:
    metadata['x1'], metadata['x2'], metadata['y1'], metadata['y2'] = (
      metadata['x1']*2, metadata['x2']*2, metadata['y1']*2, metadata['y2']*2)

  send_json_req(LETTERBOX_BATCH_CREATE_URL, {'items': dataset.metadata})


if __name__ == '__main__':
  dataset = DSSLettersDataset(
      fragments=['4QCalendrical-4Q320-Frag1'], filter=SINGLE_LETTERS_ONLY,
      cache_file='4Q320-Frag1.jsonl', override_cache=True)
  print(f'Dataset {len(dataset)} items')
  data_loader = DataLoader(
      DD4Subset(dataset, test_transform), batch_size=1000, shuffle=False)

  ort_session = ort.InferenceSession("letter_model.onnx")
  input_name = ort_session.get_inputs()[0].name

  label_lookup = list(dataset.classes)
  rows = []
  for inputs, targets, metadata in data_loader:
    inputs, targets = inputs.numpy(), targets.numpy()
    outputs = ort_session.run(None, {input_name: inputs})[0]
    preds = np.argmax(outputs, axis=1)

    for i in range(len(inputs)):
      rows.append({
        "target": label_lookup[targets[i]],
        "prediction": label_lookup[preds[i]],
        "missmatch": preds[i] != targets[i],
        "confidence": np.max(softmax(outputs[i])),
        "filename": metadata["filename"][i],
        "x1": metadata["x1"][i].item(),
        "y1": metadata["y1"][i].item(),
        "x2": metadata["x2"][i].item(),
        "y2": metadata["y2"][i].item(),
      })

  df = pd.DataFrame(rows)

  cm = confusion_matrix(df["target"], df["prediction"])
  cm_df = pd.DataFrame(cm)
  print(cm_df)
  # cm_norm = cm.astype("float") / cm.sum(axis=1)[:, None]
  # cm_norm_df = pd.DataFrame(cm_norm, index=dataset.classes, columns=dataset.classes)
  # print(cm_norm_df)

  missmatch = df[df['missmatch'] == True]
  print(f'Found: {len(missmatch)} missmatch out of {len(df)}')

  # by letter
  groups = missmatch.groupby("target")
  for target, group in groups:
    print(f'{target} missmatch: {len(group)}')

  # by filename
  groups = missmatch.groupby("filename")
  for fn, group in groups:
    print(f'{fn} missmatch: {len(group)}')

  missmatch.sort_values('filename')
  # Filter out wav/yod confusion before displaying, too many of those.
  missmatch = missmatch[~(
      ((missmatch['target'] == 'ו') & (missmatch['prediction'] == 'י')) |
      ((missmatch['target'] == 'י') & (missmatch['prediction'] == 'ו'))
  )]
  for start in range(0, len(missmatch), VISUALIZE_PAGE_SIZE):
    page = missmatch.iloc[start:start + VISUALIZE_PAGE_SIZE]
    visualize_missmatch(f"Missmatch labels {start + 1}–{start + len(page)} of {len(missmatch)}", page)
