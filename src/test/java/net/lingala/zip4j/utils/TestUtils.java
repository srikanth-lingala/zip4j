package net.lingala.zip4j.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class TestUtils {

  private static final String TEST_FILES_FOLDER_NAME = "test-files";

  public static File getFileFromResources(String fileName) {
    try {
      String path = "/" + TEST_FILES_FOLDER_NAME + "/" + fileName;
      String utfDecodedFilePath = URLDecoder.decode(TestUtils.class.getResource(path).getFile(),
          StandardCharsets.UTF_8.toString());
      return new File(utfDecodedFilePath);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
