package net.lingala.zip4j.utils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static net.lingala.zip4j.TestUtils.getFileFromResources;

public abstract class AbstractIT {

  protected static final char[] PASSWORD = "test123!".toCharArray();
  protected static final List<File> FILES_TO_ADD = Arrays.asList(
      getFileFromResources("sample_text1.txt"),
      getFileFromResources("sample_text_large.txt"),
      getFileFromResources("sample.pdf")
  );

  protected File generatedZipFile;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void before() throws IOException {
    generatedZipFile = temporaryFolder.newFile("output.zip");
    File[] allTempFiles = temporaryFolder.getRoot().listFiles();
    Arrays.stream(allTempFiles).forEach(File::delete);
  }

}
