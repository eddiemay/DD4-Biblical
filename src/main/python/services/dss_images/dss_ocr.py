from PIL import Image
import gradio
import pytesseract
import requests, json

filename = 'torah-4-7-4.jpeg'
pytesseract.pytesseract.tesseract_cmd = r'/opt/homebrew/bin/tesseract'
TRANSLATE_URL = 'https://dd4-biblical.appspot.com/_api/scriptures/v1/translate'


def image_to_string(image_or_filename):
  text = pytesseract.image_to_string(image_or_filename, lang='heb')
  print(text)
  translation = ''
  for line in text.splitlines():
    if line != '':
      response = json.loads(requests.post(TRANSLATE_URL, data={'text': line}).text)
      print(response)
      for result in response['items']:
        if 'subTokens' in result:
          for subToken in result['subTokens']:
            translation += subToken['translation']
          translation += ' '
    translation += '\n'

  return text + '\n\n' + translation


print(image_to_string(filename))
print(image_to_string(Image.open(filename)))

ui = gradio.Interface(
  image_to_string,
  inputs=gradio.Image(label="Scroll Fragment"),
  outputs=gradio.Textbox(label="Hebrew Text", lines=28))
ui.launch()