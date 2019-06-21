package net.lingala.zip4j.tasks;

import lombok.AllArgsConstructor;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.AddFolderToZipTask.AddFolderToZipTaskParameters;

import java.io.File;
import java.util.List;

import static net.lingala.zip4j.util.FileUtils.getFilesInDirectoryRecursive;

public class AddFolderToZipTask extends AbstractAddFileToZipTask<AddFolderToZipTaskParameters> {

  public AddFolderToZipTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel, char[] password,
                            HeaderWriter headerWriter) {
    super(progressMonitor, runInThread, zipModel, password, headerWriter);
  }

  @Override
  protected void executeTask(AddFolderToZipTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws ZipException {
    List<File> filesToAdd = getFilesToAdd(taskParameters);
    setDefaultFolderPath(taskParameters);
    addFilesToZip(filesToAdd, progressMonitor, taskParameters.zipParameters);
  }

  @Override
  protected long calculateTotalWork(AddFolderToZipTaskParameters taskParameters) throws ZipException {
    List<File> filesToAdd = getFilesInDirectoryRecursive(taskParameters.folderToAdd,
        taskParameters.zipParameters.isReadHiddenFiles());

    if (taskParameters.zipParameters.isIncludeRootFolder()) {
      filesToAdd.add(taskParameters.folderToAdd);
    }

    return calculateWorkForFiles(filesToAdd, taskParameters.zipParameters);
  }

  private void setDefaultFolderPath(AddFolderToZipTaskParameters taskParameters) {
    String rootFolderPath;
    File folderToAdd = taskParameters.folderToAdd;
    if (taskParameters.zipParameters.isIncludeRootFolder()) {
      rootFolderPath = folderToAdd.getParentFile().getPath();
    } else {
      rootFolderPath = folderToAdd.getAbsolutePath();
    }

    taskParameters.zipParameters.setDefaultFolderPath(rootFolderPath);
  }

  private List<File> getFilesToAdd(AddFolderToZipTaskParameters taskParameters) throws ZipException {
    List<File> filesToAdd = getFilesInDirectoryRecursive(taskParameters.folderToAdd,
        taskParameters.zipParameters.isReadHiddenFiles());

    if (taskParameters.zipParameters.isIncludeRootFolder()) {
      filesToAdd.add(taskParameters.folderToAdd);
    }

    return filesToAdd;
  }

  @AllArgsConstructor
  public static class AddFolderToZipTaskParameters {
    private File folderToAdd;
    private ZipParameters zipParameters;
  }

}
