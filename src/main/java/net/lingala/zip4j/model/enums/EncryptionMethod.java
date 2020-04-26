package net.lingala.zip4j.model.enums;

/**
 * Indicates the encryption method used in the ZIP file
 *
 */
public enum EncryptionMethod {

  /**
   * No encryption is performed 
   */
  NONE,
  /**
   * Encrypted with the weak ZIP standard algorithm 
   */
  ZIP_STANDARD,
  /**
   * Encrypted with the stronger ZIP standard algorithm 
   */
  ZIP_STANDARD_VARIANT_STRONG,
  /**
   * Encrypted with AES, the strongest choice but currently
   * cannot be expanded in Windows Explorer 
   */
  AES

}
