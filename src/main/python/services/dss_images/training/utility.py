roman_numerals = {'M':1000, 'CM':900, 'D':500, 'CD':400, 'C':100, 'XC':90,
                  'L':50, 'XL':40, 'X':10, 'IX':9, 'V':5, 'IV':4, 'I':1}


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
