package com.digitald4.biblical.model;

import com.digitald4.common.model.ModelObjectModUser;

public class LetterBox extends ModelObjectModUser<Long> {
  private String filename;
  public enum Type {Letter, Word, Row};
  private Type type = Type.Letter;
  private int x1;
  private int y1;
  private int x2;
  private int y2;
  private String value;

  public String getFilename() {
    return filename;
  }

  public LetterBox setFilename(String filename) {
    this.filename = filename;
    return this;
  }

  public Type getType() {
    return type;
  }

  public LetterBox setType(Type type) {
    this.type = type;
    return this;
  }

  public int getX1() {
    return x1;
  }

  public LetterBox setX1(int x1) {
    this.x1 = x1;
    return this;
  }

  public int getY1() {
    return y1;
  }

  public LetterBox setY1(int y1) {
    this.y1 = y1;
    return this;
  }

  public int getX2() {
    return x2;
  }

  public LetterBox setX2(int x2) {
    this.x2 = x2;
    return this;
  }

  public int getY2() {
    return y2;
  }

  public LetterBox setY2(int y2) {
    this.y2 = y2;
    return this;
  }

  @Deprecated
  public String getLetter() {
    return null;
  }

  @Deprecated
  public LetterBox setLetter(String letter) {
    return setValue(letter);
  }

  public String getValue() {
    return value;
  }

  public LetterBox setValue(String value) {
    this.value = value;
    return this;
  }
}
