/*
 * Copyright 2010 Srikanth Reddy Lingala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lingala.zip4j.util;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderSignature;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.model.EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.Zip64EndOfCentralDirectoryLocator;
import net.lingala.zip4j.model.Zip64EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.enums.RandomAccessFileMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Random;

import static net.lingala.zip4j.util.InternalZipConstants.BUFF_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.DEFAULT_COMMENT_CHARSET;
import static net.lingala.zip4j.util.InternalZipConstants.MAX_ALLOWED_ZIP_COMMENT_LENGTH;
import static net.lingala.zip4j.util.InternalZipConstants.THREAD_NAME;
import static net.lingala.zip4j.util.Zip4jUtil.getFileLengh;
import static net.lingala.zip4j.util.Zip4jUtil.getIndexOfFileHeader;

public class ArchiveMaintainer {

  public void removeZipFile(ZipModel zipModel, FileHeader fileHeader, ProgressMonitor progressMonitor,
                               boolean runInThread) throws ZipException {

    initProgressMonitorForRemoveOp(zipModel, fileHeader, progressMonitor);

    if (runInThread) {
      Thread thread = new Thread(THREAD_NAME) {
        public void run() {
          try {
            initRemoveZipFile(zipModel, fileHeader, progressMonitor);
            progressMonitor.endProgressMonitorSuccess();
          } catch (ZipException e) {
          }
        }
      };
      thread.start();
    } else {
      initRemoveZipFile(zipModel, fileHeader, progressMonitor);
      progressMonitor.endProgressMonitorSuccess();
    }

  }

  public void initRemoveZipFile(ZipModel zipModel,FileHeader fileHeader, ProgressMonitor progressMonitor)
      throws ZipException {

    if (zipModel.isSplitArchive()) {
      throw new ZipException("This is a split archive. Zip file format does not allow updating split/spanned files");
    }

    File temporaryZipFile = getTemporaryFile(zipModel.getZipFile().getPath());
    boolean successFlag = false;

    try (SplitOutputStream outputStream = new SplitOutputStream(temporaryZipFile);
        RandomAccessFile inputStream = new RandomAccessFile(zipModel.getZipFile(),
            RandomAccessFileMode.READ.getValue())){

      int indexOfFileHeader = getIndexOfFileHeader(zipModel, fileHeader);
      long offsetLocalFileHeader = getOffsetLocalFileHeader(fileHeader);
      long offsetStartOfCentralDirectory = getOffsetOfStartOfCentralDirectory(zipModel);
      List<FileHeader> fileHeaders = zipModel.getCentralDirectory().getFileHeaders();
      long offsetEndOfCompressedData = getOffsetEndOfCompressedData(zipModel, indexOfFileHeader,
          offsetStartOfCentralDirectory, fileHeaders);

      if (indexOfFileHeader == 0) {
        if (zipModel.getCentralDirectory().getFileHeaders().size() > 1) {
          // if this is the only file and it is deleted then no need to do this
          copyFile(inputStream, outputStream, offsetEndOfCompressedData + 1, offsetStartOfCentralDirectory, progressMonitor);
        }
      } else if (indexOfFileHeader == fileHeaders.size() - 1) {
        copyFile(inputStream, outputStream, 0, offsetLocalFileHeader, progressMonitor);
      } else {
        copyFile(inputStream, outputStream, 0, offsetLocalFileHeader, progressMonitor);
        copyFile(inputStream, outputStream, offsetEndOfCompressedData + 1, offsetStartOfCentralDirectory, progressMonitor);
      }

      if (progressMonitor.isCancelAllTasks()) {
        progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
        progressMonitor.setState(ProgressMonitor.STATE_READY);
        return;
      }

      updateHeaders(zipModel, outputStream, indexOfFileHeader, offsetEndOfCompressedData, offsetLocalFileHeader);
      successFlag = true;
    } catch (IOException e) {
      throw new ZipException(e);
    } finally {
      cleanupFile(successFlag, zipModel.getZipFile(), temporaryZipFile);
    }
  }

  private File getTemporaryFile(String zipPathWithName) {
    Random random = new Random();
    File tmpFile = new File(zipPathWithName + random.nextInt(10000));

    while (tmpFile.exists()) {
      tmpFile = new File(zipPathWithName + random.nextInt(10000));
    }

    return tmpFile;
  }

  private long getOffsetLocalFileHeader(FileHeader fileHeader) {
    long offsetLocalFileHeader = fileHeader.getOffsetLocalHeader();

    if (fileHeader.getZip64ExtendedInfo() != null && fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() != -1) {
      offsetLocalFileHeader = fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader();
    }

    return offsetLocalFileHeader;
  }

  private long getOffsetEndOfCompressedData(ZipModel zipModel, int indexOfFileHeader,
                                            long offsetStartOfCentralDirectory, List<FileHeader> fileHeaders) {
    if (indexOfFileHeader == fileHeaders.size() - 1) {
      return offsetStartOfCentralDirectory - 1;
    }

    FileHeader nextFileHeader = fileHeaders.get(indexOfFileHeader + 1);
    long offsetEndOfCompressedFile = nextFileHeader.getOffsetLocalHeader() - 1;
    if (nextFileHeader.getZip64ExtendedInfo() != null
        && nextFileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() != -1) {
      offsetEndOfCompressedFile = nextFileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() - 1;
    }

    return offsetEndOfCompressedFile;
  }

  private long getOffsetOfStartOfCentralDirectory(ZipModel zipModel) {
    long offsetStartCentralDir = zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory();

    if (zipModel.isZip64Format() && zipModel.getZip64EndOfCentralDirectoryRecord() != null) {
        offsetStartCentralDir = zipModel.getZip64EndOfCentralDirectoryRecord().getOffsetStartCentralDirectoryWRTStartDiskNumber();
    }

    return offsetStartCentralDir;
  }

  private void updateHeaders(ZipModel zipModel, SplitOutputStream splitOutputStream, int indexOfFileHeader, long
                             offsetEndOfCompressedFile, long offsetLocalFileHeader) throws IOException, ZipException {

    updateEndOfCentralDirectoryRecord(zipModel, splitOutputStream);
    zipModel.getCentralDirectory().getFileHeaders().remove(indexOfFileHeader);
    updateFileHeadersWithLocalHeaderOffsets(zipModel, offsetEndOfCompressedFile, offsetLocalFileHeader);

    HeaderWriter headerWriter = new HeaderWriter();
    headerWriter.finalizeZipFile(zipModel, splitOutputStream);
  }

  private void updateEndOfCentralDirectoryRecord(ZipModel zipModel, SplitOutputStream splitOutputStream)
      throws IOException {
    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = zipModel.getEndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(splitOutputStream.getFilePointer());
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectory(
        endOfCentralDirectoryRecord.getTotalNumberOfEntriesInCentralDirectory() - 1);
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
        endOfCentralDirectoryRecord.getTotalNumberOfEntriesInCentralDirectoryOnThisDisk() - 1);
    zipModel.setEndOfCentralDirectoryRecord(endOfCentralDirectoryRecord);
  }

  private void updateFileHeadersWithLocalHeaderOffsets(ZipModel zipModel, long offsetEndOfCompressedFile,
                                                       long offsetLocalFileHeader) {
    for (FileHeader fileHeader : zipModel.getCentralDirectory().getFileHeaders()) {
      long offsetLocalHdr = fileHeader.getOffsetLocalHeader();
      if (fileHeader.getZip64ExtendedInfo() != null && fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() != -1) {
        offsetLocalHdr = fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader();
      }
      fileHeader.setOffsetLocalHeader(offsetLocalHdr - (offsetEndOfCompressedFile - offsetLocalFileHeader) - 1);
    }
  }

  private void cleanupFile(boolean successFlag, File zipFile, File temporaryZipFile) throws ZipException {
    if (successFlag) {
      restoreFileName(zipFile, temporaryZipFile);
    } else {
      temporaryZipFile.delete();
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

  private void copyFile(RandomAccessFile randomAccessFile, OutputStream outputStream, long start, long end,
                        ProgressMonitor progressMonitor) throws ZipException {

    if (start < 0 || end < 0 || start > end) {
      throw new ZipException("invalid offsets");
    }

    if (start == end) {
      return;
    }

    try {
      randomAccessFile.seek(start);

      int readLen = -2;
      byte[] buff;
      long bytesRead = 0;
      long bytesToRead = end - start;

      if ((end - start) < BUFF_SIZE) {
        buff = new byte[(int) bytesToRead];
      } else {
        buff = new byte[BUFF_SIZE];
      }

      while ((readLen = randomAccessFile.read(buff)) != -1) {
        outputStream.write(buff, 0, readLen);

        progressMonitor.updateWorkCompleted(readLen);
        if (progressMonitor.isCancelAllTasks()) {
          progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
          return;
        }

        bytesRead += readLen;

        if (bytesRead == bytesToRead) {
          break;
        } else if (bytesRead + buff.length > bytesToRead) {
          buff = new byte[(int) (bytesToRead - bytesRead)];
        }
      }

    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  public void mergeSplitZipFiles(ZipModel zipModel, File outputZipFile, ProgressMonitor progressMonitor,
                                 boolean runInThread) throws ZipException {
    if (runInThread) {
      Thread thread = new Thread(THREAD_NAME) {
        public void run() {
          try {
            initMergeSplitZipFile(zipModel, outputZipFile, progressMonitor);
          } catch (ZipException e) {
          }
        }
      };
      thread.start();
    } else {
      initMergeSplitZipFile(zipModel, outputZipFile, progressMonitor);
    }
  }

  private void initMergeSplitZipFile(ZipModel zipModel, File outputZipFile, ProgressMonitor progressMonitor)
      throws ZipException {

    if (!zipModel.isSplitArchive()) {
      ZipException e = new ZipException("archive not a split zip file");
      progressMonitor.endProgressMonitorError(e);
      throw e;
    }

    try (OutputStream outputStream = new FileOutputStream(outputZipFile)) {
      long totalBytesWritten = 0;
      int totalNumberOfSplitFiles = zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk();
      if (totalNumberOfSplitFiles <= 0) {
        throw new ZipException("corrupt zip model, archive not a split zip file");
      }

      for (int i = 0; i <= totalNumberOfSplitFiles; i++) {
        try (RandomAccessFile randomAccessFile = createSplitZipFileStream(zipModel, i)) {
          int start = 0;
          Long end = randomAccessFile.length();

          if (i == 0) {
            byte[] buff = new byte[4];
            randomAccessFile.read(buff);
            if (Raw.readIntLittleEndian(buff, 0) == HeaderSignature.SPLIT_ZIP.getValue()) {
              start = 4;
            } else {
              randomAccessFile.seek(0);
            }
          }

          if (i == totalNumberOfSplitFiles) {
            end = new Long(zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory());
          }

          copyFile(randomAccessFile, outputStream, start, end, progressMonitor);
          totalBytesWritten += (end - start);
          if (progressMonitor.isCancelAllTasks()) {
            progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
            progressMonitor.setState(ProgressMonitor.STATE_READY);
            return;
          }
        }
      }
      updateHeadersForMergeSplitFileAction(zipModel, totalBytesWritten, outputStream);
      progressMonitor.endProgressMonitorSuccess();
    } catch (ZipException e) {
      progressMonitor.endProgressMonitorError(e);
      throw e;
    } catch (Exception e) {
      progressMonitor.endProgressMonitorError(e);
      throw new ZipException(e);
    }
  }

  private RandomAccessFile createSplitZipFileStream(ZipModel zipModel, int partNumber) throws FileNotFoundException {
    File splitFile = getNextSplitZipFile(zipModel, partNumber);
    return new RandomAccessFile(splitFile, RandomAccessFileMode.READ.getValue());
  }

  private void updateHeadersForMergeSplitFileAction(ZipModel zipModel, long totalBytesWritten,
                                                    OutputStream outputStream)
      throws ZipException, CloneNotSupportedException {

    ZipModel newZipModel = (ZipModel) zipModel.clone();
    newZipModel.getEndOfCentralDirectoryRecord().setOffsetOfStartOfCentralDirectory(totalBytesWritten);

    updateSplitZipModel(newZipModel, totalBytesWritten);

    HeaderWriter headerWriter = new HeaderWriter();
    headerWriter.finalizeZipFileWithoutValidations(newZipModel, outputStream);
  }

  private void updateSplitZipModel(ZipModel zipModel, long totalFileSize) {
    zipModel.setSplitArchive(false);
    updateSplitFileHeader(zipModel, totalFileSize);
    updateSplitEndCentralDirectory(zipModel);

    if (zipModel.isZip64Format()) {
      updateSplitZip64EndCentralDirLocator(zipModel, totalFileSize);
      updateSplitZip64EndCentralDirRec(zipModel, totalFileSize);
    }
  }

  private void updateSplitFileHeader(ZipModel zipModel, long totalFileSize) {
    for (FileHeader fileHeader : zipModel.getCentralDirectory().getFileHeaders()) {
      fileHeader.setOffsetLocalHeader(fileHeader.getOffsetLocalHeader() + totalFileSize);
      fileHeader.setDiskNumberStart(0);
    }
  }

  private void updateSplitEndCentralDirectory(ZipModel zipModel) {
    int numberOfFileHeaders = zipModel.getCentralDirectory().getFileHeaders().size();
    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = zipModel.getEndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setNumberOfThisDisk(0);
    endOfCentralDirectoryRecord.setNumberOfThisDiskStartOfCentralDir(0);
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectory(numberOfFileHeaders);
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(numberOfFileHeaders);
  }

  private void updateSplitZip64EndCentralDirLocator(ZipModel zipModel, long totalFileSize) {
    if (zipModel.getZip64EndOfCentralDirectoryLocator() == null) {
      return;
    }

    Zip64EndOfCentralDirectoryLocator zip64EndOfCentralDirectoryLocator = zipModel
        .getZip64EndOfCentralDirectoryLocator();
    zip64EndOfCentralDirectoryLocator.setNumberOfDiskStartOfZip64EndOfCentralDirectoryRecord(0);
    zip64EndOfCentralDirectoryLocator.setOffsetZip64EndOfCentralDirectoryRecord(
        zip64EndOfCentralDirectoryLocator.getOffsetZip64EndOfCentralDirectoryRecord() + totalFileSize);
    zip64EndOfCentralDirectoryLocator.setTotalNumberOfDiscs(1);
  }

  private void updateSplitZip64EndCentralDirRec(ZipModel zipModel, long totalFileSize) {
    if (zipModel.getZip64EndOfCentralDirectoryRecord() == null) {
      return;
    }

    Zip64EndOfCentralDirectoryRecord zip64EndOfCentralDirectoryRecord = zipModel.getZip64EndOfCentralDirectoryRecord();
    zip64EndOfCentralDirectoryRecord.setNumberOfThisDisk(0);
    zip64EndOfCentralDirectoryRecord.setNumberOfThisDiskStartOfCentralDirectory(0);
    zip64EndOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
        zipModel.getEndOfCentralDirectoryRecord().getTotalNumberOfEntriesInCentralDirectory());
    zip64EndOfCentralDirectoryRecord.setOffsetStartCentralDirectoryWRTStartDiskNumber(
        zip64EndOfCentralDirectoryRecord.getOffsetStartCentralDirectoryWRTStartDiskNumber() + totalFileSize);
  }

  public void setComment(ZipModel zipModel, String comment) throws ZipException {
    if (comment == null) {
      throw new ZipException("comment is null, cannot update Zip file with comment");
    }

    String encodedComment = convertCommentToDefaultCharset(comment);

    if (encodedComment.length() > MAX_ALLOWED_ZIP_COMMENT_LENGTH) {
      throw new ZipException("comment length exceeds maximum length");
    }

    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = zipModel.getEndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setComment(encodedComment);
    endOfCentralDirectoryRecord.setCommentBytes(encodedComment.getBytes());
    endOfCentralDirectoryRecord.setCommentLength(encodedComment.length());

    try (SplitOutputStream outputStream = new SplitOutputStream(zipModel.getZipFile())) {
      if (zipModel.isZip64Format()) {
        outputStream.seek(zipModel.getZip64EndOfCentralDirectoryRecord()
            .getOffsetStartCentralDirectoryWRTStartDiskNumber());
      } else {
        outputStream.seek(endOfCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory());
      }

      HeaderWriter headerWriter = new HeaderWriter();
      headerWriter.finalizeZipFileWithoutValidations(zipModel, outputStream);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private String convertCommentToDefaultCharset(String comment) throws ZipException {
    if (Zip4jUtil.isSupportedCharset(DEFAULT_COMMENT_CHARSET)) {
      try {
        return new String(comment.getBytes(DEFAULT_COMMENT_CHARSET), DEFAULT_COMMENT_CHARSET);
      } catch (UnsupportedEncodingException e) {
        return comment;
      }
    }
    return comment;
  }

  private void initProgressMonitorForRemoveOp(ZipModel zipModel, FileHeader fileHeader, ProgressMonitor progressMonitor)
      throws ZipException {
    progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_REMOVE);
    progressMonitor.setFileName(fileHeader.getFileName());
    progressMonitor.setTotalWork(calculateTotalWorkForRemoveOp(zipModel, fileHeader));
    progressMonitor.setState(ProgressMonitor.STATE_BUSY);
  }

  private long calculateTotalWorkForRemoveOp(ZipModel zipModel, FileHeader fileHeader) throws ZipException {
    return getFileLengh(zipModel.getZipFile()) - fileHeader.getCompressedSize();
  }

  public void initProgressMonitorForMergeOp(ZipModel zipModel, ProgressMonitor progressMonitor) throws ZipException {
    if (zipModel == null) {
      throw new ZipException("zip model is null, cannot calculate total work for merge op");
    }

    progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_MERGE);
    progressMonitor.setFileName(zipModel.getZipFile().getPath());
    progressMonitor.setTotalWork(calculateTotalWorkForMergeOp(zipModel));
    progressMonitor.setState(ProgressMonitor.STATE_BUSY);
  }

  private long calculateTotalWorkForMergeOp(ZipModel zipModel) throws ZipException {
    if (!zipModel.isSplitArchive()) {
      return 0;
    }

    long totalSize = 0;
    for (int i = 0; i <= zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk(); i++) {
      totalSize += getFileLengh(getNextSplitZipFile(zipModel, i));
    }
    return totalSize;
  }

  private File getNextSplitZipFile(ZipModel zipModel, int partNumber) {
    if (partNumber == zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk() - 1) {
      return zipModel.getZipFile();
    }

    String splitZipExtension = ".z0";
    if (partNumber >= 9) {
      splitZipExtension = ".z";
    }
    String rootZipFile = zipModel.getZipFile().getPath();
    String nextSplitZipFileName =  zipModel.getZipFile().getPath().substring(0, rootZipFile.lastIndexOf("."))
        + splitZipExtension + (partNumber + 1);
    return new File(nextSplitZipFileName);
  }
}
