package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.RandomAccessFileMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;

public class SplitInputStream extends InputStream {

  private PushbackInputStream inputStream;
  private RandomAccessFile randomAccessFile;
  private ZipModel zipModel;
  private boolean isSplitZipArchive = false;
  private int currentSplitFileCounter = 0;
  private long numberOfBytesRead = 0;
  private byte[] singleByteArray = new byte[1];

  public SplitInputStream(InputStream inputStream) {
    this.inputStream = new PushbackInputStream(inputStream, 512);
  }

  public SplitInputStream(RandomAccessFile randomAccessFile, ZipModel zipModel) {
    this.randomAccessFile = randomAccessFile;
    this.zipModel = zipModel;
    this.inputStream = null;
    this.isSplitZipArchive = zipModel.isSplitArchive();
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
    int readLen = readDataFromStream(b, off, len);
    numberOfBytesRead += readLen;
    return readLen;
  }

  public void prepareExtractionForFileHeader(FileHeader fileHeader) throws IOException {
    if (zipModel == null || randomAccessFile == null) {
      throw new IOException("This method can only be called in randomaccessfile mode");
    }

    if (!zipModel.isSplitArchive()) {
      randomAccessFile.seek(fileHeader.getOffsetLocalHeader());
      return;
    }

    openRandomAccessFileForIndex(fileHeader.getDiskNumberStart());
  }

  private int readDataFromStream(byte[] b, int off, int len) throws IOException {
    if (randomAccessFile != null) {
      return readFromRandomAccessFile(b, off, len);
    } else if (inputStream != null) {
      return inputStream.read(b, off, len);
    } else {
      throw new IOException("Invalid ZipEntryInputStream state. No inputstreams available to read data from");
    }
  }

  private int readFromRandomAccessFile(byte[] b, int off, int len) throws IOException {
    int readLen = randomAccessFile.read(b, off, len);

    if (readLen == -1 && isSplitZipArchive) {
      openRandomAccessFileForIndex(currentSplitFileCounter);
      currentSplitFileCounter++;

      if (readLen < 0) readLen = 0;
      int newlyRead = randomAccessFile.read(b, readLen, len - readLen);
      if (newlyRead > 0) readLen += newlyRead;
    }

    return readLen;
  }

  private void openRandomAccessFileForIndex(int zipFileIndex) throws IOException {
    File nextSplitFile = getNextSplitFileName(zipFileIndex);
    if (!nextSplitFile.exists()) {
      throw new FileNotFoundException("zip split file does not exist: " + nextSplitFile);
    }
    randomAccessFile.close();
    randomAccessFile = new RandomAccessFile(nextSplitFile, RandomAccessFileMode.READ.getCode());
  }

  private File getNextSplitFileName(int zipFileIndex) throws IOException {
    if (zipFileIndex == zipModel.getEndOfCentralDirRecord().getNoOfThisDisk()) {
      return zipModel.getZipFile();
    }

    String currZipFileNameWithPath = zipModel.getZipFile().getCanonicalPath();
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

  public long getNumberOfBytesRead() {
    return numberOfBytesRead;
  }
}
