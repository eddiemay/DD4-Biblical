var FT_NODE_WIDTH = 100;
var FT_NODE_HEIGHT = 50;
com.digitald4.biblical.FamilyTreeCtrl = function($http, $scope, $window, globalData, familyTreeNodeService, biblicalEventService) {
  this.$http = $http;
  this.$scope = $scope;
  this.$window = $window;
  this.globalData = globalData;
  this.globalData.scriptureVersion = globalData.scriptureVersion || 'ISR';
  this.familyTreeNodeService = familyTreeNodeService;
  this.biblicalEventService = biblicalEventService;
  this.canvasWidth = 8192;
  this.canvasHeight = 16384;

  this.canvas = document.getElementById("family_canvas");
  if (!this.canvas.getContext) {
    alert('Canvas not supported, will not be able to view scroll');
    return;
  }
  this.addEventListeners();
  this.refresh();
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.addEventListeners = function() {
  var canvas = this.canvas;
  var $window = this.$window;
  this.selectedNodes = [];
  canvas.addEventListener('click', event => {
    event.preventDefault();
    var rect = canvas.getBoundingClientRect();
    var x = event.x - rect.left,
        y = event.y - rect.top;

    this.editTreeNode = undefined;
    this.familyTreeNodes.forEach(treeNode => {
      if (treeNode.x - FT_NODE_WIDTH / 2 <= x && treeNode.x + FT_NODE_WIDTH / 2 >= x &&
          treeNode.y - FT_NODE_HEIGHT / 2 <= y && treeNode.y + FT_NODE_HEIGHT / 2 >= y) {
        if (event.shiftKey) {
          if (this.selectedNodes.indexOf(treeNode) == -1) {
            this.selectedNodes.push(treeNode);
          } else {
            this.selectedNodes.splice(this.selectedNodes.indexOf(treeNode), 1);
          }
        } else {
          this.selectedNodes = [treeNode];
        }
        // this.editTreeNode = treeNode;
        this.drawScroll();
      }
    });
  });

  canvas.addEventListener('dblclick', event => {
    event.preventDefault();
    var rect = canvas.getBoundingClientRect();
    var x = event.x - rect.left,
        y = event.y - rect.top;

    this.viewNode = this.editTreeNode = undefined;
    this.familyTreeNodes.forEach(treeNode => {
      if (treeNode.x - FT_NODE_WIDTH / 2 <= x && treeNode.x + FT_NODE_WIDTH / 2 >= x &&
          treeNode.y - FT_NODE_HEIGHT / 2 <= y && treeNode.y + FT_NODE_HEIGHT / 2 >= y) {
        this.viewNode = treeNode;
        this.drawScroll();
        this.viewDialogShown = true;
      }
    });

    if (!this.viewNode) {
      this.showEditDialog({x: x, y: y});
    }

    this.$scope.$apply();
  });

  document.addEventListener('keydown', event => {
    if (this.editDialogShown || this.viewDialogShown) {
      return;
    }

    if (["ArrowUp","ArrowDown","ArrowLeft","ArrowRight"].indexOf(event.key) > -1) {
      event.preventDefault();
      var arrowKey = true;
      var step = event.shiftKey ? 1 : 10;
      var xMod = yMod = 0;
      switch (event.key) {
        case 'ArrowUp': yMod = -step; break;
        case 'ArrowDown': yMod = step; break;
        case 'ArrowLeft': xMod = -step; break;
        case 'ArrowRight': xMod = step; break;
      }
      this.selectedNodes.forEach(treeNode => {
        treeNode.y += yMod;
        treeNode.x += xMod;
        treeNode._state = 'modified';
      });
      this.drawScroll();
    }
  });
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.refresh = function() {
  this.ctx = this.canvas.getContext('2d');
  this.familyTreeNodes = [];

  this.familyTreeNodeService.list({pageSize: 0}, response => {
    console.log("Family Tree Nodes: " + response.items.length);
    this.familyTreeNodes = response.items;
    this.familyTreeNodes.push({id: undefined, name: ''});
    this.familyTreeNodes.push({id: undefined, name: '', female: true});
    this.nodesById = {};
    this.familyTreeNodes.forEach(treeNode => {this.nodesById[treeNode.id] = treeNode});
    this.drawScroll();
  });

  this.img = new Image();
  this.img.onload = () => {
    this.canvasWidth = this.img.width * 2;
    this.canvasHeight = this.img.height;
    this.$scope.$apply();
    this.drawScroll();
  }
  // this.img.src = `https://dss-images-dot-dd4-biblical.appspot.com/images/isaiah/columns/column_10_54.jpg`;
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.addChild = function(parentNode) {
  this.showEditDialog({x: parentNode.x, y: parentNode.y + FT_NODE_HEIGHT + 20,
      fatherId: parentNode.female ? undefined : parentNode.id, motherId: parentNode.female ? parentNode.id : undefined,
      summary: parentNode.summary});
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.addSibling = function(siblingNode) {
  this.showEditDialog({x: siblingNode.x + FT_NODE_WIDTH + 10, y: siblingNode.y,
      fatherId: siblingNode.fatherId, motherId: siblingNode.motherId, summary: siblingNode.summary});
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.showEditDialog = function(editNode) {
  this.editTreeNode = editNode;
  this.viewDialogShown = false;
  this.editDialogShown = true;
  this.dialogStyle = {top: this.$window.visualViewport.pageTop - 20};
  if (!this.allEvents) {
    this.biblicalEventService.listAll(response => {
      this.allEvents = response.items;
      this.allEvents.splice(0, 0, {id: undefined, title: ''});
    });
  }
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.saveAll = function() {
  var modified = [];
  this.familyTreeNodes.forEach(treeNode => {
    if (treeNode._state == 'modified' || treeNode._state == 'error') {
      modified.push(treeNode);
      treeNode._state = 'saving';
    }
  });

  this.familyTreeNodeService.batchCreate(modified, result => {
    modified.forEach(treeNode => {treeNode._state = undefined});
    this.drawScroll();
  }, error => {
    modified.forEach(treeNode => {treeNode._state = 'error'});
    notifyError(error);
    this.drawScroll();
  });
  this.drawScroll();
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.saveSelected = function() {
  var saveNode = this.editTreeNode;
  saveNode._state = 'saving';

  if (!saveNode.id && saveNode.fatherId && !saveNode.indirect) {
    // If we are creating a new entry and the father has been assigned.
    // The child 20 pixels below the father.
    saveNode.y = this.nodesById[saveNode.fatherId].y + FT_NODE_HEIGHT + 20;
  }
  this.familyTreeNodeService.create(saveNode, result => {
    saveNode._state = undefined;
    if (!saveNode.id) {
      this.familyTreeNodes.push(saveNode);
      this.nodesById[result.id] = saveNode;
    } else {
      this.editTreeNode = undefined;
    }
    saveNode.id = result.id;
    saveNode.summary = result.summary;
    saveNode.birthYear = result.birthYear;
    saveNode.deathYear = result.deathYear;
    this.closeDialog();
    this.selectedNodes = [saveNode];
    this.drawScroll();
  }, error => {
    saveNode._state = 'error';
    notifyError(error);
    this.drawScroll();
  });
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.scriptureVersionChanged = function() {
  this.viewNode.summary = this.viewNode.summary + ' ';
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.closeDialog = function() {
  this.editDialogShown = this.viewDialogShown = undefined;
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.drawScroll = function() {
  this.ctx.fillStyle = 'white';
  this.ctx.fillRect(0, 0, this.canvasWidth, this.canvasHeight);
  if (this.img.src) {
    this.ctx.drawImage(this.img, 0, 0);
  }
  this.familyTreeNodes.forEach(familyTreeNode => this.drawTreeNode(familyTreeNode));
}

com.digitald4.biblical.FamilyTreeCtrl.prototype.drawTreeNode = function(treeNode) {
  var ctx = this.ctx;
  var color;
  if (treeNode == this.editTreeNode || this.selectedNodes.indexOf(treeNode) != -1) {
    color = 'green';
  } else if (treeNode._state == 'saving') {
    color = 'yellow';
  } else if (treeNode._state == 'error') {
    color = 'red';
  } else if (treeNode._state == 'modified') {
    color = 'orange';
  } else if (treeNode.female) {
    color = 'purple';
  }

  ctx.beginPath();
  ctx.strokeStyle = color || 'blue';
  ctx.rect(treeNode.x - FT_NODE_WIDTH / 2, treeNode.y - FT_NODE_HEIGHT / 2, FT_NODE_WIDTH, FT_NODE_HEIGHT);
  ctx.stroke();

  if (treeNode.fatherId) {
    var fatherNode = this.nodesById[treeNode.fatherId];
    if (fatherNode) {
      ctx.beginPath();
      ctx.strokeStyle = 'blue';
      // ctx.lineWidth = 2;
      ctx.moveTo(treeNode.x, treeNode.y - FT_NODE_HEIGHT / 2);
      ctx.lineTo(treeNode.x, treeNode.y - FT_NODE_HEIGHT / 2 - 10);
      ctx.lineTo(fatherNode.x, treeNode.y - FT_NODE_HEIGHT / 2 - 10);
      ctx.lineTo(fatherNode.x, fatherNode.y + FT_NODE_HEIGHT / 2);
      if (treeNode.indirect) {
        ctx.setLineDash([5, 3]);
      }
      ctx.stroke();
      ctx.setLineDash([]);
    }
  }

  if (treeNode.motherId) {
    var motherNode = this.nodesById[treeNode.motherId];
    ctx.beginPath();
    ctx.strokeStyle = 'purple';
    // ctx.lineWidth = 2;
    ctx.moveTo(treeNode.x, treeNode.y - FT_NODE_HEIGHT / 2);
    ctx.lineTo(treeNode.x, treeNode.y - FT_NODE_HEIGHT / 2 - 10);
    ctx.lineTo(motherNode.x, treeNode.y - FT_NODE_HEIGHT / 2 - 10);
    ctx.lineTo(motherNode.x, motherNode.y + FT_NODE_HEIGHT / 2);
    ctx.stroke();
  }

  if (treeNode.husbandId) {
    var husbandNode = this.nodesById[treeNode.husbandId];
    ctx.beginPath();
    ctx.strokeStyle = 'red';
    // ctx.lineWidth = 2;
    if (treeNode.x < husbandNode.x) {
      ctx.moveTo(treeNode.x + FT_NODE_WIDTH / 2, treeNode.y);
      ctx.lineTo(husbandNode.x - FT_NODE_WIDTH / 2, husbandNode.y);
    } else {
      ctx.moveTo(treeNode.x - FT_NODE_WIDTH / 2, treeNode.y);
      ctx.lineTo(husbandNode.x + FT_NODE_WIDTH / 2, husbandNode.y);
    }
    ctx.stroke();
  }

  ctx.font = '15px Arial';
  ctx.fillStyle = color || 'black';
  ctx.fillText(treeNode.name, treeNode.x - FT_NODE_WIDTH / 2 + 2, treeNode.y - FT_NODE_HEIGHT / 2 + 13);

  if (treeNode.birthYear && treeNode.deathYear) {
    ctx.fillText(treeNode.birthYear + ' - ' + treeNode.deathYear,
        treeNode.x - FT_NODE_WIDTH / 2 + 2, treeNode.y + FT_NODE_HEIGHT / 2 - 2);
  } else if (treeNode.birthYear) {
    ctx.fillText(treeNode.birthYear + ' - ?',
        treeNode.x - FT_NODE_WIDTH / 2 + 2, treeNode.y + FT_NODE_HEIGHT / 2 - 2);
  }
}