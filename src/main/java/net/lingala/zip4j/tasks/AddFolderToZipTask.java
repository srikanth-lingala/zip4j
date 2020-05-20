package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.AddFolderToZipTask.AddFolderToZipTaskParameters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static net.lingala.zip4j.util.FileUtils.getFilesInDirectoryRecursive;

public class AddFolderToZipTask extends AbstractAddFileToZipTask<AddFolderToZipTaskParameters> {

  public AddFolderToZipTask(ZipModel zipModel, char[] password, HeaderWriter headerWriter, AsyncTaskParameters asyncTaskParameters) {
    super(zipModel, password, headerWriter, asyncTaskParameters);
  }

  @Override
  protected void executeTask(AddFolderToZipTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws IOException {
    List<File> filesToAdd = getFilesToAdd(taskParameters);
    setDefaultFolderPath(taskParameters);
    addFilesToZip(filesToAdd, progressMonitor, taskParameters.zipParameters, taskParameters.charset);
  }

  @Override
  protected long calculateTotalWork(AddFolderToZipTaskParameters taskParameters) throws ZipException {
    List<File> filesToAdd = getFilesInDirectoryRecursive(taskParameters.folderToAdd,
        taskParameters.zipParameters.isReadHiddenFiles(),
        taskParameters.zipParameters.isReadHiddenFolders(),
        taskParameters.zipParameters.getExcludeFileFilter());

    if (taskParameters.zipParameters.isIncludeRootFolder()) {
      filesToAdd.add(taskParameters.folderToAdd);
    }

    return calculateWorkForFiles(filesToAdd, taskParameters.zipParameters);
  }

  private void setDefaultFolderPath(AddFolderToZipTaskParameters taskParameters) throws IOException {
    String rootFolderPath;
    File folderToAdd = taskParameters.folderToAdd;
    if (taskParameters.zipParameters.isIncludeRootFolder()) {
      File parentFile = folderToAdd.getCanonicalFile().getParentFile();
      if (parentFile == null) {
        rootFolderPath = folderToAdd.getCanonicalPath();
      } else {
        rootFolderPath = folderToAdd.getCanonicalFile().getParentFile().getCanonicalPath();
      }
    } else {
      rootFolderPath = folderToAdd.getCanonicalPath();
    }

    taskParameters.zipParameters.setDefaultFolderPath(rootFolderPath);
  }

  private List<File> getFilesToAdd(AddFolderToZipTaskParameters taskParameters) throws ZipException {
    List<File> filesToAdd = getFilesInDirectoryRecursive(taskParameters.folderToAdd,
        taskParameters.zipParameters.isReadHiddenFiles(),
        taskParameters.zipParameters.isReadHiddenFolders(),
        taskParameters.zipParameters.getExcludeFileFilter());

    if (taskParameters.zipParameters.isIncludeRootFolder()) {
      filesToAdd.add(taskParameters.folderToAdd);
    }

    return filesToAdd;
  }

  public static class AddFolderToZipTaskParameters extends AbstractZipTaskParameters {
    private File folderToAdd;
    private ZipParameters zipParameters;

    public AddFolderToZipTaskParameters(File folderToAdd, ZipParameters zipParameters, Charset charset) {
      super(charset);
      this.folderToAdd = folderToAdd;
      this.zipParameters = zipParameters;
    }
  }

}
