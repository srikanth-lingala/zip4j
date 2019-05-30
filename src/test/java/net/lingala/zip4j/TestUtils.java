package net.lingala.zip4j;

import java.io.File;

public class TestUtils {

  private static final String TEST_FILES_FOLDER_NAME = "test-files";

  public static File getFileFromResources(String fileName) {
    final String path = "/" + TEST_FILES_FOLDER_NAME + "/" + fileName;
    return new File(TestUtils.class.getResource(path).getFile());
  }

}
