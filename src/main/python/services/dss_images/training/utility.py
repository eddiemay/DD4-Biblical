import cv2
import numpy as np
import pytesseract
from PIL import ImageFont, ImageDraw, Image

roman_numerals = {'M':1000, 'CM':900, 'D':500, 'CD':400, 'C':100, 'XC':90,
                  'L':50, 'XL':40, 'X':10, 'IX':9, 'V':5, 'IV':4, 'I':1}


def image_to_boxes_data(img, lang=None):
    boxes = []
    h = img.shape[0]
    for b in pytesseract.image_to_boxes(img, lang=lang).splitlines():
        b = b.split(' ')
        boxes.append({'text': b[0], 'left': int(b[1]), 'top': h - int(b[4]),
                      'right':  int(b[3]), 'bottom': h - int(b[2])})

    return boxes


def post_process_boxes(boxes):
    mean_height = 30
    processed = []
    prev_box = None
    for x in range(len(boxes)):
        box = boxes[x]
        next_box = boxes[x + 1] if x + 1 < len(boxes) else None
        if box['text'] < 'א' or box['text'] > 'ת':
            print(f'Filtering out {box}')
            continue
        if box['bottom'] - box['top'] < 7:
            box['top'] -= 7
        elif box['bottom'] - box['top'] > mean_height and box['text'] != 'ל':
            box['top'] = box['bottom'] - mean_height
        if box['right'] - box['left'] < 7:
            box['right'] += 7
        if next_box and box['left'] + 7 < next_box['left'] < box['right'] - 7:
            box['right'] = next_box['left']
        if prev_box and box['right'] - 7 > prev_box['right'] > box['left'] + 7:
            box['left'] = prev_box['right']
        processed.append(box)
        prev_box = box

    return processed


def romanize(num):
    roman = ''
    for r, v in roman_numerals.items():
        while num >= v:
            roman += r
            num -= v
    return roman


def unfinalize(text):
    result = ''
    for c in text:
        if c == 'ך':
            result += 'כ'
        elif c == 'ם':
            result += 'מ'
        elif c == 'ן':
            result += 'נ'
        elif c == 'ף':
            result += 'פ'
        elif c == 'ץ':
            result += 'צ'
        elif c == '.':
            result += ' '
        elif c == ' ' or c == '\n' or 'א' <= c <= 'ת':
            result += c
    return result


def draw_letter_boxes(img, boxes, box_colors=None):
    if box_colors is None:
        box_colors = [[0, 255, 0], [255, 0, 0], [0, 0, 255]]

    letter_img = img.copy()
    ci = 0
    for b in boxes:
        cv2.rectangle(letter_img,
                      [b['left'], b['top']], [b['right'], b['bottom']],
                      box_colors[ci], 2)
        ci = (ci + 1) % len(box_colors)

    return letter_img


def draw_letter_text(img, boxes, box_colors=None):
    if box_colors is None:
        box_colors = [(0, 0, 0), (0, 0, 255), (255, 0, 0)]

    image = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    pil_image = Image.fromarray(image)

    # Draw non-ascii text onto image
    font = ImageFont.truetype("DSSPaleo.ttf", 16)
    draw = ImageDraw.Draw(pil_image)
    ci = 0
    for b in boxes:
        draw.text([b['left'] + 7, b['bottom']], b['text'], fill=box_colors[ci], font=font)
        ci = (ci + 1) % len(box_colors)

    # Convert back to Numpy array and switch back from RGB to BGR
    image = np.asarray(pil_image)
    image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)

    return image


def draw_letter_boxes_and_text(img, boxes, box_colors=None):
    return draw_letter_text(
        draw_letter_boxes(img, boxes, box_colors), boxes, box_colors)


if __name__ == '__main__':
    img = cv2.imread('dss_isa_9_6_7-11.png')
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    gblur = cv2.GaussianBlur(gray, [3, 3], sigmaX=30, sigmaY=300)
    otsu = cv2.threshold(gblur, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]

    # run tesseract, returning the bounding boxes
    boxes = image_to_boxes_data(otsu, lang='fragment')
    letter_box_img = draw_letter_boxes(img, boxes)
    pp_boxes = post_process_boxes(boxes)

    # Draw the bounding boxes on the image
    cv2.imshow('Post Processed Letter Boxes and text',
               draw_letter_boxes_and_text(img, pp_boxes))
    cv2.imshow('Post Processed Letter text', draw_letter_text(img, pp_boxes))
    cv2.imshow('Post Processed Letter Box locations', draw_letter_boxes(img, pp_boxes))
    cv2.imshow('Letter Box locations', letter_box_img)
    cv2.waitKey(0)
