import gradio
# from PIL import Image
from dss_ocr import image_to_string

filename = 'Torah-yom-3.png'

print(image_to_string(filename))
# print(image_to_string(Image.open(filename)))

ui = gradio.Interface(
  image_to_string,
  inputs=gradio.Image(label="Scroll Fragment"),
  outputs=gradio.Textbox(label="Hebrew Text", lines=32))
ui.launch()
