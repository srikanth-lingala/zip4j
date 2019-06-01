package net.lingala.zip4j.tasks;

import lombok.AllArgsConstructor;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.SplitInputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.ExtractAllFilesTask.ExtractAllFilesTaskParameters;

import java.io.IOException;

public class ExtractAllFilesTask extends AbstractExtractFileTask<ExtractAllFilesTaskParameters> {

  private char[] password;

  public ExtractAllFilesTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel, char[] password) {
    super(progressMonitor, runInThread, zipModel);
    this.password = password;
  }

  @Override
  protected void executeTask(ExtractAllFilesTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws ZipException {
    try (ZipInputStream zipInputStream = createZipInputStream()) {
      for (FileHeader fileHeader : getZipModel().getCentralDirectory().getFileHeaders()) {
        extractFile(zipInputStream, fileHeader, taskParameters.outputPath, null, taskParameters.unzipParameters,
            progressMonitor);
        verifyIfTaskIsCancelled();
      }
    } catch (IOException e) {
      throw new ZipException(e);
    }

  }

  @Override
  protected long calculateTotalWork(ExtractAllFilesTaskParameters taskParameters) {
    long totalWork = 0;

    for (FileHeader fileHeader : getZipModel().getCentralDirectory().getFileHeaders()) {
      if (fileHeader.getZip64ExtendedInfo() != null &&
          fileHeader.getZip64ExtendedInfo().getUncompressedSize() > 0) {
        totalWork += fileHeader.getZip64ExtendedInfo().getCompressedSize();
      } else {
        totalWork += fileHeader.getCompressedSize();
      }
    }

    return totalWork;
  }

  private ZipInputStream createZipInputStream() throws ZipException {
    try {
      SplitInputStream splitInputStream = new SplitInputStream(getZipModel().getZipFile(),
          getZipModel().isSplitArchive(), getZipModel().getEndOfCentralDirectoryRecord().getNumberOfThisDisk());
      return new ZipInputStream(splitInputStream, password);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  @AllArgsConstructor
  public static class ExtractAllFilesTaskParameters {
    private UnzipParameters unzipParameters;
    private String outputPath;

  }

}
