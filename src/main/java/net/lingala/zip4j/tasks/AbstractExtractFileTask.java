package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.UnzipUtil;
import net.lingala.zip4j.util.Zip4jUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static net.lingala.zip4j.util.InternalZipConstants.BUFF_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.FILE_SEPARATOR;

public abstract class AbstractExtractFileTask<T> extends AsyncZipTask<T> {

  private ZipModel zipModel;
  private byte[] buff = new byte[BUFF_SIZE];

  public AbstractExtractFileTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel) {
    super(progressMonitor, runInThread);
    this.zipModel = zipModel;
  }

  protected void extractFile(ZipInputStream zipInputStream, FileHeader fileHeader, String outPath, String newFileName,
                             ProgressMonitor progressMonitor) throws ZipException {

    progressMonitor.setFileName(fileHeader.getFileName());

    if (!outPath.endsWith(FILE_SEPARATOR)) {
      outPath += FILE_SEPARATOR;
    }

    // make sure no file is extracted outside of the target directory (a.k.a zip slip)
    String fileName = fileHeader.getFileName();
    String completePath = outPath + fileName;
    if (!new File(completePath).getPath().startsWith(new File(outPath).getPath())) {
      throw new ZipException("illegal file name that breaks out of the target directory: "
          + fileHeader.getFileName());
    }

    verifyNextEntry(zipInputStream, fileHeader);

    if (fileHeader.isDirectory()) {
      File file = new File(completePath);
      if (!file.exists()) {
        if (!file.mkdirs()) {
          throw new ZipException("Could not create directory: " + file);
        }
      }
    } else {
      checkOutputDirectoryStructure(fileHeader, outPath, newFileName);
      unzipFile(zipInputStream, fileHeader, outPath, newFileName, progressMonitor);
    }
  }

  private void unzipFile(ZipInputStream inputStream, FileHeader fileHeader, String outputPath, String newFileName,
                         ProgressMonitor progressMonitor) throws ZipException {

    String outputFileName = Zip4jUtil.isStringNotNullAndNotEmpty(newFileName) ? newFileName : fileHeader.getFileName();
    File outputFile = new File(outputPath + System.getProperty("file.separator") + outputFileName);

    int readLength;
    try (OutputStream outputStream = new FileOutputStream(outputFile)) {
      while ((readLength = inputStream.read(buff)) != -1) {
        outputStream.write(buff, 0, readLength);
        progressMonitor.updateWorkCompleted(readLength);
        verifyIfTaskIsCancelled();
      }
    } catch (IOException e) {
      throw new ZipException(e);
    }

    UnzipUtil.applyFileAttributes(fileHeader, outputFile);
  }

  private void verifyNextEntry(ZipInputStream zipInputStream, FileHeader fileHeader) throws ZipException {
    try {
      LocalFileHeader localFileHeader = zipInputStream.getNextEntry();

      if (localFileHeader == null) {
        throw new ZipException("Could not read corresponding local file header for file header: "
            + fileHeader.getFileName());
      }

      if (!fileHeader.getFileName().equals(localFileHeader.getFileName())) {
        throw new ZipException("File header and local file header mismatch");
      }
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private void checkOutputDirectoryStructure(FileHeader fileHeader, String outPath, String newFileName)
      throws ZipException {

    String fileName = fileHeader.getFileName();
    if (Zip4jUtil.isStringNotNullAndNotEmpty(newFileName)) {
      fileName = newFileName;
    }

    if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
      // Do nothing
      return;
    }

    String compOutPath = outPath + fileName;
    File file = new File(compOutPath);
    if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
      throw new ZipException("Unable to create parent directories: " + file.getParentFile());
    }
  }

  @Override
  protected ProgressMonitor.Task getTask() {
    return ProgressMonitor.Task.EXTRACT_ENTRY;
  }

  public ZipModel getZipModel() {
    return zipModel;
  }
}
