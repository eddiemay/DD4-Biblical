import pytesseract
import requests, json

# pytesseract.pytesseract.tesseract_cmd = r'/opt/homebrew/bin/tesseract'
TRANSLATE_URL = 'https://dd4-biblical.appspot.com/_api/scriptures/v1/translate'


def image_to_string(image_or_filename, translate=False, lang='heb'):
    text = pytesseract.image_to_string(image_or_filename, lang=lang)
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
    print(image_to_string('Torah-4-yom-3.png'))
    print(image_to_string('Torah-4-yom-3.png', lang='Guttman_Stam'))
