package net.lingala.zip4j.model.enums;

/**
 * Indicates the level of compression for the DEFLATE compression method
 *
 */
public enum CompressionLevel {

  /**
   * Level 1 Deflate compression
   * @see java.util.zip.Deflater#BEST_SPEED
   */
  FASTEST(1),
  /**
   * Level 3 Deflate compression
   * @see java.util.zip.Deflater
   */
  FAST(3),
  /**
   * Level 5 Deflate compression
   * @see java.util.zip.Deflater
   */
  NORMAL(5),
  /**
   * Level 7 Deflate compression
   * @see java.util.zip.Deflater
   */
  MAXIMUM(7),
  /**
   * Level 9 Deflate compression. Not part of the original ZIP format specification.
   * @see java.util.zip.Deflater#BEST_COMPRESSION
   */
  ULTRA(9);

  private int level;

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
