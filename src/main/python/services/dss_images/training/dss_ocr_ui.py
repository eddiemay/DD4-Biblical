import gradio
from dss_ocr import image_to_string

filename = 'dss-isa_6_7-11.tif'

def process(file):
  return image_to_string(file, translate=True)


print(process(filename))

ui = gradio.Interface(
    process,
    inputs=gradio.Image(label="Scroll Fragment"),
    outputs=gradio.Textbox(label="Hebrew Text", lines=32))
ui.launch()
