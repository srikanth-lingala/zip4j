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
import java.util.ArrayList;
import java.util.Collections;
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

  void updateOffsetsForAllSubsequentFileHeaders(List<FileHeader> sortedFileHeaders, ZipModel zipModel,
                                                FileHeader fileHeaderModified, long offsetToAdd) throws ZipException {
    int indexOfFileHeader = getIndexOfFileHeader(sortedFileHeaders, fileHeaderModified);

    if (indexOfFileHeader == -1) {
      throw new ZipException("Could not locate modified file header in zipModel");
    }

    for (int i = indexOfFileHeader + 1; i < sortedFileHeaders.size(); i++) {
      FileHeader fileHeaderToUpdate = sortedFileHeaders.get(i);
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

  List<FileHeader> cloneAndSortFileHeadersByOffset(List<FileHeader> allFileHeaders) {
    List<FileHeader> clonedFileHeaders = new ArrayList<>(allFileHeaders);
    //noinspection Java8ListSort
    Collections.sort(clonedFileHeaders, (o1, o2) -> {
      if (o1.getFileName().equals(o2.getFileName())) {
        return 0;
      }

      return o1.getOffsetLocalHeader() < o2.getOffsetLocalHeader() ? -1 : 1;
    });

    return clonedFileHeaders;
  }

  long getOffsetOfNextEntry(List<FileHeader> sortedFileHeaders, FileHeader fileHeader,
                                   ZipModel zipModel) throws ZipException {
    int indexOfFileHeader = getIndexOfFileHeader(sortedFileHeaders, fileHeader);

    if (indexOfFileHeader == sortedFileHeaders.size() - 1) {
      return HeaderUtil.getOffsetStartOfCentralDirectory(zipModel);
    } else {
      return sortedFileHeaders.get(indexOfFileHeader + 1).getOffsetLocalHeader();
    }
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

  private int getIndexOfFileHeader(List<FileHeader> allFileHeaders, FileHeader fileHeaderForIndex) throws ZipException {
    for (int i = 0; i < allFileHeaders.size(); i++) {
      FileHeader fileHeader = allFileHeaders.get(i);
      if (fileHeader.equals(fileHeaderForIndex)) {
        return i;
      }
    }

    throw new ZipException("Could not find file header in list of central directory file headers");
  }
}
