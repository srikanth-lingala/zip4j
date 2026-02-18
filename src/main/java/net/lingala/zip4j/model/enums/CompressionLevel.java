package net.lingala.zip4j.model.enums;

/**
 * Indicates the level of compression for the DEFLATE compression method
 *
 */
public enum CompressionLevel {

  /**
   * Level 1 Deflate compression. Fastest compression.
   */
  FASTEST(1),
  /**
   * Level 3 Deflate compression
   */
  FAST(3),
  /**
   * Level 5 Deflate compression. A compromise between speed and compression level.
   */
  NORMAL(5),
  /**
   * Level 9 Deflate compression. Highest compression.
   */
  MAXIMUM(9);

  private final int level;

  CompressionLevel(int level) {
    this.level = level;
  }

  /**
   * Get the Deflate compression level (0-9) for this CompressionLevel
   * @return the deflate compression level
   */
  public int getLevel() {
    return level;
  }
}
