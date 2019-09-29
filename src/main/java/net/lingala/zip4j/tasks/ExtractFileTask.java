package net.lingala.zip4j.tasks;

import net.lingala.zip4j.io.inputstream.SplitInputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.ExtractFileTask.ExtractFileTaskParameters;

import java.io.IOException;
import java.nio.charset.Charset;

public class ExtractFileTask extends AbstractExtractFileTask<ExtractFileTaskParameters> {

  private char[] password;
  private SplitInputStream splitInputStream;

  public ExtractFileTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel, char[] password) {
    super(progressMonitor, runInThread, zipModel);
    this.password = password;
  }

  @Override
  protected void executeTask(ExtractFileTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws IOException {
    try(ZipInputStream zipInputStream = createZipInputStream(taskParameters.fileHeader, taskParameters.charset)) {
      extractFile(zipInputStream, taskParameters.fileHeader, taskParameters.outputPath, taskParameters.newFileName,
          progressMonitor);
    } finally {
      if (splitInputStream != null) {
        splitInputStream.close();
      }
    }
  }

  @Override
  protected long calculateTotalWork(ExtractFileTaskParameters taskParameters) {
    return taskParameters.fileHeader.getUncompressedSize();
  }

  protected ZipInputStream createZipInputStream(FileHeader fileHeader, Charset charset) throws IOException {
    splitInputStream = new SplitInputStream(getZipModel().getZipFile(),
        getZipModel().isSplitArchive(), getZipModel().getEndOfCentralDirectoryRecord().getNumberOfThisDisk());
    splitInputStream.prepareExtractionForFileHeader(fileHeader);
    return new ZipInputStream(splitInputStream, password, charset);
  }

  public static class ExtractFileTaskParameters extends AbstractZipTaskParameters {
    private String outputPath;
    private FileHeader fileHeader;
    private String newFileName;

    public ExtractFileTaskParameters(String outputPath, FileHeader fileHeader, String newFileName, Charset charset) {
      super(charset);
      this.outputPath = outputPath;
      this.fileHeader = fileHeader;
      this.newFileName = newFileName;
    }
  }
}
