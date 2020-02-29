package net.lingala.zip4j.io.inputstream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A split input stream for zip file split as per zip specification. They end with .z01, .z02... .zip
 */
public class ZipStandardSplitInputStream extends SplitInputStream {

  private int lastSplitZipFileNumber;

  public ZipStandardSplitInputStream(File zipFile, boolean isSplitZipArchive, int lastSplitZipFileNumber) throws FileNotFoundException {
    super(zipFile, isSplitZipArchive, lastSplitZipFileNumber);
    this.lastSplitZipFileNumber = lastSplitZipFileNumber;
  }

  @Override
  protected File getNextSplitFile(int zipFileIndex) throws IOException {
    if (zipFileIndex == lastSplitZipFileNumber) {
      return zipFile;
    }

    String currZipFileNameWithPath = zipFile.getCanonicalPath();
    String extensionSubString = ".z0";
    if (zipFileIndex >= 9) {
      extensionSubString = ".z";
    }

    return new File(currZipFileNameWithPath.substring(0,
        currZipFileNameWithPath.lastIndexOf(".")) + extensionSubString + (zipFileIndex + 1));
  }
}
