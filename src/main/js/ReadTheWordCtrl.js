com.digitald4.biblical.ReadTheWordCtrl =
    function($location, $window, globalData, bookService, interlinearService, scriptureService) {
  this.locationProvider = $location;
  this.window = $window;
  this.globalData = globalData;
  this.globalData.scriptureVersion = globalData.scriptureVersion || 'ISR';
  this.bookService = bookService;
  this.interlinearService = interlinearService;
  this.scriptureService = scriptureService;
  this.scripture_views = ['Text', 'Interlaced', 'Interlinear'];
  this.reference = {value: $location.search()['reference']};
  this.pageToken = $location.search()['pageToken'] || 1;
  this.language = $location.search()['lang'];
  this.scriptureView = $location.search()['view'] || 'Text';
  this.highlight = $location.search()['highlight'];
  this.views = {READ: 1, SEARCH_AND_REPLACE: 2, UPLOAD: 3};
  this.view = this.views.READ;
  this.showReference();
  var strongsId = $location.search()['strongsId'];
  if (strongsId) {
    this.lexiconRequest = {strongsId: strongsId, view: $location.search()['lexiconView']};
  }
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.getOrSearch = function(page) {
  this.locationProvider.search('reference', this.reference.value);
  this.locationProvider.search('version', this.globalData.scriptureVersion);
  this.locationProvider.search('lang', this.globalData.language);
  this.locationProvider.search('view', this.scriptureView);
  this.locationProvider.search('pageToken', page);
  this.locationProvider.search('strongsId', undefined);
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.setReference = function(reference) {
  this.locationProvider.search('reference', reference);
  this.locationProvider.search('version', this.globalData.scriptureVersion);
  this.locationProvider.search('lang', this.globalData.language);
  this.locationProvider.search('view', this.scriptureView);
  this.locationProvider.search('pageToken', undefined);
  this.locationProvider.search('strongsId', undefined);
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showReference = function() {
  if (!this.reference.value) {
    return;
  }
  var request = {
    searchText: this.reference.value,
    version: this.globalData.scriptureVersion,
    lang: this.language,
    view: this.scriptureView,
    pageSize: 50,
    pageToken: this.pageToken
  };
  this.scriptureService.search(request, response => {
    if (response.resultType == 'GET') {
      this.processScriptureResult(response);
    } else {
      this.processSearchResult(response);
    }
  });

  this.scriptures = [{text: 'Loading ' + this.reference.value + '...'}];
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.processScriptureResult = function(scriptureResult) {
  this.prevChapter = scriptureResult.prevChapter;
  this.scriptures = scriptureResult.items || [];
  this.nextChapter = scriptureResult.nextChapter;
  var previous;
  for (const script of this.scriptures) {
    if (!previous || previous.chapter != script.chapter || previous.verse + 1 != script.verse
        || previous.book != script.book || previous.version != script.version) {
      script.showChapter = true;
    }
    script.dir = script.language.startsWith('he') ? 'rtl' : 'ltr';
    script.tableAlign = script.language.startsWith('he') ? 'right' : 'left';
    script.tableFloat = script.language.startsWith('he') ? 'tablefloatheb' : 'tablefloat';
    if (this.highlight != undefined && script.interlinears) {
      for (const interlinear of script.interlinears) {
        interlinear.highlight = interlinear.strongsId == this.highlight ? 'diff-delete' : undefined;
      }
    }
    previous = script;
  }
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.processSearchResult = function(searchResult) {
  searchResult.grouped = [];
  var current;
  for (const result of searchResult.items) {
    if (current && current.book == result.book && current.chapter == result.chapter && current.verse == result.verse) {
      current.items.push(result);
    } else {
      current = {book: result.book, chapter: result.chapter, verse: result.verse, items: [result]};
      searchResult.grouped.push(current);
    }
  }
  this.searchResult = searchResult;
  this.searchShown = true;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showScripture = function(version, book, chapter, verse) {
  this.globalData.reference = {book: book, chapter: chapter, verse: verse,
      version: version || this.globalData.scriptureVersion, lang: this.globalData.language};
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showStrongsDef = function(strongsId) {
  this.lexiconRequest = {strongsId: strongsId};
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showStrongsRefDialog = function(interlinear, isSub) {
  this.setDialogStyle();
  this.strongsId = interlinear.strongsId;
  if (isSub) {
    for (var subToken of interlinear.subTokens) {
      if (subToken.strongsId) {
        this.strongsId = subToken.strongsId;
      }
    }
  }
  this.interlinear = interlinear;
  this.references = undefined;
  this.dialogShown = 'REFERENCES';
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showStrongsRefs = function(page) {
  var request = {pageToken: page};
  if (this.globalData.matchStrongs) request.strongsId = this.strongsId;
  if (this.globalData.matchWord) request.word = this.interlinear.word;
  if (this.globalData.matchConstantsOnly) request.hebrewWord = this.interlinear.constantsOnly;
  this.interlinearService.getReferences(request, response => {
    this.references = this.interlinearService.processPagination(response);
  });
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showScroll = function(scripture) {
  this.canvasReference = scripture.reference;
  this.setDialogStyle();
  const canvas = document.getElementById("scrollview");
  this.dialogShown = 'SCROLL_VIEW';
  if (!canvas.getContext) {
    alert('Canvas not supported, will not be able to view scroll');
    return;
  }
  const ctx = canvas.getContext("2d");
  this.canvasTitle = 'Dead Sea Scrolls Viewer - ' + this.canvasReference;

  this.scriptureService.getScrollCoords(scripture, scrollData => {
    this.canvasWidth = DSS_ISA_COLUMN_INFO[scrollData.columns[0]].width * 3.7445;
    this.drawScroll(ctx, scrollData);
  });
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showDSSAudit = function() {
  this.canvasShowAudit = !this.canvasShowAudit;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.drawScroll = function(ctx, scrollData) {
  // printBaseWidth(ctx);

  // Zoom 9 is 250 images wide and 10 images high.
  // 18 -> 22 is one nice full DSS column, don't know which yet.
  // Isa 1:1   shape: {"p": "M 15460,72,15184,72,15184,104,15316,104,15316,112,15460,112,15460,72", "c": "1"}
  // Isa 1:25  shape: {"p": "M 15290,496,15240,496,15240,490,15231,490,15231,495,15204,495,15204,512,15322,515,15322,532,15422,535,15422,512,15359,512,15359,509,15352,509,15324,508,15290,508,15290,496", "c": "1"}
  // Isa 9:6   shape: {"p": "M 13353,423,13352,468,13628,484,13627,447,13403,434,13404,419,13354,417,13352,417,13353,423", "c": "9"}
  // I have found the numbers are off by 1, what is called 9:5 is the real 9:6.
  // Isa 9:5   shape: {"p": "M 13403,439,13627,449,13627,433,13550,429,13550,409,13353,403,13353,421,13403,423", "c": "9"}
  // Isa 66:15 shape: {"p": "M 212,121, 212,111, 40,112, 39,94, 111,94, 111,89, 120,89, 120,96, 308,92, 309,121, 212,121", "c": "66"}
  // 54 columns, 66 chapters
  // Zoom dim: 227px * 256 = 58112px, 227px * 10 = 2270px
  // Base dim: 15517px, 600px
  // Ratios: 3.745053811948186, 3.78333

  // Zoom level 9 Isa 9:6 should be at about (49018_1524, 49838_1699) = (13403_403, 13627_449) x (3.657279_3.78333)
  // That would be column 215 = 49018 / 227, row 6 = 1524 / 227 to column 219 = 51555/227 , row 7 = 1699 / 227
  // However we found it should really be: 220_6 to 224_7

  // Zoom level 9 Isa 66:15: (142_336, 1130_457) = (39_89, 309_121) x (3.657279_3.78333)
  // Resulting tiles 0_1 = (142 / 227, 336 / 227), 4_2 (1130 / 227, 457 / 227)

  // var shape = {"p": "M 212,121,212,111,40,112,39,94,111,94,111,89,120,89,120,96,308,92,309,121,212,121", "c": "66"};
  var paths = $.parseJSON(scrollData.shape).p.split('M ').slice(1);
  console.log(paths);
  var values = paths[0].split(',');
  var coords = [];
  var rollOffset = (54 - scrollData.columns[0]) / 53 * 90;
  var minY, maxY;
  for (var i = 0; i < values.length; i++) {
    var coord = {
      x: Math.round(parseInt(values[i]) * 3.7445) + rollOffset,
      y: Math.round(parseInt(values[++i]) * 3.7445)};
    coords.push(coord);
    console.log(coord.x + ',' + coord.y);
    if (i == 1) {
      minY = maxY = coord.y;
    } else if (coord.y < minY) {
      minY = coord.y;
    } else if (coord.y > maxY) {
      maxY = coord.y;
    }
  }

  const startX = DSS_ISA_COLUMN_INFO[scrollData.columns[0]].start * 3.7445;
  const endX = startX + DSS_ISA_COLUMN_INFO[scrollData.columns[0]].width * 3.7445;

  const startTileCol = Math.floor(startX / 227);
  const endTileCol = Math.floor(endX / 227);
  const startTileRow = Math.floor(minY / 227);
  const endTileRow = Math.floor(maxY / 227);

  const xOffset = startTileCol * 227;
  const yOffset = startTileRow * 227;

  var totalDrawn = 0;
  const bookService = this.bookService;
  var drawTile = function(c, r) {
    const col = (startTileCol + c);
    const row =  (startTileRow + r)
    var img = new Image();
    img.onload = () => {
      if (c == 0) {
        ctx.drawImage(
            img, (startX - xOffset), 0, img.width, img.height, 0, r * 227, img.width, img.height);
      } else {
        ctx.drawImage(img, c * 227 - (startX - xOffset), r * 227);
      }
      if (++totalDrawn == 10) {
        setTimeout(function() {
          ctx.strokeStyle = 'green';
          ctx.beginPath();
          ctx.moveTo(coords[0].x - startX, coords[0].y - yOffset);
          for (var i = 1; i < coords.length; i++) {
            ctx.lineTo(coords[i].x - startX, coords[i].y - yOffset);
          }
          ctx.stroke();
        }, 500); // Wait 1/2 a second before drawing lines to give time for images to be drawn.
      }
    }
    img.src = `https://dss-images-dot-dd4-biblical.appspot.com/images/isaiah/tiles/9_${col}_${row}.jpg`;
  }

  for (var c = startTileCol; c <= endTileCol; c++) {
    for (var r = 0; r < 2; r++) {
      drawTile(c - startTileCol, r);
    }
  }
}

printBaseWidth = function(ctx) {
  var total = 0;
  console.log('var DSS_ISA_COLUMN_STARTS = [');
  var getWidth = function(c) {
    var fileName = 'http://tiles.imj.org.il/columns/isaiah/isaiah' + c + '.jpg';
    // console.log('Loading: ' + fileName);
    var img = new Image();
    img.onload = () => {
      console.log('{column: ' + c + ', start: ' + total + ', width: ' + img.width + '}');

      total += img.width;
      // console.log(total);
    }
    img.src = fileName;
  }
  for (var c = 54; c > 0; c--) {
    getWidth(c);
  }
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.setDialogStyle = function() {
  this.dialogStyle = this.dialogStyle || {top: this.window.visualViewport.pageTop - 20};
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.closeDialog = function() {
  this.dialogShown = this.dialogStyle = undefined;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.searchAndReplace = function(preview) {
  var request = {'phrase': this.phrase, 'replacement': this.replacement, 'filter': this.filter, 'preview': preview};
  this.scriptureService.searchAndReplace(request, searchResult => {
    this.processSearchResult(searchResult);
    this.previewShown = preview ? true : undefined;
  });

  this.scriptures = [{text: this.phrase + ' ' + this.filter}];
  this.searchShown = undefined;
  this.previewShown = undefined;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showUpload = function() {
  if (!this.allBooks) {
    this.bookService.getBibleBooks(true, bibleBooks => {
      this.allBooks = [];
      for (var b = 0; b < bibleBooks.length; b++) {
        if (bibleBooks[b].unreleased) {
          this.allBooks.push(bibleBooks[b]);
        }
      }
      this.allBooks.push({name: '-------------------------------'});
      [].push.apply(this.allBooks, bibleBooks);
    });
  }
  this.view = this.views.UPLOAD;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.uploadScripture = function(preview) {
  var bookName = this.uploadBook.name
  var request = {'version': this.uploadVersion, 'book': bookName, 'chapter': this.uploadChapter,
      'text': this.uploadText, 'preview': preview};
  this.scriptureService.uploadScripture(request, scriptureResult => {
    this.processScriptureResult(scriptureResult);
    this.uploadChapter = scriptureResult.items[0].chapter;
    this.previewShown = preview ? true : undefined;
    if (!preview) {
      this.uploadText = '';
      this.uploadChapter++;
    }
  });

  this.scriptures =
      [{text: 'Uploading to: ' + this.uploadVersion + ' ' + bookName + ' ' + this.uploadChapter}];
  this.searchShown = undefined;
  this.previewShown = undefined;
}
