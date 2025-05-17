import cv2
import json
import Levenshtein
import numpy as np
import pytesseract
import time

from dask.distributed import Client
from diff_match_patch import diff_match_patch
from dss_ocr import image_to_string
from matplotlib import pyplot as plt
from multiprocessing import Pool
from pytesseract import Output
from scipy import stats
from utility import image_to_boxes_data, post_process_boxes, romanize, unfinalize
from utility import draw_letter_boxes_and_text

BEST_MODEL='Hebrew_Font_Embedding_Label_19'
DASK_SCHEDULER = 'localhost:8786'
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


class Multithread:
    COLUMN_LOCAL = 1
    COLUMN_DISTRIBUTED = 2
    PREPROCESSOR_LOCAL = 3
    PREPROCESSOR_DISTRIBUTED = 4


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


def process_image(img, params):
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

    return img, name


def evaluate(eval):
    txt = eval['text']
    params = eval['parameters']
    img, name = process_image(eval['image'], params)

    ocr = unfinalize(image_to_string(img, model=params['model']).strip())
    ld = Levenshtein.distance(txt, ocr)
    percent = round((len(txt) - ld) * 100 / len(txt), 2)
    eval['name'], eval['image'], eval['ocr'], eval['ld'], eval['percent'] =(
        name, None, ocr, ld, percent)

    return eval


def verify(verify_request):
    name = verify_request['name']
    img = verify_request['image']
    txt = verify_request['text']
    model = verify_request['model']
    display = verify_request['display']
    multithread = verify_request['multithread']
    result = {'name': name}
    evaluated = list(map(
        lambda pp: {'text': txt, 'image': img, 'parameters': pp['parameters']},
        verify_request['preprocessors']))

    if display:
        cv2.imshow(f'Original Image', img)
        cv2.waitKey(1)

    if len(evaluated) == 0:
        for isGray in [False]:
            for bf in [None, 7]:
                for blur in [None, 'median', 'gaussian']:
                    threshold_values = [130, 135, 145] if blur == 'median' else [132]
                    for blur_size in [3, 5] if blur is not None else [None]:
                        for threshold_type in [None, cv2.THRESH_BINARY, cv2.THRESH_BINARY_INV, cv2.THRESH_TRUNC]:
                            for threshold in threshold_values if threshold_type is not None else [None]:
                                evaluated.append(
                                    {'text': txt, 'image': img, 'parameters': {
                                        'model': model, 'gray': isGray, 'bf': bf,
                                        'blur': blur, 'blur_size': blur_size,
                                        'threshold': threshold,
                                        'threshold_type': threshold_type}})

    print(f'Evaluating {name} with {len(evaluated)} preprocessors')
    if multithread == Multithread.PREPROCESSOR_LOCAL:
        with Pool() as pool:
            evaluated = pool.map(evaluate, evaluated)
    elif multithread == Multithread.PREPROCESSOR_DISTRIBUTED:
        client = Client(DASK_SCHEDULER)
        client.upload_file('dss_ocr.py')
        client.upload_file('utility.py')
        evaluated = client.gather(client.map(evaluate, evaluated))
    else:
        evaluated = list(map(evaluate, evaluated))

    best = evaluated[np.argmax(list(map(lambda e: e['percent'], evaluated)))]
    result['best'] = best
    print(f'{name}, {best["parameters"]["model"]}, {best["name"]}, {best["ld"]}, {best["percent"]}%')
    top = list(reversed(sorted(evaluated, key=lambda r:r['percent'])))[:7]
    result['top'] = top

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
        model = best['parameters']['model']
        model = f"dabar.cloud/{model}" if model.startswith("Heb") else model
        best_image, _ = process_image(img, best['parameters'])
        d = pytesseract.image_to_data(best_image, lang=model, output_type=Output.DICT)

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
        boxes = image_to_boxes_data(best_image, model=model)
        # cv2.imshow('Letter locations', draw_letter_boxes_and_text(img, boxes))
        cv2.imshow('Post Processed Letter Locations',
                   draw_letter_boxes_and_text(img, post_process_boxes(boxes)))

        cv2.imshow('Best Image', best_image)
        plt.figure(num=f'{name} {model}')
        for i in range(6):
            # cv2.imshow(sr[i]['name'], sr[i]['image'])
            plt.subplot(3, 2, i + 1), plt.imshow(process_image(img, top[i]['parameters'])[0])
            plt.title(f'{top[i]['name']} {top[i]["percent"]}%')
            plt.xticks([]), plt.yticks([])
        cv2.waitKey(1)
        plt.show()

    return result


def to_verify_request(name, img_file, txt, model=None, multithread=Multithread.PREPROCESSOR_LOCAL, use_best=True, display=False):
    img = cv2.imread(img_file)
    preprocessors = []

    if use_best:
        with open('verify_best_preprocessors.json', 'r') as f:
            for l in f:
                preprocessor = json.loads(l)
                if model is not None:
                    preprocessor['parameters']['model'] = model
                preprocessors.append(preprocessor)


    return {'name': name, 'image': img, 'text': txt, 'model': model,
            'display': display, 'multithread': multithread, 'preprocessors': preprocessors}


def to_isa_verify_request(column, model=None, multithread=Multithread.PREPROCESSOR_LOCAL, use_best=True, display=False):
    txt_file = '../books/1Q_Isaiah_a.txt'
    roman_numeral = romanize(column)
    with open(txt_file, 'r') as f:
        lines = f.readlines()
        txt = ''
        l = 0
        while not lines[l].startswith(f'Col. {roman_numeral},'):
            l += 1
        while l + 1 < len(lines) and not lines[l+1].startswith('Col. '):
            l += 1
            txt += lines[l].strip() + '\n'

    return to_verify_request(
        f'isaiah-{column}', f'../images/isaiah/columns/column_9_{column}.jpg',
        unfinalize(txt), model, multithread, use_best, display)


def output(output_file, row_title, values):
    print(f'{row_title}: {list(map(str, values))}')
    output_file.write(f'{row_title},{",".join(list(map(str, values)))}\n')


def output_column_stats(model=None, use_best=False, multithread=Multithread.COLUMN_LOCAL):
    start_time = time.time()
    # Load the best by fragment from file.
    bests_by_fragment = {}
    with open('verify_best_by_fragment.json', "r", encoding="utf-8") as f:
        for line in f:
            j = json.loads(line)
            bests_by_fragment[j.get('fragment')] = j['bests']

    requests = list(map(
        lambda c:to_isa_verify_request(c + 1, model, multithread, use_best),
        range(54)))

    if multithread == Multithread.COLUMN_LOCAL:
        with Pool() as pool:
            results = pool.map(verify, requests)
    elif multithread == Multithread.COLUMN_DISTRIBUTED:
        client = Client(DASK_SCHEDULER)
        client.upload_file('dss_ocr.py')
        client.upload_file('utility.py')
        results = client.gather(client.map(verify, requests))
    else:
        results = list(map(verify, requests))

    results = sorted(results, key=lambda r:r['best']['percent'])
    pool_time = time.time()

    decimals = 2
    overall_bests = []
    with open('verify.csv', 'w') as csv:
        output(csv, 'Fragment', ['Percent', 'Best'])
        for result in results:
            # Find the entry for this fragment.
            by_fragment = bests_by_fragment.get(result['name'])
            by_name = {}
            if by_fragment is None:
                by_fragment = []
            else:
                for best in by_fragment:
                    by_name[best['preprocessor_name']] = best
            # Append the 7 best from this run and skip any that are already part of the set.
            for eval in result['top'][:7]:
                if by_name.get(eval['name']) is None:
                    by_fragment.append({
                        'preprocessor_name': eval['name'],
                        'percent': eval['percent'],
                        'parameters': eval['parameters']
                    })
                elif by_name[eval['name']]['percent'] <= eval['percent']:
                    by_name[eval['name']]['percent'] = eval['percent']
                    by_name[eval['name']]['parameters'] = eval['parameters']

            # Sort the results from greatest percentage and only keep the top 7.
            bests_by_fragment[result['name']] = list(
                reversed(sorted(by_fragment, key=lambda r:r['percent'])))[:7]
            overall_best = bests_by_fragment.get(result['name'])[0]
            overall_bests.append(overall_best['percent'])
            output(csv, result["name"],
                   [result["best"]["percent"], result["best"]["name"],
                    overall_best["percent"], overall_best["preprocessor_name"], overall_best['parameters']['model']])
        output(csv, '', [])
        percents = np.array(list(map(lambda r:r['best']['percent'], results)))
        overall_bests = np.array(overall_bests)
        mean = np.round(np.mean(percents), decimals)
        overall_mean = np.round(np.mean(overall_bests), decimals)
        output(csv, 'Mean', [mean, overall_mean])
        output(csv, 'Median', [np.round(np.median(percents), decimals), np.round(np.median(overall_bests), decimals)])
        output(csv, 'Mode', [stats.mode(np.round(percents / 5) * 5).mode, stats.mode(np.round(overall_bests / 5) * 5).mode])
        std = np.round(np.std(percents), decimals)
        overall_std = np.round(np.std(overall_bests), decimals)
        output(csv, 'Std', [std, overall_std])
        output(csv, 'Z-Low', [np.round(mean - std * 3, decimals), np.round(overall_mean - overall_std * 3, decimals)])
        output(csv, 'Z-High', [np.round(mean + std * 3, decimals), np.round(overall_mean + overall_std * 3, decimals)])
        print(f"Pool time: {pool_time - start_time} seconds")
        print(f"Column comparison time: {time.time() - start_time} seconds\n")

    if not use_best:
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
    # output_column_stats(use_best=False, model=BEST_MODEL, multithread=Multithread.COLUMN_DISTRIBUTED)

    models = ['heb', 'script/Hebrew', 'Heb_Font', 'Hebrew_Font',
              'Heb_Embedding', 'Hebrew_Embedding', 'Hebrew_Font_Embedding',
              'Hebrew_Paleo_14', 'Heb_Paleo_14', 'Hebrew_Label_13', 'Heb_Label_13',
              'Hebrew_Font_Label_13', 'Hebrew_Font_Label_14',
              'Hebrew_Font_Embedding_Label_14', 'Hebrew_Font_Embedding_Label_17', BEST_MODEL]

    for fragment in [16, 7, 48, 1, 54]:
        print(f'\nIsaiah-{fragment}')
        for model in ['Heb_Embedding', 'Hebrew_Embedding', 'Hebrew_Font_Embedding', 'Hebrew_Font_Label_14', 'Hebrew_Font_Embedding_Label_14', 'Hebrew_Font_Embedding_Label_17', BEST_MODEL]:
            verify(to_isa_verify_request(fragment, model, use_best=True, display=model == BEST_MODEL))

    image_files = ['dss_isa_9_6_7-11.png', 'dss_isa_9_6_7-11_scaled.png',
                   'dss_isa_9_6_7-11_threshold.png', 'dss-isa_6_7-11.tif',
                   'dss_isa_9_6_7-11_embedded.jpg']
    with open('dss_isa_6_7-11.txt', 'r') as f:
        txt = unfinalize(f.read().strip())
    for img_file in image_files:
        for model in ['Hebrew_Font_Label_14', 'Hebrew_Font_Embedding_Label_14', BEST_MODEL]:
            verify(to_verify_request(img_file, img_file, txt, model, display=model == BEST_MODEL))
