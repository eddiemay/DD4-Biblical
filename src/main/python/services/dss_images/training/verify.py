import cv2
import json
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

BEST_MODEL='Hebrew_Font_Embedding_Label_19'
output_all = False
threshold_names = {
    cv2.THRESH_BINARY: 'THRESH_BINARY',
    cv2.THRESH_BINARY_INV: 'THRESH_BINARY_INV',
    cv2.THRESH_TRUNC: 'THRESH_TRUNC',
    cv2.THRESH_TOZERO: 'THRESH_TOZERO',
    cv2.THRESH_TOZERO_INV: 'THRESH_TOZERO_INV',
    cv2.THRESH_MASK: 'THRESH_MASK',
    cv2.THRESH_OTSU: 'THRESH_OTSU',
    cv2.THRESH_TRIANGLE: 'THRESH_TRIANGLE'}


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
    img = eval['image']
    params = eval['parameters']
    name = ''
    if params['gray']:
        img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        name += 'gray'
    if params['bf'] is not None:
        img = cv2.bilateralFilter(img, params['bf'], 75, 75)
        name += f'-bf{params['bf']}'
    if params['blur'] == 'median':
        img = cv2.medianBlur(img, params['blur_size'])
        name += f'-median{params['blur_size']}'
    elif params['blur'] == 'gaussian':
        img = cv2.GaussianBlur(img, [params['blur_size'], params['blur_size']], sigmaX=30, sigmaY=300)
        name += f'-gaussian{params['blur_size']}'
    if params['threshold_type'] is not None:
        threshold_type = params['threshold_type']
        img = cv2.threshold(img, params['threshold'], 255, threshold_type)[1]
        name += f'-{threshold_names[threshold_type]}_{params['threshold']}'

    ocr = unfinalize(image_to_string(img, model=params['model']).strip())
    ld = Levenshtein.distance(txt, ocr)
    percent = round((len(txt) - ld) * 100 / len(txt), 2)
    eval['name'], eval['image'], eval['ocr'], eval['ld'], eval['percent'] =(
        name, img, ocr, ld, percent)

    return eval


def verify_(name, img_file, txt, model, display, multithread, use_best):
    result = {'name': name, 'evaluated': []}
    img = cv2.imread(img_file)

    if display:
        cv2.imshow(f'Original Image', img)
        cv2.waitKey(1)

    if use_best:
        with open('verify_best_preprocessors.json', 'r') as f:
            for l in f:
                js = json.loads(l)
                result['evaluated'].append(
                    {'text': txt, 'image': img, 'parameters': js['parameters']})
    else:
        for isGray in [False, True]:
            for bf in [None, 7, 14, 21, 28, 35]:
                for blur in [None, 'median', 'gaussian']:
                    threshold_values = [130, 135, 145] if blur == 'median' else [132]
                    for blur_size in [3, 5] if blur is not None else [None]:
                        for threshold_type in [None, cv2.THRESH_BINARY, cv2.THRESH_BINARY_INV, cv2.THRESH_TRUNC]:
                            for threshold in threshold_values if threshold_type is not None else [None]:
                                result['evaluated'].append(
                                    {'text': txt, 'image': img, 'parameters': {
                                        'model': model, 'gray': isGray, 'bf': bf,
                                        'blur': blur, 'blur_size': blur_size,
                                        'threshold': threshold,
                                        'threshold_type': threshold_type}})

    print(f'Evaluating {name} with {len(result['evaluated'])} preprocessors')
    if multithread:
        with Pool() as pool:
            result['evaluated'] = pool.map(evaluate, result['evaluated'])
    else:
        result['evaluated'] = list(map(evaluate, result['evaluated']))

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
        sr = list(
            reversed(sorted(result['evaluated'], key=lambda r:r['percent'])))
        for i in range(6):
            # cv2.imshow(sr[i]['name'], sr[i]['image'])
            plt.subplot(3, 2, i + 1), plt.imshow(sr[i]['image'])
            plt.title(f'{sr[i]['name']} {sr[i]["percent"]}%')
            plt.xticks([]), plt.yticks([])
        cv2.waitKey(1)
        plt.show()

    return result


def verify(img_file, txt_file, model='heb', display=False, multithread=True, use_best=True):
    with open(txt_file, 'r') as f:
        txt = unfinalize(f.read().strip())
    verify_(img_file, img_file, txt, model, display, multithread, use_best)


def verify_fragment(scroll, fragment, model='heb', display=False, multithread=True, use_best=True):
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

    return verify_(f'{scroll}-{fragment}', img_file, unfinalize(txt),
                   model, display, multithread, use_best)


def verify_frag(column):
    return verify_fragment('isaiah', column + 1, BEST_MODEL,
                           multithread=False, use_best=False)


def output(output_file, row_title, values, best_values):
    print(f'{row_title}:{' ,' + values if output_all else ''} {list(map(str, best_values))}')
    output_file.write(f'{row_title}{',' + ','.join(list(map(str, values))) if output_all else ''},{','.join(list(map(str, best_values)))}\n')


def output_column_stats(model=BEST_MODEL, use_best=False, use_column_multithread=True):
    start_time = time.time()
    # Load the best by fragment from file.
    bests_by_fragment = {}
    with open('verify_best_by_fragment.json', "r", encoding="utf-8") as f:
        for line in f:
            j = json.loads(line)
            bests_by_fragment[j.get('fragment')] = j['bests']

    if use_column_multithread and not use_best:
        with Pool() as pool:
            results = pool.map(verify_frag, range(54))
    else:
        results = list(map(
            lambda c:verify_fragment('Isaiah', c + 1, model, use_best=use_best), range(54)
        ))
    results = sorted(results, key=lambda r:r['best']['percent'])
    pool_time = time.time()

    titles = list(map(lambda e:e['name'], results[0]['evaluated']))

    with open('verify.csv', 'w') as csv:
        output(csv, 'Fragment', titles, ['Best', 'Best Percent'])
        percents = []
        for result in results:
            rp = np.array(list(map(lambda e:e['percent'], result['evaluated'])))
            percents.append(rp)
            output(csv, result["name"], rp, [result["best"]["name"], result["best"]["percent"]])
        output(csv, '', [], [])
        percents = np.array(percents)
        best_percents = np.array(list(map(lambda r:r['best']['percent'], results)))
        best_indexes = np.array(list(map(lambda r:titles.index(r['best']['name']), results)))
        means = np.round(np.mean(percents, axis=0), 2)
        bests_mean = np.round(np.mean(best_percents), 2)
        output(csv, 'Means',means, [titles[np.argmax(means)], bests_mean])
        medians = np.round(np.median(percents, axis=0), 2)
        output(csv, 'Medians', medians, [titles[np.argmax(medians)], np.round(np.median(best_percents), 2)])
        modes = stats.mode(np.round(percents / 5) * 5, axis=0).mode
        output(csv, 'Modes', modes, [titles[stats.mode(best_indexes).mode], stats.mode(np.round(best_percents / 5) * 5).mode])
        stds = np.round(np.std(percents, axis=0), 2)
        bests_std = np.round(np.std(best_percents), 2)
        output(csv, 'Stds', stds, [titles[np.argmin(stds)], bests_std])
        zLows = np.round(means - stds * 3, 2)
        output(csv, 'Z-Lows', zLows, [titles[np.argmax(zLows)], np.round(bests_mean - bests_std * 3, 2)])
        zHighs = np.round(means + stds * 3, 2)
        bests_zScoreHigh = np.round(bests_mean + bests_std * 3, 2)
        output(csv, 'Z-Highs', zHighs, [titles[np.argmin(np.absolute(bests_zScoreHigh - zHighs))], bests_zScoreHigh])
        print(f"Pool time: {pool_time - start_time} seconds")
        print(f"Column comparison time: {time.time() - start_time} seconds\n")

    if not use_best:
        # Save the best parameters for each scroll.
        for result in results:
            # Find the entry for this fragment.
            by_fragment = bests_by_fragment.get(result['name'])
            preprocessor_names = {}
            if by_fragment is None:
                by_fragment = []
            else:
                for best in by_fragment:
                    preprocessor_names[best['preprocessor_name']] = 1
            # Append the 7 best from this run and skip any that are already part of the set.
            for eval in list(reversed(sorted(result['evaluated'], key=lambda r:r['percent'])))[:7]:
                if preprocessor_names.get(eval['name']) is None:
                    by_fragment.append({
                        'preprocessor_name': eval['name'],
                        'percent': eval['percent'],
                        'parameters': eval['parameters']
                    })
            # Sort the results from greatest percentage and only keep the top 7.
            bests_by_fragment[result['name']] = list(
                reversed(sorted(by_fragment, key=lambda r:r['percent'])))[:7]

        bests = {}
        with open('verify_best_by_fragment.json', "w", encoding="utf-8") as f:
            for b in sorted(bests_by_fragment):
                json.dump({'fragment': b, 'bests': bests_by_fragment[b]}, f)
                f.write("\n")

                for best in bests_by_fragment[b]:
                    name = best['preprocessor_name']
                    if bests.get(name) is None:
                        bests[name] = {'preprocessor_name': name, 'count': 1,
                                       'parameters': best['parameters']}
                    else:
                        bests[name]['count'] = bests[name]['count'] + 1

        with open('verify_best_preprocessors.json', "w", encoding="utf-8") as f:
            for b in sorted(bests):
                json.dump(bests[b], f)
                f.write("\n")


if __name__ == '__main__':
    output_column_stats(use_best=False)

    models = ['heb', 'script/Hebrew', 'Heb_Font', 'Hebrew_Font',
              'Heb_Embedding', 'Hebrew_Embedding', 'Hebrew_Font_Embedding',
              'Hebrew_Paleo_14', 'heb_Paleo_14', 'Hebrew_Label_13', 'Heb_Label_13',
              'Hebrew_Font_Label_13', 'Hebrew_Font_Label_14',
              'Hebrew_Font_Embedding_Label_14', 'Hebrew_Font_Embedding_Label_17', BEST_MODEL]

    for fragment in [16, 7, 48, 1, 54]:
        print(f'\nIsaiah-{fragment}')
        for model in ['Heb_Embedding', 'Hebrew_Embedding', 'Hebrew_Font_Embedding', 'Hebrew_Font_Label_14', 'Hebrew_Font_Embedding_Label_14', 'Hebrew_Font_Embedding_Label_17', BEST_MODEL]:
            verify_fragment('isaiah', fragment, model, model == BEST_MODEL, use_best=False)

    image_files = ['dss_isa_9_6_7-11.png', 'dss_isa_9_6_7-11_scaled.png',
                   'dss_isa_9_6_7-11_threshold.png', 'dss-isa_6_7-11.tif',
                   'dss_isa_9_6_7-11_embedded.jpg']
    for img_file in image_files:
        for model in ['Hebrew_Font_Label_14', 'Hebrew_Font_Embedding_Label_14', BEST_MODEL]:
            verify(img_file, 'dss_isa_6_7-11.txt', model, model == BEST_MODEL)
