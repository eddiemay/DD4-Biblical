com.digitald4.biblical.DssIdentifierCtrl = function($http, $scope, $window, letterBoxService, scriptureService) {
  this.$http = $http;
  this.$scope = $scope;
  this.$window = $window;
  this.letterBoxService = letterBoxService;
  this.scriptureService = scriptureService;
  this.showLetterBoxes = this.showLetters = this.showMissmatch = this.showRowBoxes = true;
  this.rowNum = 1;
  const makeColumns = (n) => Array.from({length: n}, (_, i) => String(i + 1));
  this.scrolls = {
    ISAIAH: {name: 'Isaiah', filename: 'isaiah', columns: makeColumns(54), res: 9, textFile: '1Q_Isaiah_a.txt'},
    COMMUNITY_RULE: {name: 'Community Rule', filename: 'community', columns: makeColumns(11), res: 7},
    WAR_SCROLL: {name: 'War Scroll', filename: 'war', columns: makeColumns(15), res: 8},
    TEMPLE: {name: 'Temple Scroll', filename: 'temple', columns: makeColumns(67), res: 9},
    HABAKKUK: {name: 'Commentary on Habakkuk', filename: 'habakkuk', columns: makeColumns(14), res: 7},
    TORAH: {name: 'Torah Scroll', filename: 'torah', columns: makeColumns(4), res: 4},
    JUBILEES: {name: 'Jubilees', filename: 'Jubilees', columns: ['3Q5-Frag1', '3Q5-Frag6', '3Q5-Frag7', '3Q5-Frag8',
        '4Q176-Frag1', '4Q176-Frag2', '4Q176-Frag5', '4Q176-Frag6', '4Q176-Frag7',
        'Plate293-Frag1', 'Plate293-Frag2', 'Plate293-Frag3', 'Plate293-Frag4', 'Plate293-Frag5', 'Plate293-Frag6', 'Plate293-Frag7', 'Plate293-Frag8', 'Plate293-Frag9']},
    Calendrical: {name: '4QCalendrical', filename: '4QCalendrical', textFile: '4QCalendrical.txt', columns: ['4Q318-Frag1', '4Q318-Frag2', '4Q318-Frag3', '4Q318-Frag4', '4Q318-Frag5',
        '4Q319-Frag1', '4Q319-Frag2', '4Q319-Frag2-II', '4Q319-Frag3', '4Q319-Frag7',
        '4Q320', '4Q320-Frag1', '4Q320-Frag2', '4Q320-Frag3',
        '4Q321-Frag1', '4Q321-Frag2', '4Q321-Frag3', '4Q321-Frag4', '4Q321-Frag5', '4Q321-Frag6', '4Q321-Frag7', '4Q321-Frag8', '4Q321-Frag9', '4Q321-P372-Frag1', '4Q321-P372-Frag2', '4Q321-P372-Frag3',
        '4Q321a-Frag1', '4Q321a-Frag2', '4Q321a-Frag3', '4Q321a-Frag4', '4Q321a-Frag5', '4Q321a-Frag6', '4Q321a-Frag7',
        '4Q326', '4Q326-Frag1', '4Q326-Plates-693_710', '4Q326-Plates-693_710_694',
        '4Q328']}
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
    interlinear: {name: 'Interlinear', isEnabled: () => true}
  }

  this.textFile = "Please select a scroll and fragment."

  this.canvas = document.getElementById("scroll_view");
  if (!this.canvas.getContext) {
    alert('Canvas not supported, will not be able to view scroll');
    return;
  }
  this.letterBoxes = [];
  this.addEventListeners();
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.addEventListeners = function() {
  var canvas = this.canvas;
  var $window = this.$window;
  canvas.addEventListener('click', event => {
    // event.preventDefault();
    var rect = canvas.getBoundingClientRect();
    var x = Math.round(event.x - rect.left),
        y = Math.round(event.y - rect.top);
    if (this.newRow) {
      this.x = x;
      this.y = y;
      if (this.addRowCoord()) {
        return;
      }
    }

    this.saveSelected();

    if (this.showLetterBoxes || this.showLetters || this.showMissmatch) {
      for (var b = 0; b < this.letterBoxes.length; b++)  {
        var lx = x > this.canvasWidth / 2 ? x - this.canvasWidth / 2 : x;
        var ly = x > this.canvasWidth / 2 ? y + 20 : y;
        var letBox = this.letterBoxes[b];
        if (this.showLetterBoxes && letBox.x1 < lx && letBox.x2 > lx && letBox.y1 < ly && letBox.y2 > ly ||
            this.showMissmatch && letBox.value != letBox._predicted && letBox.x1 < lx && letBox.x2 > lx && letBox.y1 < ly && letBox.y2 > ly ||
            this.showLetters && x >= letBox.x1 + 7 && x < letBox.x1 + 14 && y > letBox.y2 + 7 && y <= letBox.y2 + 14) {
            this.selectedBox = letBox;
          this.drawScroll();
          return;
        }
      }
    }
    if (this.showRowBoxes) {
      for (var b = 0; b < this.rows.length; b++)  {
        var rowBox = this.rows[b];
        for (var c = 0; c < rowBox.coords.length; c++) {
          var coord = rowBox.coords[c];
          if (Math.abs(x - coord.x) < 5 && Math.abs(y - coord.y) < 5) {
            this.selectedBox = rowBox;
            this.selectedCoord = coord;
            this.drawScroll();
            return;
          }
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
    this.$scope.$apply();
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
       if (deleteBox.type == 'Row') {
          this.rows.splice(this.rows.indexOf(deleteBox), 1);
        } else {
          // Remove the letter from the list.
          this.letterBoxes.splice(this.letterBoxes.indexOf(deleteBox), 1);

          // Remove from stats.
          var stats = this.statMap[this.selectedBox.value];
          stats.letterBoxes.splice(stats.letterBoxes.indexOf(deleteBox), 1);

          // Remove from row.
          if (deleteBox._row) {
            deleteBox._row._letterBoxes.splice(deleteBox._row._letterBoxes.indexOf(deleteBox), 1);
          }
        }
        this.selectedBox = undefined;
        this.drawScroll();
        // this.updateTranslation();
      });
    } else if (event.key == 'r' || event.key == 'R') {
      this.letterDialogShown = true;
      this.dialogStyle = {top: $window.visualViewport.pageTop - 20};
      this.$scope.$apply();
    } else if (event.key == 's' || event.key == 'S') {
      this.saveSelected();
    } else if (this.selectedCoord) {
      if (event.key == 'ArrowUp') {
        this.selectedCoord.y -= 1;
      } else if (event.key == 'ArrowDown') {
        this.selectedCoord.y += 1;
      } else if (event.key == 'ArrowLeft') {
        this.selectedCoord.x -= 1;
      } else if (event.key == 'ArrowRight') {
        this.selectedCoord.x += 1;
      }
      this.drawScroll();
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

  var letterCanvas = document.getElementById("letters");
  letterCanvas.addEventListener('click', event => {
    this.saveSelected();

    var rect = letterCanvas.getBoundingClientRect();
    var x = event.x - rect.left,
        y = event.y - rect.top;

    for (var b = 0; b < this.letterBoxes.length; b++)  {
      var letterBox = this.letterBoxes[b];
      var byLCoords = letterBox._byLetterCoords;
      if (byLCoords && byLCoords.x1 < x && byLCoords.x2 > x && byLCoords.y1 < y && byLCoords.y2 > y) {
        this.selectedBox = letterBox;
        console.log(this.selectedBox.x1 + ',' + this.selectedBox.y1);
        this.drawScroll();
        return;
      }
    }
  });
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.saveSelected = function() {
  this.saveBox(this.selectedBox);
  this.selectedBox = this.selectedCoord = this.newRow = undefined;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.saveBox = function(saveBox) {
  if (!saveBox) {
    return;
  }

  saveBox._state = 'saving';
  if (saveBox.type == 'Row') {
    this.rows.forEach(row => row._letterBoxes = []);
    this.letterBoxes.forEach(letterBox => {
      letterBox._row = undefined;
      this.addLetterStat(letterBox);
    });
  } else {
    saveBox.type = saveBox.value.length == 1 ? 'Letter' : 'Word';
    this.addLetterStat(saveBox);
  }

  if (saveBox.coords) {
    saveBox.x1 = saveBox.coords[0].x;
    saveBox.x2 = saveBox.coords[saveBox.coords.length - 1].x;

    saveBox.y1 = saveBox.coords[0].y;
    saveBox.y2 = saveBox.coords[0].y;
    saveBox.coords.forEach(coord => {
      if (coord.y < saveBox.y1) {
        saveBox.y1 = coord.y;
      } else if (coord.y > saveBox.y2) {
        saveBox.y2 = coord.y;
      }
    });
  }

  if (this.offline) {
    return;
  }

  var _row = saveBox._row;
  var _letterBoxes = saveBox._letterBoxes;
  saveBox._row = saveBox._letterBoxes = undefined;
  this.letterBoxService.create(saveBox, result => {
    saveBox.id = result.id;
    saveBox._state = undefined;
    saveBox._predicted = result._predicted;
    saveBox._row = _row;
    saveBox._letterBoxes = _letterBoxes;
    this.drawScroll();
  }, error => {
    saveBox._state = 'error';
    notifyError(error);
    this.drawScroll();
  });
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.setSelectedTab = function(tab) {
  this.selectedTab = tab;
  if (tab == this.tabs.interlinear) {
    this.updateTranslation();
  }
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.scrollChanged = function() {
  this.column = undefined;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.refresh = function() {
  this.canvasReady = false;
  this.ctx = this.canvas.getContext('2d');
  this.canvasTitle = 'Dead Sea Scrolls Viewer - ' + this.scroll.name + '-' + this.column;

  if (this.scroll.res) {
    this.filename = this.scroll.filename + '-column-' + this.column;
  } else {
    this.filename = this.scroll.filename + '-' + this.column;
  }

  this.letterBoxes = [];
  this.statMap = {};
  this.rows = [];
  var filename = this.filename;
  this.letterBoxService.byFilename(filename, true, response => {
    if (this.filename != filename) {
      return;
    }

    console.log("Letter boxes: " + response.items.length);
    this.letterBoxes = [];
    this.rows = [];
    this.statMap = {};
    response.items.forEach(letterBox => {
      /* if (this.scroll == this.scrolls.ISAIAH && this.column == 9) {
        letterBox.x1 += 12;
        letterBox.x2 += 12;
      } */
      if (letterBox.type == 'Row') {
        letterBox._letterBoxes = [];
        this.rows.push(letterBox);
      } else {
        this.letterBoxes.push(letterBox);
      }
    });
    this.letterBoxes.forEach(letterBox => this.addLetterStat(letterBox));
    if (this.canvasReady) {
      this.drawScroll();
    }

    this.rows.forEach(row => {
      var origY1 = row.y1;
      var miny = row.coords[0].y;
      row.coords.forEach(coord => {
        if (coord.y < miny) {
          miny = coord.y;
        }
      });

      if (origY1 != miny) {
        console.log("origY1: " + origY1 + " updated: " + miny);
        this.saveBox(row);
      }
    });

    this.updateTranslation();
  });

  this.img = new Image();
  this.img.onload = () => {
    this.canvasWidth = this.img.width * 2;
    this.canvasHeight = this.img.height;
    this.$scope.$apply();
    this.drawScroll();
  }
  if (this.scroll.res) {
    this.img.src = `https://dss-images-dot-dd4-biblical.appspot.com/images/${this.scroll.filename}/columns/column_${this.scroll.res}_${this.column}.jpg`;
  } else {
    this.img.src = `https://dss-images-dot-dd4-biblical.appspot.com/images/${this.scroll.filename}/columns/${this.column}.jpg`;
  }

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
      while (!(lines[l].startsWith('Col. ' + romanNumeral) || lines[l].startsWith('Frg. ' + romanNumeral))) {l++;}
      this.textFile = lines[l];
      while (!lines[++l].startsWith('Col. ') && !lines[l].startsWith('Frg. ')) {
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

com.digitald4.biblical.DssIdentifierCtrl.prototype.addRowCoord = function() {
  if (!this.newRow) {
    this.newRow = {filename: this.filename, type: 'Row', value: this.rowNum, coords: [], _letterBoxes: []};
    this.rows.push(this.newRow);
    this.selectedBox = this.newRow;
    this.rowNum++;
    this.closeDialog();
  }

  if (this.newRow.coords.length > 1 && this.x - this.newRow.coords[this.newRow.coords.length - 1].x < -50) {
    return false;
  }

  this.selectedCoord = {x: this.x, y: this.y};
  this.newRow.coords.push(this.selectedCoord);
  this.drawScroll();
  return true;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.closeDialog = function() {
  this.letterDialogShown = undefined;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.drawScroll = function() {
  var ctx = this.ctx;
  ctx.drawImage(this.img, 0, 0);
  ctx.fillStyle = 'white';
  ctx.fillRect(this.canvasWidth / 2, 0, this.canvasWidth / 2, this.canvasHeight);

  this.letterBoxes.forEach(letterBox => this.drawLetterBox(letterBox));
  this.canvasReady = true;
  this.drawImagesByLetter();

  this.rows.forEach(row => {
    if (this.showRowBoxes || this.selectedBox == row) {
      ctx.beginPath();
      ctx.strokeStyle = this.selectedBox == row ? 'blue' : 'black';
      var lastCoord = undefined;
      row.coords.forEach(coord => {
        ctx.arc(coord.x, coord.y, 5, 0, 2 * Math.PI);
        if (this.selectedCoord == coord) {
          ctx.arc(coord.x, coord.y, 7, 0, 2 * Math.PI);
        }
        if (lastCoord) {
          ctx.moveTo(lastCoord.x, lastCoord.y);
          ctx.lineTo(coord.x, coord.y);
        }
        lastCoord = coord;
      });
      ctx.stroke();
    }
  });

  this.rows.forEach(row => {
    this.ctx.font = '25px Arial';
    this.ctx.fillStyle = this.selectedBox == row ? 'blue' : 'black';
    if (row.x2 < this.canvasWidth / 4) {
      // If this column ends less than halfway then it is most likely has 2 columns so paint the number to the left.
      this.ctx.fillText(row.value, row.x1 - 10, row.y2 - 20);
    } else {
      if (row.coords) {
        this.ctx.fillText(row.value, row.x2 + 10, row.coords[row.coords.length - 1].y - 20);
      } else {
        this.ctx.fillText(row.value, row.x2 + 10, row.y2 - 20);
      }
    }
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
  } else if (this.showMissmatch && letterBox._predicted && letterBox.value != letterBox._predicted) {
    color = 'purple';
  }

  var ctx = this.ctx;

  // If we are showing letter boxes or the letter is of interest.
  if (this.showLetterBoxes || color) {
      ctx.beginPath();
      ctx.strokeStyle = color || 'green';
      ctx.rect(letterBox.x1, letterBox.y1, letterBox.x2 - letterBox.x1, letterBox.y2 - letterBox.y1);
      ctx.stroke();

      ctx.font = '25px Arial';
      ctx.fillStyle = color || 'black';
      ctx.fillText(letterBox.value, letterBox.x1 + this.canvasWidth / 2, letterBox.y2 - 20);
  }

  // If we are showing letters or the letter is of interest.
  if (this.showLetters || letterBox == this.selectedBox) {
      ctx.font = '16px Arial';
      ctx.fillStyle = color || 'green';
      if (letterBox._predicted && letterBox.value != letterBox._predicted) {
        ctx.fillText(letterBox._predicted + '-' + letterBox.value, letterBox.x1 - 0, letterBox.y2 + 14);
      } else {
        ctx.fillText(letterBox.value, letterBox.x1 + 7, letterBox.y2 + 14);
      }
  }
}

function includesBox(row, letterBox) {
  if (row.y2 < letterBox.y2 || row.x1 > letterBox.x1 || row.x2 < letterBox.x2) {
    return false;
  }

  var yAtX = row.y2;
  if (row.coords) {
    var ci = 0;
    while (row.coords.length > ci + 1 && row.coords[ci + 1].x <= letterBox.x1) {
      ci++;
    }
    if (ci == row.coords.length - 1) {
      console.log('got here, letterBox: ' + letterBox + ' row: ' + row);
      return false;
    }
    var slope = (row.coords[ci + 1].y - row.coords[ci].y) / (row.coords[ci + 1].x - row.coords[ci].x)
    yAtX = (letterBox.x1 - row.coords[ci].x) * slope + row.coords[ci].y;
  }

  return yAtX >= letterBox.y2;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.addLetterStat = function(letterBox) {
  if (letterBox.type == 'Row') {
    return;
  }

  if (letterBox.type == 'Letter') {
    var stats = this.statMap[letterBox.value];
    if (!stats) {
      stats = {letterBoxes: []};
      this.statMap[letterBox.value] = stats;
    }
    if (stats.letterBoxes.indexOf(letterBox) == -1) {
      stats.letterBoxes.push(letterBox);
    }
  }

  if (letterBox._row) {
    letterBox._row._letterBoxes.splice(letterBox._row._letterBoxes.indexOf(letterBox), 1);
  }

  var rows = this.rows.slice().sort((a, b) => Number(a.y1) - Number(b.y1));
  for (var r = 0; r < rows.length; r++) {
    var row = rows[r];
    if (includesBox(row, letterBox)) {
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
  var text = '';
  var lastX1 = undefined;
  row._letterBoxes.forEach(letterBox => {
    if (letterBox.type == 'Word') {
      text += ' ' + letterBox.value + ' ';
      lastX1 = undefined;
    } else {
      if (lastX1 && (lastX1 - letterBox.x2) >= 5) {
        text += ' ';
      }
      text += letterBox.value;
      lastX1 = letterBox.x1;
    }
  });
  return text;
}

com.digitald4.biblical.DssIdentifierCtrl.prototype.computeDiff = function() {
  var rowTextMap = {};
  this.resultText = '';
  this.rows
    .slice() // avoid mutating original array
    .sort((a, b) => Number(a.value) - Number(b.value))
    .forEach(row => {
      var rowText = row.value + ' ' + this.getRowText(row);
      rowTextMap[row.value] = rowText;
      this.resultText += rowText + '\n';
    });

  if (!this.textFile) {
    return;
  }

  var textFileArray = this.textFile.split('\n');
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

com.digitald4.biblical.DssIdentifierCtrl.prototype.updateTranslation = function() {
  this.translations = [];
  if (this.selectedTab == this.tabs.interlinear) {
    var rowTextList = [];
    var rowNumbers = [];
    this.rows
      .slice() // avoid mutating original array
      .sort((a, b) => Number(a.value) - Number(b.value))
      .forEach(row => {
        rowNumbers.push(row.value);
        rowTextList.push(this.getRowText(row));
      });
    this.scriptureService.bulkTranslate(rowTextList, response => {
      for (var x = 0; x < response.items.length; x++) {
        var scripture = response.items[x];
        scripture.book = this.scroll.name;
        scripture.chapter = this.column;
        scripture.verse = rowNumbers[x];
        this.translations.push(scripture);
      }
    });
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
  if (num.startsWith('4Q')) {
    return num;
  }
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