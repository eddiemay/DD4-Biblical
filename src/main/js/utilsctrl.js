com.digitald4.biblical.UtilsCtrl = function() {
  this.jubilee = 1;
  this.week = 1;
  this.year = 1;
  this.calcJubileeYear();
}

com.digitald4.biblical.UtilsCtrl.prototype.calcJubileeYear = function() {
  this.jubileeYear = (this.jubilee - 1) * 50 + (this.week - 1) * 7 + this.year;
}

com.digitald4.biblical.UtilsCtrl.prototype.incrementJubileeYear = function() {
  this.jubileeYear += 1;
  this.calcJubileeComponents();
}

com.digitald4.biblical.UtilsCtrl.prototype.decrementJubileeYear = function() {
  this.jubileeYear -= 1;
  this.calcJubileeComponents();
}

com.digitald4.biblical.UtilsCtrl.prototype.calcJubileeComponents = function() {
  this.jubilee = Math.ceil(this.jubileeYear / 50);
  var mod50 = this.jubileeYear % 50;
  if (mod50 == 0) {
    this.week = 0;
    this.year = 0;
    return;
  }

  this.week = Math.floor((mod50 - 1) / 7) + 1;
  this.year = mod50 % 7;
  if (this.year == 0) {
    this.year = 7;
  }
}