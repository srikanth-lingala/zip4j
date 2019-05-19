package net.lingala.zip4j.io.inputstreams;

import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.RandomAccessFileMode;
import net.lingala.zip4j.util.Zip4jUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

class ZipEntryInputStream extends InputStream {

  private InputStream inputStream;
  private RandomAccessFile randomAccessFile;
  private DecompressedInputStream decompressedInputStream;
  private ZipModel zipModel;
  private boolean isSplitZipArchive = false;
  private int currentSplitFileCounter = 0;
  private long numberOfBytesRead = 0;
  private byte[] singleByteArray = new byte[1];

  public ZipEntryInputStream(InputStream inputStream, ZipModel zipModel) {
    this.inputStream = inputStream;
    this.zipModel = zipModel;
    initializeRandomAccessFileIfSpliZipArchive(zipModel);
  }

  public ZipEntryInputStream(RandomAccessFile randomAccessFile, ZipModel zipModel) {
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

  private void initializeRandomAccessFileIfSpliZipArchive(ZipModel zipModel) {
    if (zipModel == null || !zipModel.isSplitArchive()) {
      randomAccessFile = null;
      return;
    }
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

    if (readLen != len && isSplitZipArchive) {
      startNextSplitFile();
      if (readLen < 0) readLen = 0;
      int newlyRead = randomAccessFile.read(b, readLen, len - readLen);
      if (newlyRead > 0) readLen += newlyRead;
    }

    return readLen;
  }

  private void startNextSplitFile() throws IOException {
    String nextSplitFileName = getNextSplitFileName();
    currentSplitFileCounter++;
    if (!Zip4jUtil.checkFileExists(nextSplitFileName)) {
      throw new FileNotFoundException("zip split file does not exist: " + nextSplitFileName);
    }
    randomAccessFile = new RandomAccessFile(nextSplitFileName, RandomAccessFileMode.READ.getCode());
  }

  private String getNextSplitFileName() {
    if (currentSplitFileCounter == zipModel.getEndOfCentralDirRecord().getNoOfThisDisk()) {
      return zipModel.getZipFile();
    }

    String currZipFile = zipModel.getZipFile();
    String extensionSubString = ".z0";
    if (currentSplitFileCounter >= 9) {
      extensionSubString = ".z";
    }

    return currZipFile.substring(0, currZipFile.lastIndexOf(".")) + extensionSubString + (currentSplitFileCounter + 1);
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
