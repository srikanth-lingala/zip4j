package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Random;

abstract class AbstractModifyFileTask<T> extends AsyncZipTask<T> {

  AbstractModifyFileTask(AsyncTaskParameters asyncTaskParameters) {
    super(asyncTaskParameters);
  }

  File getTemporaryFile(String zipPathWithName) {
    Random random = new Random();
    File tmpFile = new File(zipPathWithName + random.nextInt(10000));

    while (tmpFile.exists()) {
      tmpFile = new File(zipPathWithName + random.nextInt(10000));
    }

    return tmpFile;
  }

  long getOffsetLocalFileHeader(FileHeader fileHeader) {
    long offsetLocalFileHeader = fileHeader.getOffsetLocalHeader();

    if (fileHeader.getZip64ExtendedInfo() != null && fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() != -1) {
      offsetLocalFileHeader = fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader();
    }

    return offsetLocalFileHeader;
  }

  long getOffsetOfStartOfCentralDirectory(ZipModel zipModel) {
    long offsetStartCentralDir = zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory();

    if (zipModel.isZip64Format() && zipModel.getZip64EndOfCentralDirectoryRecord() != null) {
      offsetStartCentralDir = zipModel.getZip64EndOfCentralDirectoryRecord()
          .getOffsetStartCentralDirectoryWRTStartDiskNumber();
    }

    return offsetStartCentralDir;
  }

  void cleanupFile(boolean successFlag, File zipFile, File temporaryZipFile) throws ZipException {
    if (successFlag) {
      restoreFileName(zipFile, temporaryZipFile);
    } else {
      temporaryZipFile.delete();
    }
  }

  long copyFile(RandomAccessFile randomAccessFile, OutputStream outputStream, long start, long length,
                        ProgressMonitor progressMonitor) throws IOException {
    FileUtils.copyFile(randomAccessFile, outputStream, start, start + length, progressMonitor);
    return length;
  }

  private void restoreFileName(File zipFile, File temporaryZipFile) throws ZipException {
    if (zipFile.delete()) {
      if (!temporaryZipFile.renameTo(zipFile)) {
        throw new ZipException("cannot rename modified zip file");
      }
    } else {
      throw new ZipException("cannot delete old zip file");
    }
  }
}
