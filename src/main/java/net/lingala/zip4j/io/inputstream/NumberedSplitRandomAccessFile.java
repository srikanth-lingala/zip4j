package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A RandomAccessFile which reads files split with 7-zip format (.z001, .z002, etc) as a single file making it easier
 * for calling methods to deal with opening appropriate split file to read
 */
public class NumberedSplitRandomAccessFile extends RandomAccessFile {

  private long splitLength;
  private File[] allSortedSplitFiles;
  private RandomAccessFile randomAccessFile;
  private byte[] singleByteBuffer = new byte[1];
  private int currentOpenSplitFileCounter = 0;
  private String rwMode;

  public NumberedSplitRandomAccessFile(String name, String mode) throws IOException {
    this(new File(name), mode);
  }

  public NumberedSplitRandomAccessFile(File file, String mode) throws IOException {
    this(file, mode, FileUtils.getAllSortedNumberedSplitFiles(file));
  }

  public NumberedSplitRandomAccessFile(File file, String mode, File[] allSortedSplitFiles) throws IOException {
    super(file, mode);

    // A (dirty) hack to be able to open any split file without relying on the Parent class
    // For that the parent handle is closed, and a new RandomAccessFile is initiated
    // This makes it possible for this class to still extend RandomAccessFile and be able to open any split file
    // without calling code having to worry about opening a split file. Code using this class can read the
    // split files as if it were one file. zip4j uses RandomAccessFile in a lot of places. This hack allows to deal
    // with split files with RandomAccessFile without having to modify a lot of code, especially for a feature
    // that will not be used most often
    super.close();

    if (RandomAccessFileMode.WRITE.getValue().equals(mode)) {
      throw new IllegalArgumentException("write mode is not allowed for NumberedSplitRandomAccessFile");
    }

    assertAllSplitFilesExist(allSortedSplitFiles);

    this.randomAccessFile = new RandomAccessFile(file, mode);
    this.allSortedSplitFiles = allSortedSplitFiles;
    this.splitLength = file.length();
    this.rwMode = mode;
  }

  @Override
  public int read() throws IOException {
    int readLen = read(singleByteBuffer);

    if (readLen == -1) {
      return -1;
    }

    return singleByteBuffer[0] & 0xff;
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int readLen = randomAccessFile.read(b, off, len);

    if (readLen == -1) {
      if (currentOpenSplitFileCounter == allSortedSplitFiles.length - 1) {
        return -1;
      }
      openRandomAccessFileForIndex(currentOpenSplitFileCounter + 1);
      return read(b, off, len);
    }

    return readLen;
  }

  @Override
  public void write(int b) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void seek(long pos) throws IOException {
    int splitPartOfPosition = (int) (pos / splitLength);

    if (splitPartOfPosition != currentOpenSplitFileCounter) {
      openRandomAccessFileForIndex(splitPartOfPosition);
    }

    randomAccessFile.seek(pos - (splitPartOfPosition * splitLength));
  }

  @Override
  public long getFilePointer() throws IOException {
    return randomAccessFile.getFilePointer();
  }

  @Override
  public long length() throws IOException {
    return randomAccessFile.length();
  }

  public void seekInCurrentPart(long pos) throws IOException {
    randomAccessFile.seek(pos);
  }

  public void openLastSplitFileForReading() throws IOException {
    openRandomAccessFileForIndex(allSortedSplitFiles.length - 1);
  }

  private void openRandomAccessFileForIndex(int splitCounter) throws IOException {
    if (currentOpenSplitFileCounter == splitCounter) {
      return;
    }

    if (splitCounter > allSortedSplitFiles.length - 1) {
      throw new IOException("split counter greater than number of split files");
    }

    if (randomAccessFile != null) {
      randomAccessFile.close();
    }

    randomAccessFile = new RandomAccessFile(allSortedSplitFiles[splitCounter], rwMode);
    currentOpenSplitFileCounter = splitCounter;
  }

  private void assertAllSplitFilesExist(File[] allSortedSplitFiles) throws IOException {
    int splitCounter = 1;
    for (File splitFile : allSortedSplitFiles) {
      String fileExtension = FileUtils.getFileExtension(splitFile);
      try {
        if (splitCounter != Integer.parseInt(fileExtension)) {
          throw new IOException("Split file number " + splitCounter + " does not exist");
        }
        splitCounter++;
      } catch (NumberFormatException e) {
        throw new IOException("Split file extension not in expected format. Found: " + fileExtension
            + " expected of format: .001, .002, etc");
      }

    }
  }
}
