package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class SplitInputStream extends InputStream {

  private RandomAccessFile randomAccessFile;
  private File zipFile;
  private int lastSplitZipFileNumber;
  private boolean isSplitZipArchive;
  private int currentSplitFileCounter = 0;
  private byte[] singleByteArray = new byte[1];

  public SplitInputStream(File zipFile, boolean isSplitZipArchive, int lastSplitZipFileNumber) throws FileNotFoundException {
    this.randomAccessFile = new RandomAccessFile(zipFile, RandomAccessFileMode.READ.getValue());
    this.zipFile = zipFile;
    this.isSplitZipArchive = isSplitZipArchive;
    this.lastSplitZipFileNumber = lastSplitZipFileNumber;

    if (isSplitZipArchive) {
      currentSplitFileCounter = lastSplitZipFileNumber;
    }
  }

  @Override
  public int read() throws IOException {
    int readLen = read(singleByteArray);
    if (readLen == -1) {
      return -1;
    }

    return singleByteArray[0];
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int readLen = randomAccessFile.read(b, off, len);

    if ((readLen != len || readLen == -1) && isSplitZipArchive) {
      openRandomAccessFileForIndex(currentSplitFileCounter + 1);
      currentSplitFileCounter++;

      if (readLen < 0) readLen = 0;
      int newlyRead = randomAccessFile.read(b, readLen, len - readLen);
      if (newlyRead > 0) readLen += newlyRead;
    }

    return readLen;
  }

  public void prepareExtractionForFileHeader(FileHeader fileHeader) throws IOException {

    if (isSplitZipArchive && (currentSplitFileCounter != fileHeader.getDiskNumberStart())) {
      openRandomAccessFileForIndex(fileHeader.getDiskNumberStart());
      currentSplitFileCounter = fileHeader.getDiskNumberStart();
    }

    randomAccessFile.seek(fileHeader.getOffsetLocalHeader());
  }

  private void openRandomAccessFileForIndex(int zipFileIndex) throws IOException {
    File nextSplitFile = getNextSplitFileName(zipFileIndex);
    if (!nextSplitFile.exists()) {
      throw new FileNotFoundException("zip split file does not exist: " + nextSplitFile);
    }
    randomAccessFile.close();
    randomAccessFile = new RandomAccessFile(nextSplitFile, RandomAccessFileMode.READ.getValue());
  }

  private File getNextSplitFileName(int zipFileIndex) throws IOException {
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

  @Override
  public void close() throws IOException {
    if (randomAccessFile != null) {
      randomAccessFile.close();
    }
  }
}
