package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static net.lingala.zip4j.headers.HeaderUtil.getFileHeader;
import static net.lingala.zip4j.model.enums.CompressionMethod.DEFLATE;
import static net.lingala.zip4j.model.enums.CompressionMethod.STORE;
import static net.lingala.zip4j.model.enums.EncryptionMethod.NONE;
import static net.lingala.zip4j.model.enums.EncryptionMethod.ZIP_STANDARD;
import static net.lingala.zip4j.progress.ProgressMonitor.Task.ADD_ENTRY;
import static net.lingala.zip4j.progress.ProgressMonitor.Task.CALCULATE_CRC;
import static net.lingala.zip4j.progress.ProgressMonitor.Task.REMOVE_ENTRY;
import static net.lingala.zip4j.util.CrcUtil.computeFileCrc;
import static net.lingala.zip4j.util.FileUtils.getRelativeFileName;
import static net.lingala.zip4j.util.InternalZipConstants.BUFF_SIZE;
import static net.lingala.zip4j.util.Zip4jUtil.javaToDosTime;

public abstract class AbstractAddFileToZipTask<T> extends AsyncZipTask<T> {

  private ZipModel zipModel;
  private char[] password;
  private HeaderWriter headerWriter;

  AbstractAddFileToZipTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel,
                           char[] password, HeaderWriter headerWriter) {
    super(progressMonitor, runInThread);
    this.zipModel = zipModel;
    this.password = password;
    this.headerWriter = headerWriter;
  }

  void addFilesToZip(List<File> filesToAdd, ProgressMonitor progressMonitor, ZipParameters zipParameters)
      throws ZipException {

    removeFilesIfExists(filesToAdd, zipParameters, progressMonitor);

    try (SplitOutputStream splitOutputStream = new SplitOutputStream(zipModel.getZipFile(), zipModel.getSplitLength());
         ZipOutputStream zipOutputStream = initializeOutputStream(splitOutputStream)) {
      byte[] readBuff = new byte[BUFF_SIZE];
      int readLen = -1;

      for (File fileToAdd : filesToAdd) {
        verifyIfTaskIsCancelled();
        ZipParameters clonedZipParameters = cloneAndAdjustZipParameters(zipParameters, fileToAdd, progressMonitor);
        progressMonitor.setFileName(fileToAdd.getAbsolutePath());

        zipOutputStream.putNextEntry(clonedZipParameters);
        if (fileToAdd.isDirectory()) {
          zipOutputStream.closeEntry();
          continue;
        }

        try (InputStream inputStream = new FileInputStream(fileToAdd)) {
          while ((readLen = inputStream.read(readBuff)) != -1) {
            zipOutputStream.write(readBuff, 0, readLen);
            progressMonitor.updateWorkCompleted(readLen);
            verifyIfTaskIsCancelled();
          }
        }

        FileHeader fileHeader = zipOutputStream.closeEntry();
        fileHeader.setExternalFileAttributes(FileUtils.getFileAttributes(fileToAdd));

        headerWriter.updateLocalFileHeader(fileHeader, zipModel, splitOutputStream);
      }
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  long calculateWorkForFiles(List<File> filesToAdd, ZipParameters zipParameters) throws ZipException {
    long totalWork = 0;

    for (File fileToAdd : filesToAdd) {
      if (!fileToAdd.exists()) {
        continue;
      }

      if (zipParameters.isEncryptFiles() && zipParameters.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
        totalWork += (fileToAdd.length() * 2); // for CRC calculation
      } else {
        totalWork += fileToAdd.length();
      }

      //If an entry already exists, we have to remove that entry first and then add content again.
      //In this case, add corresponding work
      String relativeFileName = getRelativeFileName(fileToAdd.getAbsolutePath(), zipParameters.getDefaultFolderPath());
      FileHeader fileHeader = getFileHeader(getZipModel(), relativeFileName);
      if (fileHeader != null) {
        totalWork += (getZipModel().getZipFile().length() - fileHeader.getCompressedSize());
      }
    }

    return totalWork;
  }

  ZipOutputStream initializeOutputStream(SplitOutputStream splitOutputStream) throws IOException, ZipException {
    if (zipModel.getZipFile().exists()) {
      if (zipModel.getEndOfCentralDirectoryRecord() == null) {
        throw new ZipException("invalid end of central directory record");
      }
      splitOutputStream.seek(zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory());
    }

    return new ZipOutputStream(splitOutputStream, password, zipModel);
  }

  void verifyZipParameters(ZipParameters parameters) throws ZipException {
    if (parameters == null) {
      throw new ZipException("cannot validate zip parameters");
    }

    if (parameters.getCompressionMethod() != STORE && parameters.getCompressionMethod() != DEFLATE) {
      throw new ZipException("unsupported compression type");
    }

    if (parameters.isEncryptFiles()) {
      if (parameters.getEncryptionMethod() == NONE) {
        throw new ZipException("Encryption method has to be set, when encrypt files flag is set");
      }

      if (password == null || password.length <= 0) {
        throw new ZipException("input password is empty or null");
      }
    } else {
      parameters.setEncryptionMethod(NONE);
    }
  }

  private ZipParameters cloneAndAdjustZipParameters(ZipParameters zipParameters, File fileToAdd,
                                                    ProgressMonitor progressMonitor) throws ZipException {
    ZipParameters clonedZipParameters = new ZipParameters(zipParameters);
    clonedZipParameters.setLastModifiedFileTime(javaToDosTime((fileToAdd.lastModified())));
    clonedZipParameters.setFileNameInZip(fileToAdd.getName());

    if (fileToAdd.isDirectory()) {
      clonedZipParameters.setEntrySize(0);
    } else {
      clonedZipParameters.setEntrySize(fileToAdd.length());
    }

    clonedZipParameters.setWriteExtendedLocalFileHeader(false);
    clonedZipParameters.setLastModifiedFileTime(fileToAdd.lastModified());

    String relativeFileName = getRelativeFileName(fileToAdd.getAbsolutePath(), zipParameters.getDefaultFolderPath());
    clonedZipParameters.setFileNameInZip(relativeFileName);

    if (fileToAdd.isDirectory()) {
      clonedZipParameters.setCompressionMethod(CompressionMethod.STORE);
      clonedZipParameters.setEncryptionMethod(EncryptionMethod.NONE);
      clonedZipParameters.setEncryptFiles(false);
    } else {
      if (clonedZipParameters.isEncryptFiles() && clonedZipParameters.getEncryptionMethod() == ZIP_STANDARD) {
        progressMonitor.setCurrentTask(CALCULATE_CRC);
        clonedZipParameters.setEntryCRC(computeFileCrc(fileToAdd, progressMonitor));
        progressMonitor.setCurrentTask(ADD_ENTRY);
      }

      if (fileToAdd.length() == 0) {
        clonedZipParameters.setCompressionMethod(CompressionMethod.STORE);
      }
    }

    return clonedZipParameters;
  }

  private void removeFilesIfExists(List<File> files, ZipParameters zipParameters, ProgressMonitor progressMonitor)
      throws ZipException {
    if (!zipModel.getZipFile().exists()) {
      return;
    }

    for (File file : files) {
      String fileName = getRelativeFileName(file.getAbsolutePath(), zipParameters.getDefaultFolderPath());

      FileHeader fileHeader = getFileHeader(zipModel, fileName);
      if (fileHeader != null) {
        progressMonitor.setCurrentTask(REMOVE_ENTRY);
        removeFile(fileHeader, progressMonitor);
        verifyIfTaskIsCancelled();
        progressMonitor.setCurrentTask(ADD_ENTRY);
      }
    }
  }

  private void removeFile(FileHeader fileHeader, ProgressMonitor progressMonitor) throws ZipException {
    RemoveEntryFromZipFileTask removeEntryFromZipFileTask = new RemoveEntryFromZipFileTask(progressMonitor, false,
        zipModel);
    removeEntryFromZipFileTask.execute(fileHeader);
  }

  @Override
  protected ProgressMonitor.Task getTask() {
    return ProgressMonitor.Task.ADD_ENTRY;
  }

  protected ZipModel getZipModel() {
    return zipModel;
  }
}
