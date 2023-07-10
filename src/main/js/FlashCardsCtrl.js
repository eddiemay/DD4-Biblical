com.digitald4.biblical.FlashCardsCtrl = function(highScoreService, userPreferences) {
  this.highScoreService = highScoreService;
  this.userPreferences = userPreferences;
  this.tabs = ['Review', 'Test'];
  this.testOptions = ['Name', 'Modern', 'Paleo', 'Ancient', 'English', 'Aramaic',
      'Meaning', 'Value', 'Random', 'Mixed'];
  this.testStates = {
    NOT_STARTED: 0,
    IN_PROGRESS: 1,
    FINISHED: 2
  }
  this.difficulty = {
    EASY: 'Easy',
    MEDIUM: 'Medium',
    HARD: 'Hard'
  }
  this.order = {
    FORWARD: 'Forward',
    REVERSE: 'Reverse',
    RANDOM: 'Random'
  }
  this.selectedTab = this.tabs[0];
  this.cardIndex = 0;
  this.cards = [];
  var i = 0;
  for (var letter in AlefBet) {
    this.cards.push(AlefBet[letter]);
  }
  this.card = this.cards[this.cardIndex];
  this.test = {
      game: 'flashcard',
      questionSet: 'Modern',
      answerSet: 'Name',
      difficulty: this.difficulty.EASY,
      order: this.order.RANDOM,
      state: this.testStates.NOT_STARTED,
      name: userPreferences.get('name')
  };
  this.test.config = () => {
    return this.test.questionSet + '-' + this.test.answerSet +
    '-' + this.test.difficulty + '-' + this.test.order};
  this.refreshHighScoreList();
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

getTestAttribute = function(testOption) {
  if (testOption == 'Random' || testOption == 'mixed') {
    testOption = this.testOptions[
        Math.floor(Math.random() * this.testOptions.length - 2)];
  }

  return testOption.toLowerCase();
}

com.digitald4.biblical.FlashCardsCtrl.prototype.getAnswerOption =
    function(attribute, currentSelections, answer) {
  var value = answer;
  while (value == answer || currentSelections.includes(value)) {
    value = this.cards[Math.floor(Math.random() * this.cards.length)][attribute];
  }

  return value;
}

com.digitald4.biblical.FlashCardsCtrl.prototype.orderQuestions =
    function(questions) {
  if (this.test.order == this.order.FORWARD) {
    return questions;
  }

  if (this.test.order == this.order.REVERSE) {
    var reversed = [];
    while (questions.length > 0) {
      reversed.push(questions.pop());
    }
    return reversed;
  }

  var randomized = [];
  while (questions.length > 0) {
    var index = Math.floor(Math.random() * questions.length);
    randomized.push(questions[index]);
    questions.splice(index, 1);
  }
  return randomized;
}

com.digitald4.biblical.FlashCardsCtrl.prototype.startTest = function() {
  this.userPreferences.put('name', this.test.name);
  var questions = [];
  var questionAttribute = getTestAttribute(this.test.questionSet);
  var answerAttribute = getTestAttribute(this.test.answerSet);
  var answerCount = this.test.difficulty == this.difficulty.MEDIUM ? 8 : 4;
  if (this.test.difficulty == this.difficulty.HARD) {
    answerCount = 16;
  }
  for (var c = 0; c < this.cards.length; c++) {
    var card = this.cards[c];
    var aa = getTestAttribute(answerAttribute);
    var question = {
      card: card,
      text: card[getTestAttribute(questionAttribute)],
      answer: card[aa]};
    if (question.text && question.answer) {
      question.options = [];
      var answerIndex = Math.floor(Math.random() * answerCount);
      for (var o = 0; o < answerCount; o++) {
        if (o != answerIndex) {
          question.options.push(
              this.getAnswerOption(aa, question.options, question.answer));
        } else {
          question.options.push(question.answer);
        }
      }
      questions.push(question);
    }
  }
  this.test.questions = this.orderQuestions(questions);
  this.test.questionIndex = 0;
  this.test.question = this.test.questions[this.test.questionIndex];
  this.test.state = this.testStates.IN_PROGRESS;
  this.test.startTime = Date.now();
}

com.digitald4.biblical.FlashCardsCtrl.prototype.prevQuestion = function() {
  this.test.questionIndex--;
  this.test.question = this.test.questions[this.test.questionIndex];
}

com.digitald4.biblical.FlashCardsCtrl.prototype.nextQuestion = function() {
  this.test.questionIndex++;
  if (this.test.questionIndex == this.test.questions.length) {
    this.calcTestResults();
  }
  this.test.question = this.test.questions[this.test.questionIndex];
}

com.digitald4.biblical.FlashCardsCtrl.prototype.calcTestResults = function() {
  var test = this.test;
  test.endTime = Date.now();
  test.state = this.testStates.FINISHED;
  test.elapsedTime = test.endTime - test.startTime;
  test.correct = 0;
  for (var i = 0; i < test.questions.length; i++) {
    var question = test.questions[i];
    if (question.answer == question.selectedAnswer) {
      question.result = 'Correct';
      test.correct++;
    } else {
      question.result = 'Incorrect';
    }
  }
  test.percentCorrect = Math.round(test.correct * 100 / test.questions.length, 1);

  var highScore = {
    game: test.game,
    config: test.config(),
    name: test.name,
    score: test.percentCorrect,
    startTime: test.startTime,
    endTime: test.endTime,
    elapsedTime: test.elapsedTime
  };
  test.rank = undefined;
  test.highScores = undefined;
  this.highScoreService.create(highScore, rank => {
    test.rank = rank;
    this.refreshHighScoreList();
  });
}

com.digitald4.biblical.FlashCardsCtrl.prototype.refreshHighScoreList = function() {
  this.highScoreService.list(
      this.test.game, this.test.config(), 20, 0,
      response => {this.test.highScores = response.items});
}

com.digitald4.biblical.FlashCardsCtrl.prototype.resetTest = function() {
  this.test.state = this.testStates.NOT_STARTED;
}