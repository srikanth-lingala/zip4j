package net.lingala.zip4j.io.outputstream;

import net.lingala.zip4j.AbstractIT;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static net.lingala.zip4j.utils.ZipFileVerifier.verifyZipFileByExtractingAllFiles;

public class ZipOutputStreamIT extends AbstractIT {

  @Test
  public void testZipOutputStreamStoreWithoutEncryption() throws IOException, ZipException {
    testZipOutputStream(CompressionMethod.STORE, false, null, null);
  }

  @Test
  public void testZipOutputStreamStoreWithStandardEncryption() throws IOException, ZipException {
    testZipOutputStream(CompressionMethod.STORE, true, EncryptionMethod.ZIP_STANDARD, null);
  }

  @Test
  public void testZipOutputStreamStoreWithAES128() throws IOException, ZipException {
    testZipOutputStream(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
  }

  @Test
  public void testZipOutputStreamStoreWithAES256() throws IOException, ZipException {
    testZipOutputStream(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
  }

  @Test
  public void testZipOutputStreamDeflateWithoutEncryption() throws IOException, ZipException {
    testZipOutputStream(CompressionMethod.DEFLATE, false, null, null);
  }

  @Test
  public void testZipOutputStreamDeflateWithStandardEncryption() throws IOException, ZipException {
    testZipOutputStream(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null);
  }

  @Test
  public void testZipOutputStreamDeflateWithAES128() throws IOException, ZipException {
    testZipOutputStream(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
  }

  @Test
  public void testZipOutputStreamDeflateWithAES256() throws IOException, ZipException {
    testZipOutputStream(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
  }

  private void testZipOutputStream(CompressionMethod compressionMethod, boolean encrypt,
                                   EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength)
      throws IOException, ZipException {

    ZipParameters zipParameters = buildZipParameters(compressionMethod, encrypt, encryptionMethod, aesKeyStrength);
    byte[] buff = new byte[4096];
    int readLen;

    try(ZipOutputStream zos = initializeZipOutputStream(encrypt)) {
      for (File fileToAdd : FILES_TO_ADD) {

        if (zipParameters.getCompressionMethod() == CompressionMethod.STORE) {
          zipParameters.setEntrySize(fileToAdd.length());
        }

        zipParameters.setFileNameInZip(fileToAdd.getName());
        zos.putNextEntry(zipParameters);

        try(InputStream inputStream = new FileInputStream(fileToAdd)) {
          while ((readLen = inputStream.read(buff)) != -1) {
            zos.write(buff, 0, readLen);
          }
        }
        zos.closeEntry();
      }
    }
    verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
  }

  private ZipOutputStream initializeZipOutputStream(boolean encrypt) throws IOException {
    FileOutputStream fos = new FileOutputStream(generatedZipFile);

    if (encrypt) {
      return new ZipOutputStream(fos, PASSWORD);
    }

    return new ZipOutputStream(fos);
  }

  private ZipParameters buildZipParameters(CompressionMethod compressionMethod, boolean encrypt,
                                           EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength) {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(compressionMethod);
    zipParameters.setEncryptionMethod(encryptionMethod);
    zipParameters.setAesKeyStrength(aesKeyStrength);
    zipParameters.setEncryptFiles(encrypt);
    return zipParameters;
  }
}