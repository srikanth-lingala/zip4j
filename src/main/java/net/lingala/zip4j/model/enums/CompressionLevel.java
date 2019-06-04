package net.lingala.zip4j.model.enums;

public enum CompressionLevel {

  FASTEST(1),
  FAST(3),
  NORMAL(5),
  MAXIMUM(7);

  private int level;

  CompressionLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }
}
