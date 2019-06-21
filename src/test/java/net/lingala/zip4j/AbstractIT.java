package net.lingala.zip4j;

import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static net.lingala.zip4j.utils.TestUtils.getFileFromResources;

public abstract class AbstractIT {

  protected static final char[] PASSWORD = "test123!".toCharArray();
  protected static final List<File> FILES_TO_ADD = Arrays.asList(
      getFileFromResources("sample_text1.txt"),
      getFileFromResources("sample_text_large.txt"),
      getFileFromResources("sample.pdf")
  );

  protected File generatedZipFile;
  protected File outputFolder;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void before() throws IOException {
    generatedZipFile = temporaryFolder.newFile("output.zip");
    outputFolder = temporaryFolder.newFolder("output");
    File[] allTempFiles = temporaryFolder.getRoot().listFiles();
    Arrays.stream(allTempFiles).forEach(File::delete);
  }

  protected ZipParameters createZipParameters(EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength) {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(encryptionMethod);
    zipParameters.setAesKeyStrength(aesKeyStrength);
    return zipParameters;
  }
}
