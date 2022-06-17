package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A split input stream for zip file split with 7-zip. They end with .zip.001, .zip.002, etc
 */
public class NumberedSplitFileInputStream extends SplitFileInputStream {

  private RandomAccessFile randomAccessFile;

  public NumberedSplitFileInputStream(File zipFile) throws IOException {
    this.randomAccessFile = new NumberedSplitRandomAccessFile(zipFile, RandomAccessFileMode.READ.getValue());
  }

  @Override
  public int read() throws IOException {
    return randomAccessFile.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return this.read(b,0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return randomAccessFile.read(b, off, len);
  }

  @Override
  public void prepareExtractionForFileHeader(FileHeader fileHeader) throws IOException {
    randomAccessFile.seek(fileHeader.getOffsetLocalHeader());
  }

  @Override
  public void close() throws IOException {
    if (randomAccessFile != null) {
      randomAccessFile.close();
    }
  }
}
