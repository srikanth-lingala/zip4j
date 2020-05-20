package net.lingala.zip4j.model.enums;

import net.lingala.zip4j.exception.ZipException;

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
   * @throws ZipException on unknown code
   */
  public static CompressionMethod getCompressionMethodFromCode(int code) throws ZipException {
    for (CompressionMethod compressionMethod : values()) {
      if (compressionMethod.getCode() == code) {
        return compressionMethod;
      }
    }

    throw new ZipException("Unknown compression method", ZipException.Type.UNKNOWN_COMPRESSION_METHOD);
  }
}
