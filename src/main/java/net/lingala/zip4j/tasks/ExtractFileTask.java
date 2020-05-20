package net.lingala.zip4j.tasks;

import net.lingala.zip4j.io.inputstream.SplitInputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.ExtractFileTask.ExtractFileTaskParameters;
import net.lingala.zip4j.util.UnzipUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import static net.lingala.zip4j.headers.HeaderUtil.getFileHeadersUnderDirectory;
import static net.lingala.zip4j.headers.HeaderUtil.getTotalUncompressedSizeOfAllFileHeaders;

public class ExtractFileTask extends AbstractExtractFileTask<ExtractFileTaskParameters> {

  private char[] password;
  private SplitInputStream splitInputStream;

  public ExtractFileTask(ZipModel zipModel, char[] password, AsyncTaskParameters asyncTaskParameters) {
    super(zipModel, asyncTaskParameters);
    this.password = password;
  }

  @Override
  protected void executeTask(ExtractFileTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws IOException {

    String newFileName = taskParameters.newFileName;
    if (taskParameters.fileHeader.isDirectory()) {
      newFileName = null;
    }

    try(ZipInputStream zipInputStream = createZipInputStream(taskParameters.fileHeader, taskParameters.charset)) {
      List<FileHeader> fileHeadersUnderDirectory = getFileHeadersToExtract(taskParameters.fileHeader);
      for (FileHeader fileHeader : fileHeadersUnderDirectory) {
        extractFile(zipInputStream, fileHeader, taskParameters.outputPath, newFileName, progressMonitor);
      }
    } finally {
      if (splitInputStream != null) {
        splitInputStream.close();
      }
    }
  }

  @Override
  protected long calculateTotalWork(ExtractFileTaskParameters taskParameters) {
    List<FileHeader> fileHeadersUnderDirectory = getFileHeadersToExtract(taskParameters.fileHeader);
    return getTotalUncompressedSizeOfAllFileHeaders(fileHeadersUnderDirectory);
  }

  private List<FileHeader> getFileHeadersToExtract(FileHeader rootFileHeader) {
    if (!rootFileHeader.isDirectory()) {
      return Collections.singletonList(rootFileHeader);
    }

    return getFileHeadersUnderDirectory(
        getZipModel().getCentralDirectory().getFileHeaders(), rootFileHeader);
  }

  private ZipInputStream createZipInputStream(FileHeader fileHeader, Charset charset) throws IOException {
    splitInputStream = UnzipUtil.createSplitInputStream(getZipModel());
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
