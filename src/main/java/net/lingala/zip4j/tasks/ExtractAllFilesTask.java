package net.lingala.zip4j.tasks;

import net.lingala.zip4j.io.inputstream.SplitFileInputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.Zip4jConfig;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.ExtractAllFilesTask.ExtractAllFilesTaskParameters;
import net.lingala.zip4j.util.UnzipUtil;

import java.io.IOException;

import static net.lingala.zip4j.headers.HeaderUtil.getTotalUncompressedSizeOfAllFileHeaders;

public class ExtractAllFilesTask extends AbstractExtractFileTask<ExtractAllFilesTaskParameters> {

  private final char[] password;
  private SplitFileInputStream splitInputStream;

  public ExtractAllFilesTask(ZipModel zipModel, char[] password, UnzipParameters unzipParameters,
                             AsyncTaskParameters asyncTaskParameters) {
    super(zipModel, unzipParameters, asyncTaskParameters);
    this.password = password;
  }

  @Override
  protected void executeTask(ExtractAllFilesTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws IOException {
    try (ZipInputStream zipInputStream = prepareZipInputStream(taskParameters.zip4jConfig)) {
      for (FileHeader fileHeader : getZipModel().getCentralDirectory().getFileHeaders()) {
        if (fileHeader.getFileName().startsWith("__MACOSX")) {
          progressMonitor.updateWorkCompleted(fileHeader.getUncompressedSize());
          continue;
        }

        splitInputStream.prepareExtractionForFileHeader(fileHeader);

        byte[] readBuff = new byte[taskParameters.zip4jConfig.getBufferSize()];
        extractFile(zipInputStream, fileHeader, taskParameters.outputPath, null, progressMonitor, readBuff);
        verifyIfTaskIsCancelled();
      }
    } finally {
      if (splitInputStream != null) {
        splitInputStream.close();
      }
    }
  }

  @Override
  protected long calculateTotalWork(ExtractAllFilesTaskParameters taskParameters) {
    return getTotalUncompressedSizeOfAllFileHeaders(getZipModel().getCentralDirectory().getFileHeaders());
  }

  private ZipInputStream prepareZipInputStream(Zip4jConfig zip4jConfig) throws IOException {
    splitInputStream = UnzipUtil.createSplitInputStream(getZipModel());

    FileHeader fileHeader = getFirstFileHeader(getZipModel());
    if (fileHeader != null) {
      splitInputStream.prepareExtractionForFileHeader(fileHeader);
    }

    return new ZipInputStream(splitInputStream, password, zip4jConfig);
  }

  private FileHeader getFirstFileHeader(ZipModel zipModel) {
    if (zipModel.getCentralDirectory() == null
        || zipModel.getCentralDirectory().getFileHeaders() == null
        || zipModel.getCentralDirectory().getFileHeaders().size() == 0) {
      return null;
    }

    return zipModel.getCentralDirectory().getFileHeaders().get(0);
  }

  public static class ExtractAllFilesTaskParameters extends AbstractZipTaskParameters {
    private final String outputPath;

    public ExtractAllFilesTaskParameters(String outputPath, Zip4jConfig zip4jConfig) {
      super(zip4jConfig);
      this.outputPath = outputPath;
    }
  }

}
