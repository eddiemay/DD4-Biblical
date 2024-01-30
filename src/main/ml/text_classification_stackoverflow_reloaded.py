import tensorflow as tf
import os

from src.main.ml.global_functions import custom_standardization

print(tf.__version__)

reloaded = tf.keras.models.load_model(
  'text_classification_stackoverflow',
  custom_objects={"custom_standardization": custom_standardization})

sourceBase = '../../../src/main'
javaBase = sourceBase + '/java/com/digitald4/biblical'

with open(os.path.join(javaBase, 'model/Commandment.java')) as f:
  javaModelObj = f.read()

with open(os.path.join(javaBase, 'tools/DiffStats.java')) as f:
  javaFile = f.read()

with open(os.path.join(sourceBase, 'js/BiblicalCtrl.js')) as f:
  jsFile = f.read()

with open(os.path.join(sourceBase, 'ml/text_classification.py')) as f:
  pythonFile = f.read()

print(reloaded.predict([javaModelObj, javaFile, jsFile, pythonFile]))