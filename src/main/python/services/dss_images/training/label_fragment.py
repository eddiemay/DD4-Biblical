import cv2
import json
import pytesseract
from pytesseract import Output
from urllib import request
from verify import verify_fragment
from utility import image_to_boxes_data, post_process_boxes

API_BASE = 'https://dd4-biblical.appspot.com/_api/'
LETTERBOX_BY_FRAGMENT_URL = API_BASE + 'letterBoxs/v1/list?filter=filename={}&pageSize=0'
LETTERBOX_BATCH_DELETE_URL = API_BASE + 'letterBoxs/v1/batchDelete?idToken='
LETTERBOX_BATCH_CREATE_URL = API_BASE + 'letterBoxs/v1/batchCreate?idToken='
session = {}

class Upload:
    ROWS = 1
    LETTERS = 2
    ROWS_AND_LETTERS = 3


def send_json_req(url, data):
    if session.get('id') is None:
        with open('token.id', 'r') as f:
            session['id'] = f.readline()

    json_data = json.dumps(data).encode('utf-8')
    req = request.Request(url + session['id'])
    req.add_header('Content-Type', 'application/json')
    req.add_header('Content-Length', str(len(json_data)))
    print(f'Sending request: {url} with data: {json_data}')
    with request.urlopen(req, json_data) as resp:
        response = json.load(resp)
        print('Response: ', response)
        return response


def label(scroll, fragment, model='fragment', display=True, upload=None):
    filename = f'{scroll}-column-{fragment}'

    result = verify_fragment(scroll, fragment, model, multithread=True)

    img = result['evaluated'][0]['image']
    best_img = result['best']['image']

    d = pytesseract.image_to_data(best_img, lang=model, output_type=Output.DICT)

    rows = []
    line_img = img.copy()
    row_num = 0
    for i in range(len(d['level'])):
        if d['level'][i] == 4:
            row_num += 1
            x,y,w,h = d['left'][i], d['top'][i], d['width'][i], d['height'][i]
            row = {'filename': filename, 'type': 'Row', 'value': row_num,
                   'x1': x, 'y1': y, 'x2': (x + w), 'y2': (y + h)}
            cv2.rectangle(line_img, (x, y), (x + w, y + h), (0, 255, 0), 2)
            rows.append(row)

    if display:
        cv2.imshow("Raw image", img)
        cv2.imshow('Row locations',  line_img)
        print(rows)

    # run tesseract, returning the bounding boxes
    boxes = image_to_boxes_data(best_img, model=model)
    original_count = len(boxes)
    boxes = post_process_boxes(boxes)
    letter_boxes = []
    # Draw the bounding boxes on the image
    for b in boxes:
        letter_box = {'filename': filename, 'type': 'Letter', 'value': b['text'],
                      'x1': b['left'], 'y1': b['top'], 'x2': b['right'], 'y2': b['bottom']}
        letter_boxes.append(letter_box)
        # print(letter_box)
        img = cv2.rectangle(img, [b['left'], b['top']], [b['right'], b['bottom']],
                            [0, 255, 0], 2)

    print(f'Found {original_count} boxes, filtered down to {len(letter_boxes)}')

    if display:
        cv2.imshow(f'Letter locations', img)
        cv2.waitKey(0)

    if upload:
        # Get the list of existing letter boxes, if there are any.
        letterbox_url = LETTERBOX_BY_FRAGMENT_URL.format(filename)
        print('Sending request: ', letterbox_url)
        row_ids = []
        letter_ids = []
        with request.urlopen(letterbox_url) as url:
            response = json.load(url)
            print('Response: ', response)
            letterboxes = response.get('items')
            if letterboxes is not None:
                for letterbox in letterboxes:
                    if letterbox['type'] == 'Row':
                        row_ids.append(letterbox['id'])
                    elif letterbox['type'] == 'Letter':
                        letter_ids.append(letterbox['id'])
            else:
                print(f'No existing letter boxes for {filename}, continuing...')

        if upload & Upload.ROWS:
            # Delete old rows and create the new ones.
            send_json_req(LETTERBOX_BATCH_DELETE_URL, {'items': row_ids})
            # send_json_req(LETTERBOX_BATCH_CREATE_URL, {'items': rows})

        if upload & Upload.LETTERS:
            # Delete old letter boxes and create the new ones.
            send_json_req(LETTERBOX_BATCH_DELETE_URL, {'items': letter_ids})
            send_json_req(LETTERBOX_BATCH_CREATE_URL, {'items': letter_boxes})


if __name__ == '__main__':
    label('isaiah', 16, upload=Upload.ROWS_AND_LETTERS)