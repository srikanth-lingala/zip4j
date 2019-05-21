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
import net.lingala.zip4j.headers.HeaderSignature;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.ArchiveMaintainer;
import net.lingala.zip4j.util.CRCUtil;
import net.lingala.zip4j.util.Zip4jUtil;
import net.lingala.zip4j.util.enums.RandomAccessFileMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.lingala.zip4j.util.InternalZipConstants.BUFF_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.OFFSET_CENTRAL_DIR;
import static net.lingala.zip4j.util.InternalZipConstants.THREAD_NAME;

public class ZipEngine {

  private ZipModel zipModel;
  private ProgressMonitor progressMonitor;
  private char[] password;

  public ZipEngine(ZipModel zipModel, ProgressMonitor progressMonitor, char[] password) {
    this.zipModel = zipModel;
    this.progressMonitor = progressMonitor;
    this.password = password;
  }

  public void addFiles(List<File> filesToAdd, ZipParameters parameters, boolean runInThread) throws ZipException {

    progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_ADD);
    progressMonitor.setState(ProgressMonitor.STATE_BUSY);
    progressMonitor.setResult(ProgressMonitor.RESULT_WORKING);

    if (runInThread) {
      progressMonitor.setTotalWork(calculateTotalWork(filesToAdd, parameters));
      progressMonitor.setFileName(((File) filesToAdd.get(0)).getAbsolutePath());

      Thread thread = new Thread(THREAD_NAME) {
        public void run() {
          try {
            initAddFiles(filesToAdd, parameters);
          } catch (ZipException e) {
          }
        }
      };
      thread.start();

    } else {
      initAddFiles(filesToAdd, parameters);
    }
  }

  private void initAddFiles(List<File> filesToAdd, ZipParameters parameters) throws ZipException {

    if (filesToAdd == null || parameters == null) {
      throw new ZipException("one of the input parameters is null when adding files");
    }

    if (filesToAdd.size() <= 0) {
      throw new ZipException("no files to add");
    }

    if (zipModel.getEndOfCentralDirectoryRecord() == null) {
      zipModel.setEndOfCentralDirectoryRecord(createEndOfCentralDirectoryRecord());
    }

    ZipOutputStream outputStream = null;
    InputStream inputStream = null;
    try {
      checkParameters(parameters);

      removeFilesIfExists(filesToAdd, parameters);

      boolean isZipFileAlreadyExists = Zip4jUtil.checkFileExists(zipModel.getZipFile());

      SplitOutputStream splitOutputStream = new SplitOutputStream(zipModel.getZipFile(), zipModel.getSplitLength());
      outputStream = new ZipOutputStream(splitOutputStream, password, this.zipModel);

      if (isZipFileAlreadyExists) {
        if (zipModel.getEndOfCentralDirectoryRecord() == null) {
          throw new ZipException("invalid end of central directory record");
        }
        splitOutputStream.seek(zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory());
      }
      byte[] readBuff = new byte[BUFF_SIZE];
      int readLen = -1;
      for (int i = 0; i < filesToAdd.size(); i++) {

        if (progressMonitor.isCancelAllTasks()) {
          progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
          progressMonitor.setState(ProgressMonitor.STATE_READY);
          return;
        }

        ZipParameters fileParameters = new ZipParameters(parameters);
        fileParameters.setLastModifiedFileTime((int) Zip4jUtil.javaToDosTime((Zip4jUtil.getLastModifiedFileTime(
            filesToAdd.get(i), parameters.getTimeZone()))));
        fileParameters.setFileNameInZip(Zip4jUtil.getFileNameFromFilePath(filesToAdd.get(i)));

        if (parameters.getCompressionMethod() == CompressionMethod.STORE) {
          fileParameters.setUncompressedSize(Zip4jUtil.getFileLengh(filesToAdd.get(i)));
        }

        progressMonitor.setFileName(filesToAdd.get(i).getAbsolutePath());

        if (!filesToAdd.get(i).isDirectory()) {
          if (fileParameters.isEncryptFiles() && fileParameters.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
            progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_CALC_CRC);
            fileParameters.setSourceFileCRC((int) CRCUtil.computeFileCRC(((File) filesToAdd.get(i)).getAbsolutePath(), progressMonitor));
            progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_ADD);

            if (progressMonitor.isCancelAllTasks()) {
              progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
              progressMonitor.setState(ProgressMonitor.STATE_READY);
              return;
            }
          }

          if (Zip4jUtil.getFileLengh(filesToAdd.get(i)) == 0) {
            fileParameters.setCompressionMethod(CompressionMethod.STORE);
          }
        }

        outputStream.putNextEntry(fileParameters);
        if (((File) filesToAdd.get(i)).isDirectory()) {
          outputStream.closeEntry();
          continue;
        }

        inputStream = new FileInputStream((File) filesToAdd.get(i));

        while ((readLen = inputStream.read(readBuff)) != -1) {
          if (progressMonitor.isCancelAllTasks()) {
            progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
            progressMonitor.setState(ProgressMonitor.STATE_READY);
            return;
          }

          outputStream.write(readBuff, 0, readLen);
          progressMonitor.updateWorkCompleted(readLen);
        }

        outputStream.closeEntry();

        if (inputStream != null) {
          inputStream.close();
        }
      }

      progressMonitor.endProgressMonitorSuccess();
    } catch (ZipException e) {
      progressMonitor.endProgressMonitorError(e);
      throw e;
    } catch (Exception e) {
      progressMonitor.endProgressMonitorError(e);
      throw new ZipException(e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
        }
      }

      if (outputStream != null) {
        try {
          outputStream.close();
        } catch (IOException e) {
        }
      }
    }
  }

  public void addStreamToZip(InputStream inputStream, ZipParameters parameters) throws ZipException {
    if (inputStream == null || parameters == null) {
      throw new ZipException("one of the input parameters is null, cannot add stream to zip");
    }

    ZipOutputStream outputStream = null;

    try {
      checkParameters(parameters);

      boolean isZipFileAlreadExists = Zip4jUtil.checkFileExists(zipModel.getZipFile());

      SplitOutputStream splitOutputStream = new SplitOutputStream(zipModel.getZipFile(), zipModel.getSplitLength());
      outputStream = new ZipOutputStream(splitOutputStream, password, this.zipModel);

      if (isZipFileAlreadExists) {
        if (zipModel.getEndOfCentralDirectoryRecord() == null) {
          throw new ZipException("invalid end of central directory record");
        }
        splitOutputStream.seek(zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory());
      }

      byte[] readBuff = new byte[BUFF_SIZE];
      int readLen = -1;

      outputStream.putNextEntry(parameters);

      if (!parameters.getFileNameInZip().endsWith("/") &&
          !parameters.getFileNameInZip().endsWith("\\")) {
        while ((readLen = inputStream.read(readBuff)) != -1) {
          outputStream.write(readBuff, 0, readLen);
        }
      }

      outputStream.closeEntry();
    } catch (ZipException e) {
      throw e;
    } catch (Exception e) {
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

  public void addFolderToZip(File file, ZipParameters parameters, boolean runInThread) throws ZipException {
    if (file == null || parameters == null) {
      throw new ZipException("one of the input parameters is null, cannot add folder to zip");
    }

    if (!Zip4jUtil.checkFileExists(file.getAbsolutePath())) {
      throw new ZipException("input folder does not exist");
    }

    if (!file.isDirectory()) {
      throw new ZipException("input file is not a folder, user addFileToZip method to add files");
    }

    if (!Zip4jUtil.assertFileReadAccess(file.getAbsolutePath())) {
      throw new ZipException("cannot read folder: " + file.getAbsolutePath());
    }

    String rootFolderPath = null;
    if (parameters.isIncludeRootFolder()) {
      if (file.getAbsolutePath() != null) {
        rootFolderPath = file.getAbsoluteFile().getParentFile() != null ? file.getAbsoluteFile().getParentFile().getAbsolutePath() : "";
      } else {
        rootFolderPath = file.getParentFile() != null ? file.getParentFile().getAbsolutePath() : "";
      }
    } else {
      rootFolderPath = file.getAbsolutePath();
    }

    parameters.setDefaultFolderPath(rootFolderPath);

    ArrayList fileList = Zip4jUtil.getFilesInDirectoryRec(file, parameters.isReadHiddenFiles());

    if (parameters.isIncludeRootFolder()) {
      if (fileList == null) {
        fileList = new ArrayList();
      }
      fileList.add(file);
    }

    addFiles(fileList, parameters, runInThread);
  }


  private void checkParameters(ZipParameters parameters) throws ZipException {

    if (parameters == null) {
      throw new ZipException("cannot validate zip parameters");
    }

    if ((parameters.getCompressionMethod() != CompressionMethod.STORE) &&
        parameters.getCompressionMethod() != CompressionMethod.DEFLATE) {
      throw new ZipException("unsupported compression type");
    }

    if (parameters.isEncryptFiles()) {
      if (parameters.getEncryptionMethod() == EncryptionMethod.NONE) {
        throw new ZipException("Encryption method has to be set, when encrypt files flag is set");
      }

      if (password == null || password.length <= 0) {
        throw new ZipException("input password is empty or null");
      }
    } else {
      parameters.setEncryptionMethod(EncryptionMethod.NONE);
    }

  }

  private void removeFilesIfExists(List<File> files, ZipParameters parameters) throws ZipException {

    if (zipModel == null || zipModel.getCentralDirectory() == null ||
        zipModel.getCentralDirectory().getFileHeaders() == null ||
        zipModel.getCentralDirectory().getFileHeaders().size() <= 0) {
      //For a new zip file, this condition satisfies, so do nothing
      return;
    }
    RandomAccessFile outputStream = null;

    try {
      for (int i = 0; i < files.size(); i++) {
        File file = (File) files.get(i);

        String fileName = Zip4jUtil.getRelativeFileName(file.getAbsolutePath(),
            parameters.getRootFolderInZip(), parameters.getDefaultFolderPath());

        FileHeader fileHeader = Zip4jUtil.getFileHeader(zipModel, fileName);
        if (fileHeader != null) {

          if (outputStream != null) {
            outputStream.close();
            outputStream = null;
          }

          ArchiveMaintainer archiveMaintainer = new ArchiveMaintainer();
          progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_REMOVE);
          HashMap retMap = archiveMaintainer.initRemoveZipFile(zipModel,
              fileHeader, progressMonitor);

          if (progressMonitor.isCancelAllTasks()) {
            progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
            progressMonitor.setState(ProgressMonitor.STATE_READY);
            return;
          }

          progressMonitor
              .setCurrentOperation(ProgressMonitor.OPERATION_ADD);

          if (outputStream == null) {
            outputStream = prepareFileOutputStream();

            if (retMap != null) {
              if (retMap.get(OFFSET_CENTRAL_DIR) != null) {
                long offsetCentralDir = -1;
                try {
                  offsetCentralDir = Long
                      .parseLong((String) retMap
                          .get(OFFSET_CENTRAL_DIR));
                } catch (NumberFormatException e) {
                  throw new ZipException(
                      "NumberFormatException while parsing offset central directory. " +
                          "Cannot update already existing file header");
                } catch (Exception e) {
                  throw new ZipException(
                      "Error while parsing offset central directory. " +
                          "Cannot update already existing file header");
                }

                if (offsetCentralDir >= 0) {
                  outputStream.seek(offsetCentralDir);
                }
              }
            }
          }
        }
      }
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

  private RandomAccessFile prepareFileOutputStream() throws ZipException {
    try {
      if (!zipModel.getZipFile().getParentFile().exists()) {
        zipModel.getZipFile().getParentFile().mkdirs();
      }
      return new RandomAccessFile(zipModel.getZipFile(), RandomAccessFileMode.WRITE.getValue());
    } catch (FileNotFoundException e) {
      throw new ZipException(e);
    }
  }

  private EndOfCentralDirectoryRecord createEndOfCentralDirectoryRecord() {
    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = new EndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setSignature(HeaderSignature.END_OF_CENTRAL_DIRECTORY);
    endOfCentralDirectoryRecord.setNumberOfThisDisk(0);
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectory(0);
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(0);
    endOfCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(0);
    return endOfCentralDirectoryRecord;
  }

  private long calculateTotalWork(List<File> fileList, ZipParameters parameters) throws ZipException {
    if (fileList == null) {
      throw new ZipException("file list is null, cannot calculate total work");
    }

    long totalWork = 0;

    for (int i = 0; i < fileList.size(); i++) {
      if (fileList.get(i) instanceof File) {
        if (((File) fileList.get(i)).exists()) {
          if (parameters.isEncryptFiles() &&
              parameters.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
            totalWork += (Zip4jUtil.getFileLengh((File) fileList.get(i)) * 2);
          } else {
            totalWork += Zip4jUtil.getFileLengh((File) fileList.get(i));
          }

          if (zipModel.getCentralDirectory() != null &&
              zipModel.getCentralDirectory().getFileHeaders() != null &&
              zipModel.getCentralDirectory().getFileHeaders().size() > 0) {
            String relativeFileName = Zip4jUtil.getRelativeFileName(
                ((File) fileList.get(i)).getAbsolutePath(), parameters.getRootFolderInZip(), parameters.getDefaultFolderPath());
            FileHeader fileHeader = Zip4jUtil.getFileHeader(zipModel, relativeFileName);
            if (fileHeader != null) {
              totalWork += (Zip4jUtil.getFileLengh(zipModel.getZipFile()) - fileHeader.getCompressedSize());
            }
          }
        }
      }
    }

    return totalWork;
  }
}
