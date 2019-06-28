package net.lingala.zip4j.tasks;

import lombok.AllArgsConstructor;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.SplitInputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.ExtractAllFilesTask.ExtractAllFilesTaskParameters;

import java.io.IOException;

public class ExtractAllFilesTask extends AbstractExtractFileTask<ExtractAllFilesTaskParameters> {

  private char[] password;
  private SplitInputStream splitInputStream;

  public ExtractAllFilesTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel, char[] password) {
    super(progressMonitor, runInThread, zipModel);
    this.password = password;
  }

  @Override
  protected void executeTask(ExtractAllFilesTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws ZipException {
    try (ZipInputStream zipInputStream = prepareZipInputStream()) {
      for (FileHeader fileHeader : getZipModel().getCentralDirectory().getFileHeaders()) {
        if (fileHeader.getFileName().startsWith("__MACOSX")) {
          progressMonitor.updateWorkCompleted(fileHeader.getUncompressedSize());
          zipInputStream.getNextEntry();
          continue;
        }

        //splitInputStream.prepareExtractionForFileHeader(fileHeader);
        extractFile(zipInputStream, fileHeader, taskParameters.outputPath, null, progressMonitor);
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

  private ZipInputStream prepareZipInputStream() throws ZipException {
    try {
      splitInputStream = new SplitInputStream(getZipModel().getZipFile(),
          getZipModel().isSplitArchive(), getZipModel().getEndOfCentralDirectoryRecord().getNumberOfThisDisk());

      FileHeader fileHeader = getFirstFileHeader(getZipModel());
      if (fileHeader != null) {
        splitInputStream.prepareExtractionForFileHeader(fileHeader);
      }

      return new ZipInputStream(splitInputStream, password);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private FileHeader getFirstFileHeader(ZipModel zipModel) {
    if (zipModel.getCentralDirectory() == null
        || zipModel.getCentralDirectory().getFileHeaders() == null
        || zipModel.getCentralDirectory().getFileHeaders().size() == 0) {
      return null;
    }

    return zipModel.getCentralDirectory().getFileHeaders().get(0);
  }

  @AllArgsConstructor
  public static class ExtractAllFilesTaskParameters {
    private String outputPath;
  }

}
