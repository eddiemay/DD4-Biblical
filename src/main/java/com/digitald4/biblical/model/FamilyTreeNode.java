package com.digitald4.biblical.model;

import com.digitald4.common.model.ModelObject;

public class FamilyTreeNode extends ModelObject<Long> {
  private String name;
  private Boolean female;
  private StringBuilder summary;
  private String strongsId;
  private Long eventId;
  private Long fatherId;
  private Boolean indirect;
  private Long motherId;
  private Long husbandId;
  private int x;
  private int y;
  private Integer birthYear;
  private Integer deathYear;

  public String getName() {
    return name;
  }

  public FamilyTreeNode setName(String name) {
    this.name = name;
    return this;
  }

  public Boolean getFemale() {
    return female;
  }

  public FamilyTreeNode setFemale(Boolean female) {
    this.female = female;
    return this;
  }

  public StringBuilder getSummary() {
    return summary;
  }

  public FamilyTreeNode setSummary(StringBuilder summary) {
    this.summary = summary;
    return this;
  }

  public FamilyTreeNode setSummary(String summary) {
    this.summary = new StringBuilder(summary);
    return this;
  }

  public String getStrongsId() {
    return strongsId;
  }

  public FamilyTreeNode setStrongsId(String strongsId) {
    this.strongsId = strongsId;
    return this;
  }

  public Long getEventId() {
    return eventId;
  }

  public FamilyTreeNode setEventId(Long eventId) {
    this.eventId = eventId;
    return this;
  }

  public Long getFatherId() {
    return fatherId;
  }

  public FamilyTreeNode setFatherId(Long fatherId) {
    this.fatherId = fatherId;
    return this;
  }

  public Boolean getIndirect() {
    return indirect;
  }

  public FamilyTreeNode setIndirect(Boolean indirect) {
    this.indirect = indirect;
    return this;
  }

  public Long getMotherId() {
    return motherId;
  }

  public FamilyTreeNode setMotherId(Long motherId) {
    this.motherId = motherId;
    return this;
  }

  public Long getHusbandId() {
    return husbandId;
  }

  public FamilyTreeNode setHusbandId(Long husbandId) {
    this.husbandId = husbandId;
    return this;
  }

  public int getX() {
    return x;
  }

  public FamilyTreeNode setX(int x) {
    this.x = x;
    return this;
  }

  public int getY() {
    return y;
  }

  public FamilyTreeNode setY(int y) {
    this.y = y;
    return this;
  }

  public Integer getBirthYear() {
    return birthYear;
  }

  public FamilyTreeNode setBirthYear(Integer birthYear) {
    this.birthYear = birthYear;
    return this;
  }

  public Integer getDeathYear() {
    return deathYear;
  }

  public FamilyTreeNode setDeathYear(Integer deathYear) {
    this.deathYear = deathYear;
    return this;
  }
}
