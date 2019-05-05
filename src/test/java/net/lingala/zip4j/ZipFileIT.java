package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.utils.ZipVerifier;
import net.lingala.zip4j.zip.AesKeyStrength;
import net.lingala.zip4j.zip.CompressionMethod;
import net.lingala.zip4j.zip.EncryptionMethod;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class ZipFileIT {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File generatedZipFile;
  private ZipVerifier zipVerifier = new ZipVerifier();

  @Before
  public void before() throws IOException {
    generatedZipFile = temporaryFolder.newFile("output.zip");

    File[] allTempFiles = temporaryFolder.getRoot().listFiles();
    Arrays.stream(allTempFiles).forEach(File::delete);
  }

  @Test
  public void testZipFileDeflateAndWithoutEncryption() throws ZipException, IOException {
    File fileToCompress = getFileFromResources("sample_text1.txt");
    ZipParameters zipParameters = new ZipParameters();

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(fileToCompress, zipParameters);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileDeflateAndWithStandardEncryption() throws ZipException, IOException {
    File fileToCompress = getFileFromResources("sample_text1.txt");

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
    zipParameters.setPassword("test".toCharArray());

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(fileToCompress, zipParameters);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileDeflateAndWithAesEncryptionKeyStrengthDefault() throws ZipException, IOException {
    File fileToCompress = getFileFromResources("sample_text1.txt");

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setPassword("test".toCharArray());

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(fileToCompress, zipParameters);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileDeflateAndWithAesEncryptionKeyStrength128() throws ZipException, IOException {
    File fileToCompress = getFileFromResources("sample_text1.txt");

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);
    zipParameters.setPassword("test".toCharArray());

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(fileToCompress, zipParameters);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileDeflateAndWithAesEncryptionKeyStrength256() throws ZipException, IOException {
    File fileToCompress = getFileFromResources("sample_text1.txt");

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setPassword("test".toCharArray());

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(fileToCompress, zipParameters);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileStoreAndWithoutEncryption() throws ZipException, IOException {
    File fileToCompress = getFileFromResources("sample_text1.txt");
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(fileToCompress, zipParameters);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileStoreAndWithStandardEncryption() throws ZipException, IOException {
    File fileToCompress = getFileFromResources("sample_text1.txt");

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
    zipParameters.setPassword("test".toCharArray());

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(fileToCompress, zipParameters);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileStoreAndWithAesEncryptionKeyStrengthDefault() throws ZipException, IOException {
    File fileToCompress = getFileFromResources("sample_text1.txt");

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setPassword("test".toCharArray());

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(fileToCompress, zipParameters);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileStoreAndWithAesEncryptionKeyStrength128() throws ZipException, IOException {
    File fileToCompress = getFileFromResources("sample_text1.txt");

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);
    zipParameters.setPassword("test".toCharArray());

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(fileToCompress, zipParameters);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileStoreAndWithAesEncryptionKeyStrength256() throws ZipException, IOException {
    File fileToCompress = getFileFromResources("sample_text1.txt");

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setPassword("test".toCharArray());

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(fileToCompress, zipParameters);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileStoreSplitFileAndWithAesEncryptionKeyStrength256() throws ZipException, IOException {
    List<File> filesToAdd = Arrays.asList(
        getFileFromResources("sample_text_large.txt"),
        getFileFromResources("sample_text1.txt"));

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setPassword("test".toCharArray());

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(filesToAdd, zipParameters, true, 65536);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipFileStoreSplitFileWithoutEncryption() throws ZipException, IOException {
    List<File> filesToAdd = Arrays.asList(
        getFileFromResources("sample_text_large.txt"),
        getFileFromResources("sample_text1.txt"));

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(false);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createZipFile(filesToAdd, zipParameters, true, 70536);

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  @Test
  public void testZipOutputStreamWithoutEncryption() throws IOException, ZipException {
    FileOutputStream fos = new FileOutputStream(generatedZipFile);
    ZipOutputStream zos = new ZipOutputStream(fos);

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
    zipParameters.setEncryptFiles(false);
    zipParameters.setSourceExternalStream(true);

    List<String> fileNamesToAddToZip = Arrays.asList("sample_text1.txt");
    byte[] buff = new byte[4096];
    int readLen;

    for (String fileNameToAddToZip : fileNamesToAddToZip) {
      zipParameters.setFileNameInZip(fileNameToAddToZip);
      zos.putNextEntry(null, zipParameters);

      InputStream inputStream = new FileInputStream(getFileFromResources(fileNameToAddToZip));
      while ((readLen = inputStream.read(buff)) != -1) {
        zos.write(buff, 0, readLen);
      }
      zos.closeEntry();
    }

    zos.close();

    zipVerifier.verifyZipFile(generatedZipFile, zipParameters, temporaryFolder);
  }

  private File getFileFromResources(String fileName) {
    return new File(this.getClass().getResource(System.getProperty("file.separator") + fileName).getFile());
  }

}
