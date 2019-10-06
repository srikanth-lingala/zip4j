package net.lingala.zip4j.testutils;

import net.lingala.zip4j.util.InternalZipConstants;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class TestUtils {

  private static final String TEST_FILES_FOLDER_NAME = "test-files";
  private static final String TEST_ARCHIVES_FOLDER_NAME = "test-archives";

  public static File getTestFileFromResources(String fileName) {
    return getFileFromResources(TEST_FILES_FOLDER_NAME, fileName);
  }

  public static File getTestArchiveFromResources(String fileName) {
   return getFileFromResources(TEST_ARCHIVES_FOLDER_NAME, fileName);
  }

  public static Boolean isWindows() {
    String os = System.getProperty("os.name").toLowerCase();
    return (os.contains("win"));
  }

  private static File getFileFromResources(String parentFolder, String fileName) {
    try {
      String path = "/" + parentFolder + "/" + fileName;
      String utfDecodedFilePath = URLDecoder.decode(TestUtils.class.getResource(path).getFile(),
          InternalZipConstants.CHARSET_UTF_8.toString());
      return new File(utfDecodedFilePath);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
