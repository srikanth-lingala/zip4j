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

  private List<File> filesToAdd;

  public AddFolderToZipTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel, char[] password,
                            HeaderWriter headerWriter) {
    super(progressMonitor, runInThread, zipModel, password, headerWriter);
  }

  @Override
  protected void executeTask(AddFolderToZipTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws ZipException {
    addFilesToZip(filesToAdd, progressMonitor, taskParameters.zipParameters);
  }

  @Override
  protected long calculateTotalWork(AddFolderToZipTaskParameters taskParameters) throws ZipException {
    String rootFolderPath = getRootFolderPath(taskParameters.zipParameters, taskParameters.folderToAdd);
    taskParameters.zipParameters.setRootFolderInZip(rootFolderPath);
    filesToAdd = getFilesInDirectoryRecursive(taskParameters.folderToAdd,
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

  @AllArgsConstructor
  public static class AddFolderToZipTaskParameters {
    private File folderToAdd;
    private ZipParameters zipParameters;
  }

}
