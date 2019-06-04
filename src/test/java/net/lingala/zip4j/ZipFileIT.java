package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.utils.AbstractIT;
import org.junit.Test;

import java.io.IOException;

import static net.lingala.zip4j.utils.ZipVerifier.verifyZipFile;

public class ZipFileIT extends AbstractIT {
  
  @Test
  public void testZipFileDeflateAndWithoutEncryption() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFile(generatedZipFile, temporaryFolder);
  }

  @Test
  public void testZipFileDeflateAndWithStandardEncryption() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

  @Test
  public void testZipFileDeflateAndWithAesEncryptionKeyStrengthDefault() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

  @Test
  public void testZipFileDeflateAndWithAesEncryptionKeyStrength128() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

  @Test
  public void testZipFileDeflateAndWithAesEncryptionKeyStrength256() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

  @Test
  public void testZipFileStoreAndWithoutEncryption() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFile(generatedZipFile, temporaryFolder);
  }

  @Test
  public void testZipFileStoreAndWithStandardEncryption() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

  @Test
  public void testZipFileStoreAndWithAesEncryptionKeyStrengthDefault() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

  @Test
  public void testZipFileStoreAndWithAesEncryptionKeyStrength128() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

  @Test
  public void testZipFileStoreAndWithAesEncryptionKeyStrength256() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

  @Test
  public void testZipFileStoreSplitFileAndWithAesEncryptionKeyStrength256() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, true, 65536);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

  @Test
  public void testZipFileStoreSplitFileWithoutEncryption() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(false);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, true, 70536);

    verifyZipFile(generatedZipFile, temporaryFolder);
  }

  @Test
  public void testZipFileDeflateSplitFileWithoutEncryption() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
    zipParameters.setEncryptFiles(false);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, true, 70536);

    verifyZipFile(generatedZipFile, temporaryFolder);
  }

  @Test
  public void testSplitZipFileDeflateAndWithAesEncryptionKeyStrength256() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, true, 65536);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

  @Test
  public void testSplitZipFileDeflateAndWithAesEncryptionKeyStrength128() throws ZipException, IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, true, 65536);

    verifyZipFile(generatedZipFile, temporaryFolder, PASSWORD);
  }

}
