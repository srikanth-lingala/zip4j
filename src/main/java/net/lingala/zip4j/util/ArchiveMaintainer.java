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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.lingala.zip4j.util.InternalZipConstants.BUFF_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.DEFAULT_COMMENT_CHARSET;
import static net.lingala.zip4j.util.InternalZipConstants.MAX_ALLOWED_ZIP_COMMENT_LENGTH;
import static net.lingala.zip4j.util.InternalZipConstants.THREAD_NAME;
import static net.lingala.zip4j.util.Zip4jUtil.getIndexOfFileHeader;

public class ArchiveMaintainer {

  public void removeZipFile(ZipModel zipModel, FileHeader fileHeader, ProgressMonitor progressMonitor,
                               boolean runInThread) throws ZipException {

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
        offsetStartCentralDir = zipModel.getZip64EndOfCentralDirectoryRecord().getOffsetStartCenDirWRTStartDiskNo();
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
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  /**
   * Merges split Zip files into a single Zip file
   *
   * @param zipModel
   * @throws ZipException
   */
  public void mergeSplitZipFiles(final ZipModel zipModel, final File outputZipFile,
                                 final ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
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

  private void initMergeSplitZipFile(ZipModel zipModel, File outputZipFile,
                                     ProgressMonitor progressMonitor) throws ZipException {
    if (zipModel == null) {
      ZipException e = new ZipException("one of the input parameters is null, cannot merge split zip file");
      progressMonitor.endProgressMonitorError(e);
      throw e;
    }

    if (!zipModel.isSplitArchive()) {
      ZipException e = new ZipException("archive not a split zip file");
      progressMonitor.endProgressMonitorError(e);
      throw e;
    }

    OutputStream outputStream = null;
    RandomAccessFile inputStream = null;
    ArrayList fileSizeList = new ArrayList();
    long totBytesWritten = 0;
    boolean splitSigRemoved = false;
    try {

      int totNoOfSplitFiles = zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk();

      if (totNoOfSplitFiles <= 0) {
        throw new ZipException("corrupt zip model, archive not a split zip file");
      }

      outputStream = prepareOutputStreamForMerge(outputZipFile);
      for (int i = 0; i <= totNoOfSplitFiles; i++) {
        inputStream = createSplitZipFileHandler(zipModel, i);

        int start = 0;
        Long end = new Long(inputStream.length());

        if (i == 0) {
          if (zipModel.getCentralDirectory() != null &&
              zipModel.getCentralDirectory().getFileHeaders() != null &&
              zipModel.getCentralDirectory().getFileHeaders().size() > 0) {
            byte[] buff = new byte[4];
            inputStream.seek(0);
            inputStream.read(buff);
            if (Raw.readIntLittleEndian(buff, 0) == HeaderSignature.SPLIT_ZIP.getValue()) {
              start = 4;
              splitSigRemoved = true;
            }
          }
        }

        if (i == totNoOfSplitFiles) {
          end = new Long(zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory());
        }

        copyFile(inputStream, outputStream, start, end.longValue(), progressMonitor);
        totBytesWritten += (end.longValue() - start);
        if (progressMonitor.isCancelAllTasks()) {
          progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
          progressMonitor.setState(ProgressMonitor.STATE_READY);
          return;
        }

        fileSizeList.add(end);

        try {
          inputStream.close();
        } catch (IOException e) {
          //ignore
        }
      }

      ZipModel newZipModel = (ZipModel) zipModel.clone();
      newZipModel.getEndOfCentralDirectoryRecord().setOffsetOfStartOfCentralDirectory(totBytesWritten);

      updateSplitZipModel(newZipModel, fileSizeList, splitSigRemoved);

      HeaderWriter headerWriter = new HeaderWriter();
      headerWriter.finalizeZipFileWithoutValidations(newZipModel, outputStream);

      progressMonitor.endProgressMonitorSuccess();

    } catch (IOException e) {
      progressMonitor.endProgressMonitorError(e);
      throw new ZipException(e);
    } catch (Exception e) {
      progressMonitor.endProgressMonitorError(e);
      throw new ZipException(e);
    } finally {
      if (outputStream != null) {
        try {
          outputStream.close();
        } catch (IOException e) {
          //ignore
        }
      }

      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }

  /**
   * Creates an input stream for the split part of the zip file
   *
   * @return Zip4jInputStream
   * @throws ZipException
   */

  private RandomAccessFile createSplitZipFileHandler(ZipModel zipModel, int partNumber) throws ZipException {
    if (zipModel == null) {
      throw new ZipException("zip model is null, cannot create split file handler");
    }

    if (partNumber < 0) {
      throw new ZipException("invlaid part number, cannot create split file handler");
    }

    try {
      String curZipFile = zipModel.getZipFile().getPath();
      String partFile = null;
      if (partNumber == zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk()) {
        partFile = zipModel.getZipFile().getPath();
      } else {
        if (partNumber >= 9) {
          partFile = curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z" + (partNumber + 1);
        } else {
          partFile = curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z0" + (partNumber + 1);
        }
      }
      File tmpFile = new File(partFile);

      if (!Zip4jUtil.checkFileExists(tmpFile)) {
        throw new ZipException("split file does not exist: " + partFile);
      }

      return new RandomAccessFile(tmpFile, RandomAccessFileMode.READ.getValue());
    } catch (FileNotFoundException e) {
      throw new ZipException(e);
    } catch (Exception e) {
      throw new ZipException(e);
    }

  }

  private OutputStream prepareOutputStreamForMerge(File outFile) throws ZipException {
    if (outFile == null) {
      throw new ZipException("outFile is null, cannot create outputstream");
    }

    try {
      return new FileOutputStream(outFile);
    } catch (FileNotFoundException e) {
      throw new ZipException(e);
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  private void updateSplitZipModel(ZipModel zipModel, ArrayList fileSizeList, boolean splitSigRemoved) throws ZipException {
    if (zipModel == null) {
      throw new ZipException("zip model is null, cannot update split zip model");
    }

    zipModel.setSplitArchive(false);
    updateSplitFileHeader(zipModel, fileSizeList, splitSigRemoved);
    updateSplitEndCentralDirectory(zipModel);
    if (zipModel.isZip64Format()) {
      updateSplitZip64EndCentralDirLocator(zipModel, fileSizeList);
      updateSplitZip64EndCentralDirRec(zipModel, fileSizeList);
    }
  }

  private void updateSplitFileHeader(ZipModel zipModel, ArrayList fileSizeList, boolean splitSigRemoved) throws ZipException {
    try {

      if (zipModel.getCentralDirectory() == null) {
        throw new ZipException("corrupt zip model - getCentralDirectory, cannot update split zip model");
      }

      int fileHeaderCount = zipModel.getCentralDirectory().getFileHeaders().size();
      int splitSigOverhead = 0;
      if (splitSigRemoved)
        splitSigOverhead = 4;

      for (int i = 0; i < fileHeaderCount; i++) {
        long offsetLHToAdd = 0;

        for (int j = 0; j < ((FileHeader) zipModel.getCentralDirectory().getFileHeaders().get(i)).getDiskNumberStart(); j++) {
          offsetLHToAdd += ((Long) fileSizeList.get(j)).longValue();
        }
        ((FileHeader) zipModel.getCentralDirectory().getFileHeaders().get(i)).setOffsetLocalHeader(
            ((FileHeader) zipModel.getCentralDirectory().getFileHeaders().get(i)).getOffsetLocalHeader() +
                offsetLHToAdd - splitSigOverhead);
        ((FileHeader) zipModel.getCentralDirectory().getFileHeaders().get(i)).setDiskNumberStart(0);
      }

    } catch (ZipException e) {
      throw e;
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  private void updateSplitEndCentralDirectory(ZipModel zipModel) throws ZipException {
    try {
      if (zipModel == null) {
        throw new ZipException("zip model is null - cannot update end of central directory for split zip model");
      }

      if (zipModel.getCentralDirectory() == null) {
        throw new ZipException("corrupt zip model - getCentralDirectory, cannot update split zip model");
      }

      zipModel.getEndOfCentralDirectoryRecord().setNumberOfThisDisk(0);
      zipModel.getEndOfCentralDirectoryRecord().setNumberOfThisDiskStartOfCentralDir(0);
      zipModel.getEndOfCentralDirectoryRecord().setTotalNumberOfEntriesInCentralDirectory(
          zipModel.getCentralDirectory().getFileHeaders().size());
      zipModel.getEndOfCentralDirectoryRecord().setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
          zipModel.getCentralDirectory().getFileHeaders().size());

    } catch (ZipException e) {
      throw e;
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  private void updateSplitZip64EndCentralDirLocator(ZipModel zipModel, ArrayList fileSizeList) throws ZipException {
    if (zipModel == null) {
      throw new ZipException("zip model is null, cannot update split Zip64 end of central directory locator");
    }

    if (zipModel.getZip64EndOfCentralDirectoryLocator() == null) {
      return;
    }

    zipModel.getZip64EndOfCentralDirectoryLocator().setNumberOfDiskStartOfZip64EndOfCentralDirectoryRecord(0);
    long offsetZip64EndCentralDirRec = 0;

    for (int i = 0; i < fileSizeList.size(); i++) {
      offsetZip64EndCentralDirRec += ((Long) fileSizeList.get(i)).longValue();
    }
    zipModel.getZip64EndOfCentralDirectoryLocator().setOffsetZip64EndOfCentralDirRec(
        ((Zip64EndOfCentralDirectoryLocator) zipModel.getZip64EndOfCentralDirectoryLocator()).getOffsetZip64EndOfCentralDirRec() +
            offsetZip64EndCentralDirRec);
    zipModel.getZip64EndOfCentralDirectoryLocator().setTotalNumberOfDiscs(1);
  }

  private void updateSplitZip64EndCentralDirRec(ZipModel zipModel, ArrayList fileSizeList) throws ZipException {
    if (zipModel == null) {
      throw new ZipException("zip model is null, cannot update split Zip64 end of central directory record");
    }

    if (zipModel.getZip64EndOfCentralDirectoryRecord() == null) {
      return;
    }

    zipModel.getZip64EndOfCentralDirectoryRecord().setNoOfThisDisk(0);
    zipModel.getZip64EndOfCentralDirectoryRecord().setNoOfThisDiskStartOfCentralDir(0);
    zipModel.getZip64EndOfCentralDirectoryRecord().setTotNoOfEntriesInCentralDirOnThisDisk(
        zipModel.getEndOfCentralDirectoryRecord().getTotalNumberOfEntriesInCentralDirectory());

    long offsetStartCenDirWRTStartDiskNo = 0;

    for (int i = 0; i < fileSizeList.size(); i++) {
      offsetStartCenDirWRTStartDiskNo += ((Long) fileSizeList.get(i)).longValue();
    }

    zipModel.getZip64EndOfCentralDirectoryRecord().setOffsetStartCenDirWRTStartDiskNo(
        ((Zip64EndOfCentralDirectoryRecord) zipModel.getZip64EndOfCentralDirectoryRecord()).getOffsetStartCenDirWRTStartDiskNo() +
            offsetStartCenDirWRTStartDiskNo);
  }

  public void setComment(ZipModel zipModel, String comment) throws ZipException {
    if (comment == null) {
      throw new ZipException("comment is null, cannot update Zip file with comment");
    }

    if (zipModel == null) {
      throw new ZipException("zipModel is null, cannot update Zip file with comment");
    }

    String encodedComment = comment;
    byte[] commentBytes = comment.getBytes();
    int commentLength = comment.length();

    if (Zip4jUtil.isSupportedCharset(DEFAULT_COMMENT_CHARSET)) {
      try {
        encodedComment = new String(comment.getBytes(DEFAULT_COMMENT_CHARSET), DEFAULT_COMMENT_CHARSET);
        commentBytes = encodedComment.getBytes(DEFAULT_COMMENT_CHARSET);
        commentLength = encodedComment.length();
      } catch (UnsupportedEncodingException e) {
        encodedComment = comment;
        commentBytes = comment.getBytes();
        commentLength = comment.length();
      }
    }

    if (commentLength > MAX_ALLOWED_ZIP_COMMENT_LENGTH) {
      throw new ZipException("comment length exceeds maximum length");
    }

    zipModel.getEndOfCentralDirectoryRecord().setComment(encodedComment);
    zipModel.getEndOfCentralDirectoryRecord().setCommentBytes(commentBytes);
    zipModel.getEndOfCentralDirectoryRecord().setCommentLength(commentLength);

    SplitOutputStream outputStream = null;

    try {
      HeaderWriter headerWriter = new HeaderWriter();
      outputStream = new SplitOutputStream(zipModel.getZipFile());

      if (zipModel.isZip64Format()) {
        outputStream.seek(zipModel.getZip64EndOfCentralDirectoryRecord().getOffsetStartCenDirWRTStartDiskNo());
      } else {
        outputStream.seek(zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory());
      }

      headerWriter.finalizeZipFileWithoutValidations(zipModel, outputStream);
    } catch (FileNotFoundException e) {
      throw new ZipException(e);
    } catch (IOException e) {
      throw new ZipException(e);
    } finally {
      if (outputStream != null) {
        try {
          outputStream.close();
        } catch (IOException e) {
          //ignore
        }
      }
    }
  }

  public void initProgressMonitorForRemoveOp(ZipModel zipModel,
                                             FileHeader fileHeader, ProgressMonitor progressMonitor) throws ZipException {
    if (zipModel == null || fileHeader == null || progressMonitor == null) {
      throw new ZipException("one of the input parameters is null, cannot calculate total work");
    }

    progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_REMOVE);
    progressMonitor.setFileName(fileHeader.getFileName());
    progressMonitor.setTotalWork(calculateTotalWorkForRemoveOp(zipModel, fileHeader));
    progressMonitor.setState(ProgressMonitor.STATE_BUSY);
  }

  private long calculateTotalWorkForRemoveOp(ZipModel zipModel, FileHeader fileHeader) throws ZipException {
    return Zip4jUtil.getFileLengh(zipModel.getZipFile()) - fileHeader.getCompressedSize();
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
    long totSize = 0;
    if (zipModel.isSplitArchive()) {
      int totNoOfSplitFiles = zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk();
      String partFile = null;
      String curZipFile = zipModel.getZipFile().getPath();
      int partNumber = 0;
      for (int i = 0; i <= totNoOfSplitFiles; i++) {
        if (partNumber == zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk()) {
          partFile = zipModel.getZipFile().getPath();
        } else {
          if (partNumber >= 9) {
            partFile = curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z" + (partNumber + 1);
          } else {
            partFile = curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z0" + (partNumber + 1);
          }
        }

        totSize += Zip4jUtil.getFileLengh(new File(partFile));
      }

    }
    return totSize;
  }
}
