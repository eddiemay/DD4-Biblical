from PIL import Image
import pytesseract

filename = 'torah-4-7-4.jpeg'
# If you don't have tesseract executable in your PATH, include the following:
pytesseract.pytesseract.tesseract_cmd = r'/opt/homebrew/bin/tesseract'
# Example tesseract_cmd = r'C:\Program Files (x86)\Tesseract-OCR\tesseract'

# List of available languages
print(pytesseract.get_languages())

# Simple image to string
print(pytesseract.image_to_string(Image.open(filename), lang='heb'))

# In order to bypass the image conversions of pytesseract, just use relative or
# absolute image pat. NOTE: In this case you should provide tesseract supported
# images or tesseract will return error
print(pytesseract.image_to_string(filename, lang='heb'))

# Batch processing with a single file containing the list of multiple image file
# paths
print(pytesseract.image_to_string('images.txt', lang='heb'))

# Timeout/terminate the tesseract job after a period of time
try:
    # Timeout after 2 seconds
    print(pytesseract.image_to_string(filename, lang='heb', timeout=2))
    # Timeout after half a second
    print(pytesseract.image_to_string(filename, lang='heb', timeout=0.5))
except RuntimeError as timeout_error:
    # Tesseract processing is terminated
    pass

# Get bounding box estimates
print(pytesseract.image_to_boxes(Image.open(filename), lang='heb'))

# Get verbose data including boxes, confidences, line and page numbers
print(pytesseract.image_to_data(Image.open(filename), lang='heb'))

# Get information about orientation and script detection
# print(pytesseract.image_to_osd(Image.open(filename), lang='heb'))

# Get a searchable PDF
pdf = pytesseract.image_to_pdf_or_hocr(filename, lang='heb', extension='pdf')
with open('test.pdf', 'w+b') as f:
    f.write(pdf)  # pdf type is bytes by default

# Get HOCR output
hocr = pytesseract.image_to_pdf_or_hocr(filename, lang='heb', extension='hocr')

# Get ALTO XML output
xml = pytesseract.image_to_alto_xml(filename, lang='heb')

# getting multiple types of output with one call to save compute time
# currently supports mix and match of the following: txt, pdf, hocr, box, tsv
# text, boxes = pytesseract.run_and_get_multiple_output(filename, lang='heb', extensions=['txt', 'box'])
