import cv2
import Levenshtein
import numpy as np
import pytesseract
from diff_match_patch import diff_match_patch
from dss_ocr import image_to_string
from matplotlib import pyplot as plt
from multiprocessing import Pool
from pytesseract import Output
from utility import image_to_boxes_data, post_process_boxes, romanize, unfinalize
from utility import draw_letter_boxes_and_text

titles = ['Original', 'Grayscale', 'Med Blur', 'Gau Blur', 'Med Thres',
          'Gau Thres', 'Bilateral', 'OTSU']


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


def diff_line_mode(text1, text2):
    dmp = diff_match_patch()
    diffs = dmp.diff_main(text1, text2)
    return diffs


def evaluate(eval):
    txt = eval['text']
    ocr = unfinalize(image_to_string(eval['image'], lang=eval['lang']).strip())
    ld = Levenshtein.distance(txt, ocr)
    percent = round((len(txt) - ld) * 100 / len(txt), 2)
    eval['ocr'], eval['ld'], eval['percent'] = ocr, ld, percent

    return eval


def verify_(name, img_file, txt, lang, img_proc, display, multithread):
    result = {'name': name, 'evaluated': []}
    img = cv2.imread(img_file)

    if display:
        cv2.imshow(f'Original Image', img)
        cv2.waitKey(1)

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    mblur = cv2.medianBlur(gray, 3)
    gblur = cv2.GaussianBlur(gray, (3,3), sigmaX=30, sigmaY=300)
    mth = cv2.threshold(mblur, 145, 255, cv2.THRESH_BINARY)[1] # Best 145
    gth = cv2.threshold(gblur, 132, 255, cv2.THRESH_BINARY)[1] # Best 132
    blf = cv2.bilateralFilter(gray, 9, 75, 75)
    otsu = cv2.threshold(gblur, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]
    images = [img, gray, mblur, gblur, mth, gth, blf, otsu]

    if img_proc is not None:
        i = titles.index(img_proc)
        result['evaluated'].append(evaluate(
            {'name': titles[i], 'text': txt, 'image': images[i], 'lang': lang}))
    else:
        for i in range(len(titles)):
            eval = {'name': titles[i], 'text': txt, 'image': images[i], 'lang': lang}
            result['evaluated'].append(eval)
            if not multithread:
                evaluate(eval)

        if multithread:
            with Pool() as pool:
                result['evaluated'] = pool.map(evaluate, result['evaluated'])

    best = result['evaluated'][
        np.argmax(list(map(lambda e: e['percent'], result['evaluated'])))]
    result['best'] = best
    print(f'{name}, {lang}, {best["name"]}, {best["ld"]}, {best["percent"]}%')

    if display:
        print(f'expected:\n{txt}\n{get_letter_counts(txt)}\n'
              f'ocr result:\n{best["ocr"]}\n{get_letter_counts(best["ocr"])}\n')
        diffs = diff_line_mode(best['ocr'], txt)
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

        d = pytesseract.image_to_data(best['image'], lang=lang, output_type=Output.DICT)

        word_img = img.copy()
        for i in range(len(d['level'])):
            if d['level'][i] == 5:
                x, y, w, h = (
                    d['left'][i], d['top'][i], d['width'][i], d['height'][i])
                cv2.rectangle(word_img, (x, y), (x + w, y + h), (0, 255, 0), 2)
        # cv2.imshow('Word locations',  word_img)

        line_img = img.copy()
        for i in range(len(d['level'])):
            if d['level'][i] == 4:
                x, y, w, h = (
                    d['left'][i], d['top'][i], d['width'][i], d['height'][i])
                cv2.rectangle(line_img, (x, y), (x + w, y + h), (0, 255, 0), 2)
        # cv2.imshow('Row locations',  line_img)

        # run tesseract, returning the bounding boxes
        boxes = post_process_boxes(image_to_boxes_data(best['image'], lang=lang))
        cv2.imshow('Letter locations', draw_letter_boxes_and_text(img, boxes))

        for i in range(len(titles)):
            # cv2.imshow(titles[i], images[i])
            plt.subplot(3, 3, i + 1), plt.imshow(images[i], 'gray')
            plt.title(f'{titles[i]} {result["evaluated"][i]["percent"]}%')
            plt.xticks([]), plt.yticks([])
        cv2.waitKey(1)
        plt.show()

    return result


def verify(img_file, txt_file, lang='heb', display=False, img_proc=None, multithread=True):
    with open(txt_file, 'r') as f:
        txt = unfinalize(f.read().strip())
    verify_(img_file, img_file, txt, lang, img_proc, display, multithread)


def verify_fragment(scroll, fragment, lang='heb', display=False, img_proc=None, multithread=True):
    img_file= f'../images/{scroll}/columns/column_9_{fragment}.jpg'
    txt_file = '../books/1Q_Isaiah_a.txt'
    roman_numeral = romanize(fragment)
    with open(txt_file, 'r') as f:
        lines = f.readlines()
        txt = ''
        l = 0
        while not lines[l].startswith(f'Col. {roman_numeral},'):
            l += 1
        while l + 1 < len(lines) and not lines[l+1].startswith('Col. '):
            l += 1
            txt += lines[l].strip() + '\n'

    return verify_(
        f'{scroll}-{fragment}', img_file, unfinalize(txt), lang, img_proc, display, multithread)


def verify_frag(column):
    return verify_fragment('isaiah', column + 1, 'fragment', multithread=False)


if __name__ == '__main__':
    with Pool() as pool:
        results = sorted(pool.map(verify_frag, range(8)),
                        key=lambda r: r['best']['percent'])

    print(f'\n{titles}')
    percents = []
    for result in results:
        rp = list(map(lambda e: e['percent'], result['evaluated']))
        percents.append(rp)
        print(f'{result["name"]}: {rp}, {result["best"]["name"]}, {result["best"]["percent"]}%')
    best_percents = list(map(lambda r:r['best']['percent'], results))
    means = np.round(np.mean(percents, axis=0), 2)
    print(f'Means:  \t{means}, {titles[np.argmax(means)]}, {np.round(np.mean(best_percents))}%')
    medians = np.round(np.median(percents, axis=0), 2)
    print(f'Medians:\t{medians}, {titles[np.argmax(medians)]}, {np.round(np.median(best_percents))}%\n')

    langs = ['heb', 'DSS_Paleo', 'embedding', 'fragment']
    for lang in langs:
        verify(
            'dss_isa_9_6_7-11.png', 'dss_isa_6_7-11.txt', lang, lang == 'fragment')

    for lang in langs:
        verify_fragment('isaiah', 48, lang, lang == 'fragment')

    image_files = [
        'dss_isa_9_6_7-11_scaled.png', 'dss_isa 9_6_7-11_threshold.png',
        'dss-isa_6_7-11.tif', 'dss_isa_9_6_7-11_embedded.jpg']
    for img_file in image_files:
        for lang in langs:
            verify(img_file, 'dss_isa_6_7-11.txt', lang, lang == 'fragment')
