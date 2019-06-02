package net.lingala.zip4j.tasks;

import lombok.AllArgsConstructor;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.AddFolderToZipTask.AddFolderToZipTaskParameters;
import net.lingala.zip4j.util.Zip4jUtil;

import java.io.File;
import java.util.List;

public class AddFolderToZipTask extends AbstractAddFileToZipTask<AddFolderToZipTaskParameters> {

  private List<File> filesToAdd;

  public AddFolderToZipTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel, char[] password) {
    super(progressMonitor, runInThread, zipModel, password);
  }

  @Override
  protected void executeTask(AddFolderToZipTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws ZipException {
    verifyZipParameters(taskParameters.zipParameters, taskParameters.folderToAdd);
    addFilesToZip(filesToAdd, progressMonitor, taskParameters.zipParameters);
  }

  @Override
  protected long calculateTotalWork(AddFolderToZipTaskParameters taskParameters) throws ZipException {
    String rootFolderPath = getRootFolderPath(taskParameters.zipParameters, taskParameters.folderToAdd);
    taskParameters.zipParameters.setRootFolderInZip(rootFolderPath);
    filesToAdd = Zip4jUtil.getFilesInDirectoryRecursive(taskParameters.folderToAdd,
        taskParameters.zipParameters.isReadHiddenFiles());

    if (taskParameters.zipParameters.isIncludeRootFolder()) {
      filesToAdd.add(taskParameters.folderToAdd);
    }

    return calculateWorkForFiles(filesToAdd, taskParameters.zipParameters);
  }

  private String getRootFolderPath(ZipParameters zipParameters, File folderToAdd) {
    if (!zipParameters.isIncludeRootFolder()) {
      return folderToAdd.getAbsolutePath();
    }

    return folderToAdd.getAbsoluteFile().getParentFile() != null ?
        folderToAdd.getAbsoluteFile().getParentFile().getAbsolutePath() : "";
  }

  private void verifyZipParameters(ZipParameters zipParameters, File folderToAdd) throws ZipException {
    if (folderToAdd == null || zipParameters == null) {
      throw new ZipException("one of the input parameters is null, cannot add folder to zip");
    }

    if (!folderToAdd.exists()) {
      throw new ZipException("input folder does not exist");
    }

    if (!folderToAdd.isDirectory()) {
      throw new ZipException("input file is not a folder, user addFileToZip method to add files");
    }

    if (!folderToAdd.canRead()) {
      throw new ZipException("cannot read folder: " + folderToAdd.getAbsolutePath());
    }
  }

  @AllArgsConstructor
  public static class AddFolderToZipTaskParameters {
    private File folderToAdd;
    private ZipParameters zipParameters;
  }

}
