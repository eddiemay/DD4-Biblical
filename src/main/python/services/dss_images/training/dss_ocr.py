import pytesseract
import requests, json

# pytesseract.pytesseract.tesseract_cmd = r'/opt/homebrew/bin/tesseract'
TRANSLATE_URL = 'https://dd4-biblical.appspot.com/_api/scriptures/v1/translate'


def image_to_string(image_or_filename, translate=False, model='Hebrew_Font_Embedding_Label_19'):
    model = f"dabar.cloud/{model}" if model.startswith("Heb") else model
    text = pytesseract.image_to_string(image_or_filename, lang=model)
    # print(text)

    translation = ''
    if translate:
        for line in text.splitlines():
            if len(line) > 2:
                print('translating line: ' + line)
                ret = requests.post(TRANSLATE_URL, data={'text': line}).text
                if not ret.startswith('<html>'):
                    response = json.loads(ret)
                    # print(response)
                    for result in response['items']:
                        if 'subTokens' in result:
                            for subToken in result['subTokens']:
                                translation += subToken['translation']
                            translation += ' '
                translation += '\n'

    return text + '\n\n' + translation


if __name__ == '__main__':
    print(image_to_string('dss_isa_9_6_7-11.png'))
    print(image_to_string('dss-isa_6_7-11.tif', lang='Guttman_Stam'))
