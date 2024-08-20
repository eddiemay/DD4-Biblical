import gradio
from PIL import Image
from dss_ocr import image_to_string

filename = 'torah-4-7-4.jpeg'

print(image_to_string(filename))
print(image_to_string(Image.open(filename)))

ui = gradio.Interface(
  image_to_string,
  inputs=gradio.Image(label="Scroll Fragment"),
  outputs=gradio.Textbox(label="Hebrew Text", lines=28))
ui.launch()
