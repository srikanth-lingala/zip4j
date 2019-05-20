package net.lingala.zip4j;

import java.io.File;

public class TestUtils {

  private static final String TEST_FILES_FOLDER_NAME = "test-files";

  public static File getFileFromResources(String fileName) {
    return new File(TestUtils.class.getResource(
        System.getProperty("file.separator")
            + TEST_FILES_FOLDER_NAME
            + System.getProperty("file.separator")
            + fileName)
        .getFile());
  }

}
