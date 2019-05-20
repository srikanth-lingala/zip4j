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

package net.lingala.zip4j.zip;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.SplitInputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.RandomAccessFileMode;
import net.lingala.zip4j.util.Zip4jUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;

public class UnzipEngine {

  private ZipModel zipModel;
  private ProgressMonitor progressMonitor;
  private byte[] buff = new byte[InternalZipConstants.BUFF_SIZE];
  private char[] password;

  public UnzipEngine(ZipModel zipModel, ProgressMonitor progressMonitor, char[] password) {
    this.zipModel = zipModel;
    this.progressMonitor = progressMonitor;
    this.password = password;
  }

  public void extractAll(UnzipParameters unzipParameters, String outPath, boolean runInThread) throws ZipException {

    try (ZipInputStream inputStream = createZipInputStream(password)) {
      CentralDirectory centralDirectory = zipModel.getCentralDirectory();

      if (centralDirectory == null ||
          centralDirectory.getFileHeaders() == null) {
        throw new ZipException("invalid central directory in zipModel");
      }

      final List<FileHeader> fileHeaders = centralDirectory.getFileHeaders();

      progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_EXTRACT);
      progressMonitor.setTotalWork(calculateTotalWork(fileHeaders));
      progressMonitor.setState(ProgressMonitor.STATE_BUSY);

      if (runInThread) {
        Thread thread = new Thread(InternalZipConstants.THREAD_NAME) {
          public void run() {
            try {
              initExtractAll(inputStream, fileHeaders, outPath, unzipParameters);
              progressMonitor.endProgressMonitorSuccess();
            } catch (ZipException e) {
            }
          }
        };
        thread.start();
      } else {
        initExtractAll(inputStream, fileHeaders, outPath, unzipParameters);
      }
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  public void extractFile(FileHeader fileHeader, String outPath, String newFileName, boolean runInThread,
                          UnzipParameters unzipParameters) throws ZipException {
    if (fileHeader == null) {
      throw new ZipException("fileHeader is null");
    }

    progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_EXTRACT);
    progressMonitor.setTotalWork(fileHeader.getCompressedSize());
    progressMonitor.setState(ProgressMonitor.STATE_BUSY);
    progressMonitor.setPercentDone(0);
    progressMonitor.setFileName(fileHeader.getFileName());

    try(ZipInputStream inputStream = createZipInputStream(password)) {
      if (runInThread) {
        Thread thread = new Thread(InternalZipConstants.THREAD_NAME) {
          public void run() {
            try {
              initExtractFile(inputStream, fileHeader, outPath, newFileName, unzipParameters);
              progressMonitor.endProgressMonitorSuccess();
            } catch (ZipException e) {
            }
          }
        };
        thread.start();
      } else {
        initExtractFile(inputStream, fileHeader, outPath, newFileName, unzipParameters);
        progressMonitor.endProgressMonitorSuccess();
      }
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private void initExtractAll(ZipInputStream inputStream, List<FileHeader> fileHeaders, String outPath,
                              UnzipParameters unzipParameters) throws ZipException {

    for (FileHeader fileHeader : fileHeaders) {
      initExtractFile(inputStream, fileHeader, outPath, null, unzipParameters);

      if (progressMonitor.isCancelAllTasks()) {
        progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
        progressMonitor.setState(ProgressMonitor.STATE_READY);
        return;
      }
    }
  }

  private void initExtractFile(ZipInputStream inputStream, FileHeader fileHeader, String outPath, String newFileName,
                               UnzipParameters unzipParameters) throws ZipException {

    if (fileHeader == null) {
      throw new ZipException("fileHeader is null");
    }

    try {
      progressMonitor.setFileName(fileHeader.getFileName());

      if (!outPath.endsWith(InternalZipConstants.FILE_SEPARATOR)) {
        outPath += InternalZipConstants.FILE_SEPARATOR;
      }

      // make sure no file is extracted outside of the target directory (a.k.a zip slip)
      String fileName = fileHeader.getFileName();
      String completePath = outPath + fileName;
      if (!new File(completePath).getCanonicalPath().startsWith(new File(outPath).getCanonicalPath())) {
        throw new ZipException("illegal file name that breaks out of the target directory: " + fileHeader.getFileName());
      }

      if (fileHeader.isDirectory()) {
          File file = new File(completePath);
          if (!file.exists()) {
            file.mkdirs();
          }
      } else {
        checkOutputDirectoryStructure(fileHeader, outPath, newFileName);
        unzipFile(inputStream, fileHeader, outPath, newFileName, unzipParameters);
      }
    } catch (ZipException e) {
      progressMonitor.endProgressMonitorError(e);
      throw e;
    } catch (Exception e) {
      progressMonitor.endProgressMonitorError(e);
      throw new ZipException(e);
    }
  }

  private void unzipFile(ZipInputStream inputStream, FileHeader fileHeader, String outputPath, String newFileName,
                         UnzipParameters unzipParameters) throws ZipException {
    String outputFileName = Zip4jUtil.isStringNotNullAndNotEmpty(newFileName) ? newFileName : fileHeader.getFileName();
    File outputFile = new File(outputPath + System.getProperty("file.separator") + outputFileName);
    outputPath.hashCode();

    int readLength;
    try (OutputStream outputStream = new FileOutputStream(outputFile)) {
      inputStream.getNextEntry();
      while ((readLength = inputStream.read(buff)) != -1) {
        outputStream.write(buff, 0, readLength);
        progressMonitor.updateWorkCompleted(readLength);
        if (progressMonitor.isCancelAllTasks()) {
          progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
          progressMonitor.setState(ProgressMonitor.STATE_READY);
          return;
        }
      }
    } catch (IOException e) {
      throw new ZipException(e);
    }

    UnzipUtil.applyFileAttributes(fileHeader, outputFile, unzipParameters);
  }

  public ZipInputStream createZipInputStreamFor(FileHeader fileHeader) throws ZipException {
    try {
      return new ZipInputStream(createSplitInputStream(fileHeader), password);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private ZipInputStream createZipInputStream(char[] password) throws ZipException {
    try {
      return new ZipInputStream(createSplitInputStream(), password);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private SplitInputStream createSplitInputStream() throws IOException {
    return createSplitInputStream(null);
  }

  private SplitInputStream createSplitInputStream(FileHeader fileHeader) throws IOException {
    RandomAccessFile randomAccessFile = new RandomAccessFile(zipModel.getZipFile(), RandomAccessFileMode.READ.getCode());
    SplitInputStream splitInputStream = new SplitInputStream(randomAccessFile, zipModel);

    if (fileHeader != null) {
      splitInputStream.prepareExtractionForFileHeader(fileHeader);
    }

    return splitInputStream;
  }

  private void checkOutputDirectoryStructure(FileHeader fileHeader, String outPath, String newFileName) throws ZipException {
    if (fileHeader == null || !Zip4jUtil.isStringNotNullAndNotEmpty(outPath)) {
      throw new ZipException("Cannot check output directory structure...one of the parameters was null");
    }

    String fileName = fileHeader.getFileName();

    if (Zip4jUtil.isStringNotNullAndNotEmpty(newFileName)) {
      fileName = newFileName;
    }

    if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
      // Do nothing
      return;
    }

    String compOutPath = outPath + fileName;
    try {
      File file = new File(compOutPath);
      String parentDir = file.getParent();
      File parentDirFile = new File(parentDir);
      if (!parentDirFile.exists()) {
        parentDirFile.mkdirs();
      }
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  private long calculateTotalWork(List<FileHeader> fileHeaders) {
    long totalWork = 0;

    for (FileHeader fileHeader : fileHeaders) {
      if (fileHeader.getZip64ExtendedInfo() != null &&
          fileHeader.getZip64ExtendedInfo().getUnCompressedSize() > 0) {
        totalWork += fileHeader.getZip64ExtendedInfo().getCompressedSize();
      } else {
        totalWork += fileHeader.getCompressedSize();
      }
    }

    return totalWork;
  }

  private String getOutputFileNameWithPath(FileHeader fileHeader, String outputPath, String newFileName) {
    String fileName = fileHeader.getFileName();
    if (Zip4jUtil.isStringNotNullAndNotEmpty(newFileName)) {
      fileName = newFileName;
    }
    return outputPath + System.getProperty("file.separator") + fileName;
  }

}
