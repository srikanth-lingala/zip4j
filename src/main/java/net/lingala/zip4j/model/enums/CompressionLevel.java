package net.lingala.zip4j.model.enums;

/**
 * Indicates the level of compression for the DEFLATE compression method
 *
 */
public enum CompressionLevel {

  /**
   * Level 0 - No compression
   */
  NO_COMPRESSION(0),
  /**
   * Level 1 Deflate compression. Fastest compression.
   */
  FASTEST(1),
  /**
   * Level 2 Deflate compression
   */
  FASTER(2),
  /**
   * Level 3 Deflate compression
   */
  FAST(3),
  /**
   * Level 4 Deflate compression
   */
  MEDIUM_FAST(4),
  /**
   * Level 5 Deflate compression. A compromise between speed and compression level.
   */
  NORMAL(5),
  /**
   * Level 6 Deflate compression
   */
  HIGHER(6),
  /**
   * Level 7 Deflate compression
   */
  MAXIMUM(7),
  /**
   * Level 8 Deflate compression
   */
  PRE_ULTRA(8),
  /**
   * Level 9 Deflate compression. Highest compression.
   */
  ULTRA(9);

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
