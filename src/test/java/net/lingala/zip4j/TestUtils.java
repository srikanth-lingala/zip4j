package net.lingala.zip4j;

import java.io.File;

public class TestUtils {

  public static File getFileFromResources(String fileName) {
    return new File(TestUtils.class.getResource(System.getProperty("file.separator") + fileName).getFile());
  }

}
