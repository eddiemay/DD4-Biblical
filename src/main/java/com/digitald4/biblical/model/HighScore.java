package com.digitald4.biblical.model;

import java.time.Instant;

public class HighScore {
  private long id;
  private String game;
  private String config;

  private String name;
  private double score;
  private Instant startTime;
  private Instant endTime;
  private long elapsedTime;

  private String ipAddress;

  public long getId() {
    return id;
  }

  public HighScore setId(long id) {
    this.id = id;
    return this;
  }

  public String getGame() {
    return game;
  }

  public HighScore setGame(String game) {
    this.game = game;
    return this;
  }

  public String getConfig() {
    return config;
  }

  public HighScore setConfig(String config) {
    this.config = config;
    return this;
  }

  public String getName() {
    return name;
  }

  public HighScore setName(String name) {
    this.name = name;
    return this;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public HighScore setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }

  public double getScore() {
    return score;
  }

  public HighScore setScore(double score) {
    this.score = score;
    return this;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public HighScore setStartTime(Instant startTime) {
    this.startTime = startTime;
    return this;
  }

  public HighScore setStartTime(long startTime) {
    this.startTime = Instant.ofEpochMilli(startTime);
    return this;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public HighScore setEndTime(Instant endTime) {
    this.endTime = endTime;
    return this;
  }

  public HighScore setEndTime(long endTime) {
    this.endTime = Instant.ofEpochMilli(endTime);
    return this;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  public HighScore setElapsedTime(long elapsedTime) {
    this.elapsedTime = elapsedTime;
    return this;
  }
}
