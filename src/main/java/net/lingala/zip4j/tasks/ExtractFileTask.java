package net.lingala.zip4j.tasks;

import lombok.AllArgsConstructor;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.SplitInputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.ExtractFileTask.ExtractFileTaskParameters;

import java.io.IOException;

public class ExtractFileTask extends AbstractExtractFileTask<ExtractFileTaskParameters> {

  private char[] password;

  public ExtractFileTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel, char[] password) {
    super(progressMonitor, runInThread, zipModel);
    this.password = password;
  }

  @Override
  protected void executeTask(ExtractFileTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws ZipException {
    try(ZipInputStream zipInputStream = createZipInputStream(taskParameters.fileHeader)) {
      extractFile(zipInputStream, taskParameters.fileHeader, taskParameters.outputPath, taskParameters.newFileName,
          progressMonitor);
    } catch (IOException e) {
      throw new ZipException(e);
    }

  }

  @Override
  protected long calculateTotalWork(ExtractFileTaskParameters taskParameters) {
    return taskParameters.fileHeader.getCompressedSize();
  }

  protected ZipInputStream createZipInputStream(FileHeader fileHeader) throws ZipException {
    try {
      SplitInputStream splitInputStream = new SplitInputStream(getZipModel().getZipFile(),
          getZipModel().isSplitArchive(), getZipModel().getEndOfCentralDirectoryRecord().getNumberOfThisDisk());
      splitInputStream.prepareExtractionForFileHeader(fileHeader);
      return new ZipInputStream(splitInputStream, password);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  @AllArgsConstructor
  public static class ExtractFileTaskParameters {
    private String outputPath;
    private FileHeader fileHeader;
    private String newFileName;
  }
}
