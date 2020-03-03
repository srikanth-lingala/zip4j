package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderUtil;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;
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

  void updateOffsetsForAllSubsequentFileHeaders(ZipModel zipModel, FileHeader fileHeaderModified, long offsetToAdd) throws ZipException {
    int indexOfFileHeader = HeaderUtil.getIndexOfFileHeader(zipModel, fileHeaderModified);

    if (indexOfFileHeader == -1) {
      throw new ZipException("Could not locate modified file header in zipModel");
    }

    List<FileHeader> allFileHeaders = zipModel.getCentralDirectory().getFileHeaders();

    for (int i = indexOfFileHeader + 1; i < allFileHeaders.size(); i++) {
      FileHeader fileHeaderToUpdate = allFileHeaders.get(i);
      fileHeaderToUpdate.setOffsetLocalHeader(fileHeaderToUpdate.getOffsetLocalHeader() + offsetToAdd);

      if (zipModel.isZip64Format()
          && fileHeaderToUpdate.getZip64ExtendedInfo() != null
          && fileHeaderToUpdate.getZip64ExtendedInfo().getOffsetLocalHeader() != -1) {

        fileHeaderToUpdate.getZip64ExtendedInfo().setOffsetLocalHeader(
            fileHeaderToUpdate.getZip64ExtendedInfo().getOffsetLocalHeader() + offsetToAdd
        );
      }
    }
  }

  void cleanupFile(boolean successFlag, File zipFile, File temporaryZipFile) throws ZipException {
    if (successFlag) {
      restoreFileName(zipFile, temporaryZipFile);
    } else {
      if (!temporaryZipFile.delete()) {
        throw new ZipException("Could not delete temporary file");
      }
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
