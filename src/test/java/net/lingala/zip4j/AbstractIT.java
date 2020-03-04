package net.lingala.zip4j;

import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.testutils.TestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static net.lingala.zip4j.testutils.TestUtils.getTestFileFromResources;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractIT {

  protected static final char[] PASSWORD = "test123!".toCharArray();
  protected static final List<File> FILES_TO_ADD = Arrays.asList(
      getTestFileFromResources("sample_text1.txt"),
      getTestFileFromResources("sample_text_large.txt"),
      getTestFileFromResources("sample.pdf")
  );
  protected static final Charset CHARSET_MS_932 = Charset.forName("Ms932");
  protected static final Charset CHARSET_GBK = Charset.forName("GBK");
  protected static final Charset CHARSET_CP_949 = Charset.forName("Cp949");

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

  protected void verifyFileHeadersContainsFiles(List<FileHeader> fileHeaders, List<String> fileNames) {
    for (String fileName : fileNames) {
      boolean fileFound = false;
      for (FileHeader fileHeader : fileHeaders) {
        if (fileHeader.getFileName().equals(fileName)) {
          fileFound = true;
          break;
        }
      }

      assertThat(fileFound).as("File with name %s not found in zip file", fileName).isTrue();
    }
  }

  protected File getTestArchiveFromResources(String archiveName) {
    return TestUtils.getTestArchiveFromResources(archiveName);
  }

  protected void cleanupOutputFolder() {
    File[] filesInOutputFolder = outputFolder.listFiles();

    if (filesInOutputFolder == null) {
      return;
    }

    Arrays.stream(filesInOutputFolder).forEach(File::delete);
  }
}
