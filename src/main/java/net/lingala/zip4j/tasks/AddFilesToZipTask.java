package net.lingala.zip4j.tasks;

import lombok.AllArgsConstructor;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.AddFilesToZipTask.AddFilesToZipTaskParameters;

import java.io.File;
import java.util.List;

public class AddFilesToZipTask extends AbstractAddFileToZipTask<AddFilesToZipTaskParameters> {

  public AddFilesToZipTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel, char[] password) {
    super(progressMonitor, runInThread, zipModel, password);
  }

  @Override
  protected void executeTask(AddFilesToZipTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws ZipException {

    verifyZipParameters(taskParameters.zipParameters);
    addFilesToZip(taskParameters.filesToAdd, progressMonitor, taskParameters.zipParameters);
  }

  @Override
  protected long calculateTotalWork(AddFilesToZipTaskParameters taskParameters) throws ZipException {
    return calculateWorkForFiles(taskParameters.filesToAdd, taskParameters.zipParameters);
  }

  @AllArgsConstructor
  public static class AddFilesToZipTaskParameters {
    private List<File> filesToAdd;
    private ZipParameters zipParameters;
  }
}
