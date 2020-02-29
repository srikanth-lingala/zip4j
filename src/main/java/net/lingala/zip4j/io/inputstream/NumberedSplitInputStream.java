package net.lingala.zip4j.io.inputstream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static net.lingala.zip4j.util.FileUtils.getNextNumberedSplitFileCounterAsExtension;

/**
 * A split input stream for zip file split with 7-zip. They end with .zip.001, .zip.002, etc
 */
public class NumberedSplitInputStream extends SplitInputStream {

  public NumberedSplitInputStream(File zipFile, boolean isSplitZipArchive, int lastSplitZipFileNumber)
      throws FileNotFoundException {
    super(zipFile, isSplitZipArchive, lastSplitZipFileNumber);
  }

  @Override
  protected File getNextSplitFile(int zipFileIndex) throws IOException {
    String currZipFileNameWithPath = zipFile.getCanonicalPath();
    String fileNameWithPathAndWithoutExtension = currZipFileNameWithPath.substring(0,
        currZipFileNameWithPath.lastIndexOf("."));
    return new File(fileNameWithPathAndWithoutExtension + getNextNumberedSplitFileCounterAsExtension(zipFileIndex));
  }
}
