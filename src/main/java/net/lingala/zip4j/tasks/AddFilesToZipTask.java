package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.model.Zip4jConfig;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.AddFilesToZipTask.AddFilesToZipTaskParameters;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AddFilesToZipTask extends AbstractAddFileToZipTask<AddFilesToZipTaskParameters> {

  public AddFilesToZipTask(ZipModel zipModel, char[] password, HeaderWriter headerWriter,
                           AsyncTaskParameters asyncTaskParameters) {
    super(zipModel, password, headerWriter, asyncTaskParameters);
  }

  @Override
  protected void executeTask(AddFilesToZipTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws IOException {

    verifyZipParameters(taskParameters.zipParameters);
    addFilesToZip(taskParameters.filesToAdd, progressMonitor, taskParameters.zipParameters, taskParameters.zip4jConfig);
  }

  @Override
  protected long calculateTotalWork(AddFilesToZipTaskParameters taskParameters) throws ZipException {
    return calculateWorkForFiles(taskParameters.filesToAdd, taskParameters.zipParameters);
  }

  @Override
  protected ProgressMonitor.Task getTask() {
    return super.getTask();
  }

  public static class AddFilesToZipTaskParameters extends AbstractZipTaskParameters {
    private final List<File> filesToAdd;
    private final ZipParameters zipParameters;

    public AddFilesToZipTaskParameters(List<File> filesToAdd, ZipParameters zipParameters, Zip4jConfig zip4jConfig) {
      super(zip4jConfig);
      this.filesToAdd = filesToAdd;
      this.zipParameters = zipParameters;
    }
  }
}
