package net.lingala.zip4j.model.enums;

/**
 * Indicates the algorithm used for compression
 *
 */
public enum CompressionMethod {

  /**
   * No compression is performed 
   */
  STORE(0),
  /**
   * The Deflate compression is used.
   * @see java.util.zip.Deflater 
   */
  DEFLATE(8),
  /**
   * For internal use in Zip4J
   */
  AES_INTERNAL_ONLY(99);

  private int code;

  CompressionMethod(int code) {
    this.code = code;
  }

  /**
   * Get the code used in the ZIP file for this CompressionMethod
   * @return the code used in the ZIP file
   */
  public int getCode() {
    return code;
  }

  /**
   * Get the CompressionMethod for a given ZIP file code
   * @param code the code for a compression method
   * @return the CompressionMethod related to the given code
   */
  public static CompressionMethod getCompressionMethodFromCode(int code) {
    for (CompressionMethod compressionMethod : values()) {
      if (compressionMethod.getCode() == code) {
        return compressionMethod;
      }
    }

    System.out.println("ZIP4J WARNING: Unknown ZIP compression method, defaulting to no compression.");
    return STORE;
  }
}
