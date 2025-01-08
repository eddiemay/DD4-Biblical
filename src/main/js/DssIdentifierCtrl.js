com.digitald4.biblical.DssIdentifierCtrl = function($http, $scope, $window, letterBoxService) {
  this.$http = $http;
  this.$scope = $scope;
  this.$window = $window;
  this.letterBoxService = letterBoxService;
  this.showLetterBoxes = true;
  this.rowNum = 1;
  this.scrolls = {
    ISAIAH: {name: 'Isaiah', filename: 'isaiah', columns: 54, res: 9, textFile: '1Q_Isaiah_a.txt'},
    COMMUNITY_RULE: {name: 'Community Rule', filename: 'community', columns: 11, res: 7},
    WAR_SCROLL: {name: 'War Scroll', filename: 'war', columns: 15, res: 8},
    TEMPLE: {name: 'Temple Scroll', filename: 'temple', columns: 67, res: 9},
    HABAKKUK: {name: 'Commentary on Habakkuk', filename: 'habakkuk', columns: 14, res: 7},
    TORAH: {name: 'Torah Scroll', filename: 'torah', columns: 4, res: 4},
  };
  this.letters = {
    alef: {value: 'א', width: 26, height: 26}, bet: {value: 'ב', width: 26, height: 26},
    gimel: {value: 'ג', width: 26, height: 26}, dalet: {value: 'ד', width: 26, height: 26},
    hey: {value: 'ה', width: 26, height: 26}, waw: {value: 'ו', width: 13, height: 26},
    zayin: {value: 'ז', width: 26, height: 26}, chet: {value: 'ח', width: 26, height: 26},
    tet: {value: 'ט', width: 26, height: 26}, yod: {value: 'י', width: 18, height: 26},
    kaf: {value: 'כ', width: 26, height: 26}, lamed: {value: 'ל', width: 20, height: 55},
    mum: {value: 'מ', width: 26, height: 26}, nun: {value: 'נ', width: 26, height: 26},
    samekh: {value: 'ס', width: 26, height: 26}, ayin: {value: 'ע', width: 26, height: 26},
    pay: {value: 'פ', width: 26, height: 26}, tzadi: {value: 'צ', width: 26, height: 26},
    qof: {value: 'ק', width: 26, height: 26}, resh: {value: 'ר', width: 26, height: 26},
    shin: {value: 'ש', width: 26, height: 26}, tav: {value: 'ת', width: 26, height: 26},
  };

  this.tabs = {
    textFile: {name: "Logo's Text", isEnabled: () => this.textFile != undefined},
    resultText: {name: 'Text Result', isEnabled: () => true},
    textDiff: {name: 'Text Diff', isEnabled: () => this.textFile != undefined},
    imagesByLetter: {name: 'Images By Letter', isEnabled: () => true},
  }

  this.textDiffs = [
    {rowNum: 1, expected: 'ABC', actual: 'ACDC'},
    {rowNum: 2, expected: 'William', actual: 'Willy'},
    {rowNum: 3, expected: 'Mr StealYourGirl', actual: 'Mr still your girl'}
  ];

  this.textFile = "Please select a scroll and fragment."

  this.textDiffs.forEach(textDiff => {
    var diffMatch = new diff_match_patch();
    textDiff.diff = diffMatch.diff_main(textDiff.expected, textDiff.actual);
    textDiff.diff.forEach(diff => {
      if (diff['0'] == -1) {
        diff._class = 'diff-delete';
      } else if (diff['0'] == 1) {
        diff._class = 'diff-insert';
      }
    });
  });

  var canvas = document.getElementById("scroll_view");
  if (!canvas.getContext) {
    alert('Canvas not supported, will not be able to view scroll');
    return;
  }

  this.addEventListeners();

  canvas.addEventListener('click', event => {
    // event.preventDefault();
    this.saveSelected();

    var rect = canvas.getBoundingClientRect();
    var x = event.x - rect.left,
        y = event.y - rect.top;

    if (this.showLetterBoxes) {
      for (var b = 0; b < this.letterBoxes.length; b++)  {
        var lx = x > this.canvasWidth / 2 ? x - this.canvasWidth / 2 : x;
        var ly = x > this.canvasWidth / 2 ? y + 20 : y;
        var letterBox = this.letterBoxes[b];
        if (letterBox.x1 < lx && letterBox.x2 > lx && letterBox.y1 < ly && letterBox.y2 > ly) {
          this.selectedBox = letterBox;
          this.drawScroll();
          return;
        }
      }
    }
    if (this.showRowBoxes) {
      for (var b = 0; b < this.rows.length; b++)  {
        var rowBox = this.rows[b];
        if (rowBox.x1 < x && rowBox.x2 > x && rowBox.y1 < y && rowBox.y2 > y) {
          this.selectedBox = rowBox;
          this.drawScroll();
          return;
        }
      }
    }

    if (x > this.canvasWidth / 2) {
      this.selectedBox = undefined;
      this.drawScroll();
      return;
    }

    this.letterDialogShown = true;
    this.x = x;
    this.y = y;
    this.dialogStyle = {top: $window.visualViewport.pageTop - 20};
    $scope.$apply();
    this.drawScroll();
  });

  canvas.addEventListener('dblclick', event => {
    event.preventDefault();
    if (!this.selectedBox) {
      return;
    }

    this.letterDialogShown = true;
    this.dialogStyle = {top: $window.visualViewport.pageTop - 20};
    this.$scope.$apply();
  });

  document.addEventListener('keydown', event => {
    if (["Space","ArrowUp","ArrowDown","ArrowLeft","ArrowRight"].indexOf(event.key) > -1) {
      event.preventDefault();
    }

    if (!this.selectedBox) {
      if (event.shiftKey && event.key == 'ArrowRight') {
        this.letterBoxes.forEach(letterBox => {
          letterBox.x1 += 1;
          letterBox.x2 += 1;
        });
        this.drawScroll();
      } else if (event.shiftKey && event.key == 'ArrowLeft') {
        this.letterBoxes.forEach(letterBox => {
          letterBox.x1 -= 1;
          letterBox.x2 -= 1;
        });
        this.drawScroll();
      } else if (event.shiftKey && (event.key == 's' || event.key == 'S')) {
        this.letterBoxes.forEach(letterBox => letterBox._row = undefined);
        this.letterBoxService.batchCreate(this.letterBoxes, result => {
          this.selectedBox = undefined;
          this.drawScroll();
        });
      }
      return;
    }

    if (event.key == 'd' || event.key == 'D') {
      var deleteBox = this.selectedBox;
      this.letterBoxService.Delete(deleteBox.id, result => {
        if (deleteBox.type == 'Letter') {
          // Remove the letter from the list.
          this.letterBoxes.splice(this.letterBoxes.indexOf(deleteBox), 1);

          // Remove from stats.
          var stats = this.statMap[this.selectedBox.value];
          stats.letterBoxes.splice(stats.letterBoxes.indexOf(deleteBox), 1);

          // Remove from row.
          if (deleteBox._row) {
            var row = deleteBox._row;
            row._letterBoxes.splice(row._letterBoxes.indexOf(deleteBox), 1);
          }
        } else if (deleteBox.type == 'Row') {
          this.rows.splice(this.rows.indexOf(deleteBox), 1);
        }
        this.selectedBox = undefined;
        this.drawScroll();
      });
    } else if (event.key == 'r' || event.key == 'R') {
      this.letterDialogShown = true;
      this.dialogStyle = {top: $window.visualViewport.pageTop - 20};
      this.$scope.$apply();
    } else if (event.key == 's' || event.key == 'S') {
      this.letterBoxService.create(this.selectedBox, result => {
        this.selectedBox = undefined;
        this.drawScroll();
      });
    } else if (event.key == 'ArrowUp') {
      this.selectedBox.y2 -= 1;
      if (!event.shiftKey) {
        this.selectedBox.y1 -= 1;
      }
      this.drawScroll();
    } else if (event.key == 'ArrowDown') {
      this.selectedBox.y2 += 1;
      if (!event.shiftKey) {
        this.selectedBox.y1 += 1;
      }
      this.drawScroll();
    } else if (event.key == 'ArrowLeft') {
      this.selectedBox.x2 -= 1;
      if (!event.shiftKey) {
        this.selectedBox.x1 -= 1;
      }
      this.drawScroll();
    } else if (event.key == 'ArrowRight') {
      this.selectedBox.x2 += 1;
      if (!event.shiftKey) {
        this.selectedBox.x1 += 1;
      }
      this.drawScroll();
    }
  });

  this.canvas = canvas;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.saveSelected = function() {
  if (!this.selectedBox) {
    return;
  }

  // If there is a selected box currently, save it to the backend and deselect it.
  var saveBox = this.selectedBox;
  this.selectedBox = undefined;
  saveBox._state = 'saving';
  saveBox._letterBoxes = undefined;
  saveBox._row = undefined;
  this.letterBoxService.create(saveBox, result => {
    saveBox._state = undefined;
    saveBox.id = result.id;
    this.drawScroll();
  }, error => {
    saveBox._state = 'error';
    notifyError(error);
    this.drawScroll();
  });

  if (saveBox.type == 'Letter') {
    this.addLetterStat(saveBox);
  } else {
    this.rows.forEach(row => row._letterBoxes = []);
    this.letterBoxes.forEach(letterBox => {
      letterBox._row = undefined;
      this.addLetterStat(letterBox);
    });
  }
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.addEventListeners = function() {
  var letterCanvas = document.getElementById("letters");
  letterCanvas.addEventListener('click', event => {
    this.saveSelected();

    var rect = letterCanvas.getBoundingClientRect();
    var x = event.x - rect.left,
        y = event.y - rect.top;

    for (var b = 0; b < this.letterBoxes.length; b++)  {
      var letterBox = this.letterBoxes[b];
      var byLCoords = letterBox._byLetterCoords;
      if (byLCoords.x1 < x && byLCoords.x2 > x && byLCoords.y1 < y && byLCoords.y2 > y) {
        this.selectedBox = letterBox;
        this.drawScroll();
        return;
      }
    }
  });
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.setSelectedTab = function(tab) {
  this.selectedTab = tab;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.scrollChanged = function() {
  this.column = undefined;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.refresh = function() {
  this.canvasReady = false;
  this.ctx = this.canvas.getContext('2d');
  this.canvasTitle = 'Dead Sea Scrolls Viewer - ' + this.scroll.name + ' Column ' + this.column;

  this.filename = this.scroll.filename + '-column-' + this.column;

  this.letterBoxes = [];
  this.statMap = {};
  this.rows = [];
  var request = {filter: `filename=${this.filename}`, pageSize: 0, orderBy: 'y2,x2 DESC'};
  this.letterBoxService.list(request, response => {
    console.log("Letter boxes: " + response.items.length);
    this.letterBoxes = [];
    this.rows = [];
    this.statMap = {};
    response.items.forEach(letterBox => {
      /* if (this.scroll == this.scrolls.ISAIAH && this.column == 9) {
        letterBox.x1 += 12;
        letterBox.x2 += 12;
      } */
      if (letterBox.type == 'Letter') {
        this.letterBoxes.push(letterBox);
      } else if (letterBox.type == 'Row') {
        letterBox._letterBoxes = [];
        this.rows.push(letterBox);
      }
    });
    this.letterBoxes.forEach(letterBox => this.addLetterStat(letterBox));
    if (this.canvasReady) {
      this.drawScroll();
    }
  });

  this.img = new Image();
  this.img.onload = () => {
    this.canvasWidth = this.img.width * 2;
    this.canvasHeight = this.img.height;
    this.$scope.$apply();
    this.drawScroll();
  }
  this.img.src = `https://dss-images-dot-dd4-biblical.appspot.com/images/${this.scroll.filename}/columns/column_${this.scroll.res}_${this.column}.jpg`;

  if (this.scroll.textFile) {
    this.$http({
      method: 'GET',
      url: `https://dss-images-dot-dd4-biblical.appspot.com/books/${this.scroll.textFile}`,
      headers: {'Content-type': 'text/plain'}
    }).then(response => {
      var romanNumeral = romanize(this.column);
      var re=/\r\n|\n\r|\r/g;
      var lines = response.data.replace(re,'\n').split('\n');
      var l = 0;
      while (!lines[l].startsWith('Col. ' + romanNumeral)) {l++;}
      this.textFile = lines[l];
      while (!lines[++l].startsWith('Col. ')) {
        this.textFile += '\n' + lines[l];
      }
    }, errorResponse => {
      notifyError(response.data.error);
    });
  } else {
    this.textFile = undefined;
  }

  this.drawScroll();
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.getAvgDim = function(letter) {
  var stats = this.statMap[letter.value];
  if (stats) {
    var count = stats.letterBoxes.length, totalWidth = 0, totalHeight = 0;
    stats.letterBoxes.forEach(letterBox => {
      totalWidth += letterBox.x2 - letterBox.x1;
      totalHeight += letterBox.y2 - letterBox.y1;
    });
    return {avgWidth: totalWidth / count, avgHeight: totalHeight / count};
  }
  return {avgWidth: 26, avgHeight: 26};
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.create = function(letter) {
  if (this.selectedBox) {
    if (this.selectedBox.value != letter.value) {
      var stats = this.statMap[this.selectedBox.value];
      stats.letterBoxes.splice(stats.letterBoxes.indexOf(this.selectedBox), 1);
      this.selectedBox.value = letter.value;
    }
  } else {
    var stats = this.getAvgDim(letter);
    var letterBox = {
      filename: this.filename, type: 'Letter', value: letter.value,
      x1: this.x - stats.avgWidth / 2, y1: this.y - stats.avgHeight / 2,
      x2: this.x + stats.avgWidth / 2, y2: this.y + stats.avgHeight / 2};

    this.letterBoxes.push(letterBox);
    this.selectedBox = letterBox;
  }
  this.addLetterStat(this.selectedBox);
  this.drawScroll();
  this.closeDialog();
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.createRow = function() {
  if (this.selectedBox) {
    this.selectedBox.value = this.rowNum;
  } else {
    var rowBox = {
      filename: this.filename, type: 'Row', value: this.rowNum, _letterBoxes: [],
      x1: this.x, y1: this.y, x2: this.canvasWidth / 2 - 10, y2: this.y + 70};

    var index = 0;
    while (index < this.rows.length && this.rows[index].y1 < rowBox.y1) {
      index++;
    }
    this.rows.splice(index, 0, rowBox);
    this.selectedBox = rowBox;
  }

  this.rowNum++;
  this.drawScroll();
  this.closeDialog();
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.closeDialog = function() {
  this.letterDialogShown = undefined;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.drawScroll = function() {
  this.ctx.drawImage(this.img, 0, 0);
  this.ctx.fillStyle = 'white';
  this.ctx.fillRect(this.canvasWidth / 2, 0, this.canvasWidth / 2, this.canvasHeight);

  this.letterBoxes.forEach(letterBox => {
    if (this.showLetterBoxes || this.selectedBox == letterBox) {
      this.drawLetterBox(letterBox);
    }
  });
  this.canvasReady = true;
  this.drawImagesByLetter();

  this.rows.forEach(row => {
    if (this.showRowBoxes || this.selectedBox == row) {
      this.ctx.beginPath();
      this.ctx.strokeStyle = this.selectedBox == row ? 'blue' : 'black';
      this.ctx.rect(row.x1, row.y1, row.x2 - row.x1, row.y2 - row.y1);
      this.ctx.stroke();
    }
  });

  this.rows.forEach(row => {
    this.ctx.font = '25px Arial';
    this.ctx.fillStyle = this.selectedBox == row ? 'blue' : 'black';
    this.ctx.fillText(row.value, row.x2 + 10, row.y2 - 20);
  });

  this.computeDiff();
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.drawLetterBox = function(letterBox) {
  var color;
  if (letterBox == this.selectedBox) {
    color = 'blue';
  } else if (letterBox._state == 'saving') {
    color = 'yellow';
  } else if (letterBox._state == 'error') {
    color = 'red';
  }

  var ctx = this.ctx;
  ctx.beginPath();
  ctx.strokeStyle = color || 'green';
  ctx.rect(letterBox.x1, letterBox.y1, letterBox.x2 - letterBox.x1, letterBox.y2 - letterBox.y1);
  ctx.stroke();

  ctx.font = '25px Arial';
  ctx.fillStyle = color || 'black';
  ctx.fillText(letterBox.value, letterBox.x1 + this.canvasWidth / 2, letterBox.y2 - 20);
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.addLetterStat = function(letterBox) {
  if (letterBox.type != 'Letter') {
    return;
  }

  var stats = this.statMap[letterBox.value];
  if (!stats) {
    stats = {letterBoxes: []};
    this.statMap[letterBox.value] = stats;
  }
  if (stats.letterBoxes.indexOf(letterBox) == -1) {
    stats.letterBoxes.push(letterBox);
  }

  if (letterBox._row) {
    letterBox._row.splice(letterBox._row.indexOf(letterBox), 1);
  }

  for (var r = 0; r < this.rows.length; r++) {
    var row = this.rows[r];
    if (row.y1 < letterBox.y2 && row.y2 >= letterBox.y2) {
      if (row._letterBoxes.indexOf(letterBox) == -1) {
        var index = 0;
        while (index < row._letterBoxes.length && row._letterBoxes[index].x2 > letterBox.x2) {
          index++;
        }
        row._letterBoxes.splice(index, 0, letterBox);
        letterBox._row = row;
      }
      break;
    }
  }
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.drawImagesByLetter = function() {
  var canvas = document.getElementById("letters");
  var ctx = canvas.getContext('2d');
  ctx.fillStyle = 'white';
  ctx.fillRect(0, 0, 1000, 1000);
  ctx.font = '25px Arial';
  ctx.fillStyle = 'black';

  var y = 0;
  for (var l in this.letters) {
    var x = 0;
    var letter = this.letters[l];
    ctx.fillText(letter.value, x, y + 20);
    var maxHeight = 32;

    var stats = this.statMap[letter.value];
    if (stats) {
      stats.letterBoxes.forEach(letterBox => {
        x += 30;
        var width = letterBox.x2 - letterBox.x1;
        var height = letterBox.y2 - letterBox.y1;
        if (x + width > this.canvasWidth / 2) {
          x = 30;
          y += maxHeight;
        }
        if (height > maxHeight) {
          maxHeight = height;
        }
        ctx.drawImage(this.img, letterBox.x1, letterBox.y1, width, height, x, y, width, height);
        letterBox._byLetterCoords = {x1: x, y1: y, x2: x + width, y2: y + height};
        if (letterBox == this.selectedBox) {
          ctx.beginPath();
          ctx.strokeStyle = 'blue';
          ctx.rect(x, y, width, height);
          ctx.stroke();
        }
      });
    }
    y += maxHeight;
  }
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.getRowText = function(row) {
  var text = row.value + ' ';
  var lastX1 = undefined;
  row._letterBoxes.forEach(letterBox => {
    if (lastX1 && (lastX1 - letterBox.x2) >= 5) {
      text += ' ';
    }
    text += letterBox.value;
    lastX1 = letterBox.x1;
  });
  return text;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.computeDiff = function() {
  var textFileArray = this.textFile.split('\n');
  var rowTextMap = {};
  this.resultText = '';
  this.rows.forEach(row => {
    var rowText = this.getRowText(row);
    rowTextMap[row.value] = rowText;
    this.resultText += rowText + '\n';
  });
  this.textDiffs = [];

  for (var x = 1; x < textFileArray.length; x++) {
    var textDiff = {expected: unfinalize(textFileArray[x]), actual: rowTextMap[x] || ''};

    var diffMatch = new diff_match_patch();
    textDiff.diff = diffMatch.diff_main(textDiff.expected, textDiff.actual);
    textDiff.diff.forEach(diff => {
      if (diff['0'] == -1) {
        diff._class = 'diff-delete';
      } else if (diff['0'] == 1) {
        diff._class = 'diff-insert';
      }
    });
    this.textDiffs.push(textDiff);
  }
}

function unfinalize(text) {
  var result = '';
  for (var x = 0; x < text.length; x++) {
    if (text[x] == 'ך') {
      result += 'כ';
    } else if (text[x] == 'ם') {
      result += 'מ';
    } else if (text[x] == 'ן') {
      result += 'נ';
    } else if (text[x] == 'ף') {
      result += 'פ';
    } else if (text[x] == 'ץ') {
      result += 'צ';
    } else if (text[x] == '.') {
      result += ' ';
    } else if (text[x] == ' ' || x < 2 || text[x] >= 'א' && text[x] <= 'ת') {
      result += text[x];
    }
  }
  return result;
}

function romanize(num) {
  var lookup = {M:1000, CM:900, D:500, CD:400, C:100, XC:90, L:50, XL:40, X:10, IX:9, V:5, IV:4, I:1};
  var roman = '', i;
  for (i in lookup) {
    while (num >= lookup[i]) {
      roman += i;
      num -= lookup[i];
    }
  }
  return roman;
}