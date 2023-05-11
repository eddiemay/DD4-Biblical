com.digitald4.biblical.FlashCardsCtrl = function() {
  this.tabs = ['Review', 'Test'];
  this.testOptions = ['Name', 'Modern', 'Paleo', 'Ancient', 'English',
      'Meaning', 'Value', 'Random', 'Mixed'];
  this.selectedTab = this.tabs[0];
  this.cardIndex = 0;
  this.cards = [];
  var i = 0;
  for (var letter in AlefBet) {
    this.cards.push(AlefBet[letter]);
  }
  this.card = this.cards[this.cardIndex];
}

com.digitald4.biblical.FlashCardsCtrl.prototype.setSelectedTab = function(tab) {
  this.selectedTab = tab;
}

com.digitald4.biblical.FlashCardsCtrl.prototype.prevCard = function() {
  this.cardIndex--;
  if (this.cardIndex < 0) {
    this.cardIndex += this.cards.length;
  }
  this.card = this.cards[this.cardIndex];
}

com.digitald4.biblical.FlashCardsCtrl.prototype.nextCard = function() {
  this.cardIndex++;
  if (this.cardIndex >= this.cards.length) {
    this.cardIndex = 0;
  }
  this.card = this.cards[this.cardIndex];
}