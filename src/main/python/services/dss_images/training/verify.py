import cv2
import Levenshtein
import numpy as np
import pytesseract
import time
from diff_match_patch import diff_match_patch
from dss_ocr import image_to_string
from matplotlib import pyplot as plt
from multiprocessing import Pool
from pytesseract import Output
from scipy import stats
from utility import image_to_boxes_data, post_process_boxes, romanize, unfinalize
from utility import draw_letter_boxes_and_text

titles = [# 'Orginal', 'Gray Scale', 'OTSU', 'OTSU2',
          'Bilateral7', 'Bilateral9', 'Bilateral12', 'Bilateral15',
          'MedBlur3', 'GauBlur3', 'MedThres3', 'GauThres3',
          'MedBlur3g', 'GauBlur3g', 'MedThres3g', 'GauThres3g',
          'MedBlur5', 'GauBlur5', 'MedThres5', 'GauThres5',
          'MedBlur5g', 'GauBlur5g', 'MedThres5g', 'GauThres5g',
          'BiMedBlur3', 'BiGauBlur3', 'BiMedThres3', 'BiGauThres3',
          'BiMedBlur5', 'BiGauBlur5', 'BiMedThres5', 'BiGauThres5',]


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
    ocr = unfinalize(image_to_string(eval['image'], model=eval['model']).strip())
    ld = Levenshtein.distance(txt, ocr)
    percent = round((len(txt) - ld) * 100 / len(txt), 2)
    eval['ocr'], eval['ld'], eval['percent'] = ocr, ld, percent

    return eval


def verify_(name, img_file, txt, model, img_proc, display, multithread):
    result = {'name': name, 'evaluated': []}
    img = cv2.imread(img_file)

    if display:
        cv2.imshow(f'Original Image', img)
        cv2.waitKey(1)

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    otsu = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]
    otsu2 = cv2.threshold(gray, 100, 205, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]
    blf7 = cv2.bilateralFilter(img, 7, 75, 75)
    blf9 = cv2.bilateralFilter(img, 9, 75, 75)
    blf12 = cv2.bilateralFilter(img, 12, 75, 75)
    blf15 = cv2.bilateralFilter(img, 15, 75, 75)
    fmblur3 = cv2.medianBlur(blf15, 3)
    fgblur3 = cv2.GaussianBlur(blf15, [3, 3], sigmaX=30, sigmaY=300)
    fmth3 = cv2.threshold(fmblur3, 135, 255, cv2.THRESH_BINARY)[1]
    fgth3 = cv2.threshold(fgblur3, 132, 255, cv2.THRESH_BINARY)[1]
    fmblur5 = cv2.medianBlur(blf15, 5)
    fgblur5 = cv2.GaussianBlur(blf15, [5, 5], sigmaX=30, sigmaY=300)
    fmth5 = cv2.threshold(fmblur5, 135, 255, cv2.THRESH_BINARY)[1]
    fgth5 = cv2.threshold(fgblur5, 132, 255, cv2.THRESH_BINARY)[1]
    mblur3 = cv2.medianBlur(img, 3)
    gblur3 = cv2.GaussianBlur(img, [3, 3], sigmaX=30, sigmaY=300)
    mth3 = cv2.threshold(mblur3, 135, 255, cv2.THRESH_BINARY)[1]
    gth3 = cv2.threshold(gblur3, 132, 255, cv2.THRESH_BINARY)[1]
    mblur3g = cv2.medianBlur(gray, 3)
    gblur3g = cv2.GaussianBlur(gray, [3, 3], sigmaX=30, sigmaY=300)
    mth3g = cv2.threshold(mblur3g, 145, 255, cv2.THRESH_BINARY)[1] # Best 145
    gth3g = cv2.threshold(gblur3g, 132, 255, cv2.THRESH_BINARY)[1] # Best 132
    mblur5 = cv2.medianBlur(img, 5)
    gblur5 = cv2.GaussianBlur(img, [5, 5], sigmaX=30, sigmaY=300)
    mth5 = cv2.threshold(mblur5, 135, 255, cv2.THRESH_BINARY)[1]
    gth5 = cv2.threshold(gblur5, 132, 255, cv2.THRESH_BINARY)[1]
    mblur5g = cv2.medianBlur(gray, 5)
    gblur5g = cv2.GaussianBlur(gray, [5, 5], sigmaX=30, sigmaY=300)
    mth5g = cv2.threshold(mblur5g, 135, 255, cv2.THRESH_BINARY)[1]
    gth5g = cv2.threshold(gblur5g, 132, 255, cv2.THRESH_BINARY)[1]

    images = [# img, gray, otsu, otsu2,
              blf7, blf9, blf12, blf15,
              mblur3, gblur3, mth3, gth3,
              mblur3g, gblur3g, mth3g, gth3g,
              mblur5, gblur5, mth5, gth5,
              mblur5g, gblur5g, mth5g, gth5g,
              fmblur3, fgblur3, fmth3, fgth3,
              fmblur5, fgblur5, fmth5, fgth5]

    if img_proc is not None:
        i = titles.index(img_proc)
        result['evaluated'].append(evaluate(
            {'name': titles[i], 'text': txt, 'image': images[i], 'model': model}))
    else:
        for i in range(len(titles)):
            eval = {'name': titles[i], 'text': txt, 'image': images[i], 'model': model}
            result['evaluated'].append(eval)
            if not multithread:
                evaluate(eval)

        if multithread:
            with Pool() as pool:
                result['evaluated'] = pool.map(evaluate, result['evaluated'])

    best = result['evaluated'][np.argmax(list(map(lambda e: e['percent'], result['evaluated'])))]
    result['best'] = best
    print(f'{name}, {model}, {best["name"]}, {best["ld"]}, {best["percent"]}%')

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

        d = pytesseract.image_to_data(best['image'], lang=model, output_type=Output.DICT)

        word_img = img.copy()
        for i in range(len(d['level'])):
            if d['level'][i] == 5:
                x, y, w, h = d['left'][i], d['top'][i], d['width'][i], d['height'][i]
                cv2.rectangle(word_img, (x, y), (x + w, y + h), (0, 255, 0), 2)
        # cv2.imshow('Word locations',  word_img)

        line_img = img.copy()
        for i in range(len(d['level'])):
            if d['level'][i] == 4:
                x, y, w, h = d['left'][i], d['top'][i], d['width'][i], d['height'][i]
                cv2.rectangle(line_img, (x, y), (x + w, y + h), (0, 255, 0), 2)
        # cv2.imshow('Row locations',  line_img)

        # run tesseract, returning the bounding boxes
        boxes = image_to_boxes_data(best['image'], model=model)
        # cv2.imshow('Letter locations', draw_letter_boxes_and_text(img, boxes))
        cv2.imshow('Post Processed Letter Locations',
                   draw_letter_boxes_and_text(img, post_process_boxes(boxes)))

        cv2.imshow('Best Image', best['image'])
        plt.figure(num=f'{name} {model}')
        for i in range(len(titles)):
            # cv2.imshow(titles[i], images[i])
            plt.subplot(6, 4, i + 1), plt.imshow(images[i], 'gray')
            plt.title(f'{titles[i]} {result["evaluated"][i]["percent"]}%')
            plt.xticks([]), plt.yticks([])
        cv2.waitKey(1)
        plt.show()

    return result


def verify(img_file, txt_file, model='heb', display=False, img_proc=None, multithread=True):
    with open(txt_file, 'r') as f:
        txt = unfinalize(f.read().strip())
    verify_(img_file, img_file, txt, model, img_proc, display, multithread)


def verify_fragment(scroll, fragment, model='heb', display=False, img_proc=None, multithread=True):
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
        f'{scroll}-{fragment}', img_file, unfinalize(txt), model, img_proc, display, multithread)


def verify_frag(column):
    return verify_fragment('isaiah', column + 1, 'Hebrew_Font_Label_14', multithread=False)


def output(output_file, row_title, values):
    print(f'{row_title}: {values}')
    output_file.write(f'{row_title},{','.join(list(map(str, values)))}\n')


if __name__ == '__main__':
    start_time = time.time()
    with Pool() as pool:
        results = sorted(pool.map(verify_frag, range(54)), key=lambda r:r['best']['percent'])
    pool_time = time.time()

    with open('verify.csv', 'w') as csv:
        output(csv, 'Fragment', np.append(titles.copy(), ['Best', 'Best Percent']))
        percents = []
        for result in results:
            rp = np.array(list(map(lambda e:e['percent'], result['evaluated'])))
            percents.append(rp)
            out = rp.copy()
            output(csv, result["name"], np.append(rp.copy(), [result["best"]["name"], result["best"]["percent"]]))
        output(csv, '', [])
        percents = np.array(percents)
        best_percents = np.array(list(map(lambda r:r['best']['percent'], results)))
        best_indexes = np.array(list(map(lambda r:titles.index(r['best']['name']), results)))
        means = np.round(np.mean(percents, axis=0), 2)
        bests_mean = np.round(np.mean(best_percents), 2)
        output(csv, 'Means', np.append(means.copy(), [titles[np.argmax(means)], bests_mean]))
        medians = np.round(np.median(percents, axis=0), 2)
        output(csv, 'Medians', np.append(medians.copy(), [titles[np.argmax(medians)], np.round(np.median(best_percents), 2)]))
        modes = stats.mode(np.round(percents / 5) * 5, axis=0).mode
        output(csv, 'Modes', np.append(modes.copy(), [titles[stats.mode(best_indexes).mode], stats.mode(np.round(best_percents / 5) * 5).mode]))
        stds = np.round(np.std(percents, axis=0), 2)
        bests_std = np.round(np.std(best_percents), 2)
        output(csv, 'Stds', np.append(stds.copy(), [titles[np.argmin(stds)], bests_std]))
        zLows = np.round(means - stds * 3, 2)
        output(csv, 'Z-Lows', np.append(zLows.copy(), [titles[np.argmax(zLows)], np.round(bests_mean - bests_std * 3, 2)]))
        zHighs = np.round(means + stds * 3, 2)
        bests_zScoreHigh = np.round(bests_mean + bests_std * 3, 2)
        output(csv, 'Z-Highs', np.append(zHighs.copy(), [titles[np.argmin(np.absolute(bests_zScoreHigh - zHighs))], bests_zScoreHigh]))
        print(f"Pool time: {pool_time - start_time} seconds")
        print(f"Column comparison time: {time.time() - start_time} seconds\n")

    models = ['heb', 'script/Hebrew', 'Heb_Font', 'Hebrew_Font',
             # 'embedding',
              'Hebrew_Paleo_14', 'heb_Paleo_14', 'Hebrew_Label_13', 'Heb_Label_13',
              'Hebrew_Font_Label_13', 'Hebrew_Font_Label_14']

    for model in models:
        verify_fragment('isaiah', 7, model, model == 'Hebrew_Font_Label_14')

    image_files = ['dss_isa_9_6_7-11.png', 'dss_isa_9_6_7-11_scaled.png', 'dss_isa 9_6_7-11_threshold.png',
                   'dss-isa_6_7-11.tif', 'dss_isa_9_6_7-11_embedded.jpg']
    for img_file in image_files:
        for model in models:
            verify(img_file, 'dss_isa_6_7-11.txt', model, model == 'Hebrew_Font_Label_14')
