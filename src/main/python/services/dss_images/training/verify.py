import cv2
import Levenshtein
import pytesseract
from diff_match_patch import diff_match_patch
from dss_ocr import image_to_string
from pytesseract import Output
from train_by_embedding import unfinalize


class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'


def get_letter_counts(txt):
    counts = {}
    for char in txt:
        if char in counts:
            counts[char] += 1
        else:
            counts[char] = 1

    return sorted(counts.items())


def diff_lineMode(text1, text2):
    dmp = diff_match_patch()
    diffs = dmp.diff_main(text1, text2)
    return diffs


def calc_distance(img_file, txt_file, lang='heb', show_diff=False, display=False):
    with open(txt_file, 'r') as f:
        txt = unfinalize(f.read().strip())
    img = cv2.imread(img_file)
    ocr = unfinalize(image_to_string(img, lang=lang).strip())
    ld = Levenshtein.distance(txt, ocr)
    percent = (len(txt) - ld) * 100 / len(txt)
    print(f'{img_file}, {txt_file}, {lang}, {ld}, {percent}%')
    print(Levenshtein.opcodes(ocr, txt), "\n")
    if display or show_diff:
        print(f'expected:\n{txt}\n{get_letter_counts(txt)}\n'
              f'ocr result:\n{ocr}\n{get_letter_counts(ocr)}\n')
        diffs = diff_lineMode(ocr, txt)
        print(diffs)
        diff_str = ''
        for diff in diffs:
            if diff[0] == -1:
                diff_str += f'{bcolors.FAIL}{diff[1]}{bcolors.ENDC}'
            elif diff[0] == 0:
                diff_str += f'{diff[1]}'
            elif diff[0] == 1:
                diff_str += f'{bcolors.OKGREEN}{diff[1]}{bcolors.ENDC}'
        print(diff_str)

    if display:
        # cv2.imshow('Original Image',  img)
        d = pytesseract.image_to_data(img, lang=lang, output_type=Output.DICT)

        word_img = img.copy()
        for i in range(len(d['level'])):
            if d['level'][i] == 5:
                x, y, w, h = (
                    d['left'][i], d['top'][i], d['width'][i], d['height'][i])
                cv2.rectangle(word_img, (x, y), (x + w, y + h), (0, 255, 0), 2)
        cv2.imshow('Word locations',  word_img)

        line_img = img.copy()
        for i in range(len(d['level'])):
            if d['level'][i] == 4:
                x, y, w, h = (
                    d['left'][i], d['top'][i], d['width'][i], d['height'][i])
                cv2.rectangle(line_img, (x, y), (x + w, y + h), (0, 255, 0), 2)
        cv2.imshow('Row locations',  line_img)

        # run tesseract, returning the bounding boxes
        boxes = pytesseract.image_to_boxes(img, lang=lang)
        # print(boxes)
        h, w, _ = img.shape
        # draw the bounding boxes on the image
        for b in boxes.splitlines():
            b = b.split(' ')
            # if b[0] == 'ע':
            x1, y1, x2, y2 = int(b[1]), h - int(b[2]), int(b[3]), h - int(b[4])
            img = cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 2)
            # print(b)
            # print(f'{x1,y1} {x2,y2}')
        cv2.imshow(f'Letter locations {img_file} {percent}%', img)
        print(f"{bcolors.WARNING}Warning: No active frommets remain. Continue?{bcolors.ENDC}")
        cv2.waitKey(0)


calc_distance('dss_isa_9_6_7-11.png', 'dss_isa_6_7-11.txt')
calc_distance('dss_isa_9_6_7-11.png', 'dss_isa_6_7-11.txt', 'DSS_Paleo',
              display=False)
calc_distance('dss_isa_9_6_7-11.png', 'dss_isa_6_7-11.txt', 'embedding',
              display=True)

calc_distance('dss_isa_9_6_7-11_scaled.png', 'dss_isa_6_7-11.txt')
calc_distance('dss_isa_9_6_7-11_scaled.png', 'dss_isa_6_7-11.txt', 'DSS_Paleo',
              display=False)
calc_distance('dss_isa_9_6_7-11_scaled.png', 'dss_isa_6_7-11.txt', 'embedding',
              display=True)

calc_distance('dss_isa 9_6_7-11_threshold.png', 'dss_isa_6_7-11.txt')
calc_distance('dss_isa 9_6_7-11_threshold.png', 'dss_isa_6_7-11.txt',
              'DSS_Paleo', display=False) # 148 50.34% ->
calc_distance('dss_isa 9_6_7-11_threshold.png', 'dss_isa_6_7-11.txt',
              'embedding', display=True)

calc_distance('dss-isa_6_7-11.tif', 'dss_isa_6_7-11.txt')
calc_distance('dss-isa_6_7-11.tif', 'dss_isa_6_7-11.txt', 'DSS_Paleo',
              display=False)
calc_distance('dss-isa_6_7-11.tif', 'dss_isa_6_7-11.txt', 'embedding',
              display=True)

calc_distance('dss_isa_9_6_7-11_embedded.jpg', 'dss_isa_6_7-11.txt')
calc_distance('dss_isa_9_6_7-11_embedded.jpg', 'dss_isa_6_7-11.txt', 'DSS_Paleo',
              display=False)
calc_distance('dss_isa_9_6_7-11_embedded.jpg', 'dss_isa_6_7-11.txt', 'embedding',
              display=True)

# dss_isa_9_6_7-11.png, dss_isa_6_7-11.txt, heb, 169, 43.288590604026844%
# dss_isa_9_6_7-11.png, dss_isa_6_7-11.txt, DSS_Paleo, 208, 30.201342281879196%
# dss_isa_9_6_7-11_scaled.png, dss_isa_6_7-11.txt, heb, 197, 33.89261744966443%
# dss_isa_9_6_7-11_scaled.png, dss_isa_6_7-11.txt, DSS_Paleo, 188, 36.91275167785235%
# dss_isa 9_6_7-11_threshold.png, dss_isa_6_7-11.txt, heb, 174, 41.61073825503356%
# dss_isa 9_6_7-11_threshold.png, dss_isa_6_7-11.txt, DSS_Paleo, 148, 50.33557046979866%
# dss-isa_6_7-11.tif, dss_isa_6_7-11.txt, heb, 121, 59.395973154362416%
# dss-isa_6_7-11.tif, dss_isa_6_7-11.txt, DSS_Paleo, 32, 89.26174496644295%

# dss_isa_9_6_7-11.png, dss_isa_6_7-11.txt, heb, 169, 43.288590604026844%
# dss_isa_9_6_7-11.png, dss_isa_6_7-11.txt, DSS_Paleo, 167, 43.95973154362416%
# dss_isa_9_6_7-11_scaled.png, dss_isa_6_7-11.txt, heb, 197, 33.89261744966443%
# dss_isa_9_6_7-11_scaled.png, dss_isa_6_7-11.txt, DSS_Paleo, 147, 50.671140939597315%
# dss_isa 9_6_7-11_threshold.png, dss_isa_6_7-11.txt, heb, 174, 41.61073825503356%
# dss_isa 9_6_7-11_threshold.png, dss_isa_6_7-11.txt, DSS_Paleo, 126, 57.718120805369125%
# dss-isa_6_7-11.tif, dss_isa_6_7-11.txt, heb, 121, 59.395973154362416%
# dss-isa_6_7-11.tif, dss_isa_6_7-11.txt, DSS_Paleo, 5, 98.32214765100672%

