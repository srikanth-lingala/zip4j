package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderUtil;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.model.EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.RemoveFilesFromZipTask.RemoveFilesFromZipTaskParameters;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class RemoveFilesFromZipTask extends AbstractModifyFileTask<RemoveFilesFromZipTaskParameters>  {

  private ZipModel zipModel;
  private HeaderWriter headerWriter;

  public RemoveFilesFromZipTask(ZipModel zipModel, HeaderWriter headerWriter, AsyncTaskParameters asyncTaskParameters) {
    super(asyncTaskParameters);
    this.zipModel = zipModel;
    this.headerWriter = headerWriter;
  }

  @Override
  protected void executeTask(RemoveFilesFromZipTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws IOException {
    if (zipModel.isSplitArchive()) {
      throw new ZipException("This is a split archive. Zip file format does not allow updating split/spanned files");
    }

    List<String> entriesToRemove = filterNonExistingEntries(taskParameters.filesToRemove);

    if (entriesToRemove.isEmpty()) {
      return;
    }

    File temporaryZipFile = getTemporaryFile(zipModel.getZipFile().getPath());
    boolean successFlag = false;

    try (SplitOutputStream outputStream = new SplitOutputStream(temporaryZipFile);
         RandomAccessFile inputStream = new RandomAccessFile(zipModel.getZipFile(), RandomAccessFileMode.READ.getValue())){

      long currentFileCopyPointer = 0;
      List<FileHeader> sortedFileHeaders = cloneAndSortFileHeadersByOffset(zipModel.getCentralDirectory().getFileHeaders());

      for (FileHeader fileHeader : sortedFileHeaders) {
        long lengthOfCurrentEntry = getOffsetOfNextEntry(sortedFileHeaders, fileHeader, zipModel) - outputStream.getFilePointer();
        if (shouldEntryBeRemoved(fileHeader, entriesToRemove)) {
          updateHeaders(sortedFileHeaders, fileHeader, lengthOfCurrentEntry);

          if (!zipModel.getCentralDirectory().getFileHeaders().remove(fileHeader)) {
            throw new ZipException("Could not remove entry from list of central directory headers");
          }

          currentFileCopyPointer += lengthOfCurrentEntry;
        } else {
          // copy complete entry without any changes
          currentFileCopyPointer += super.copyFile(inputStream, outputStream, currentFileCopyPointer, lengthOfCurrentEntry, progressMonitor);
        }
        verifyIfTaskIsCancelled();
      }

      headerWriter.finalizeZipFile(zipModel, outputStream, taskParameters.charset);
      successFlag = true;
    } finally {
      cleanupFile(successFlag, zipModel.getZipFile(), temporaryZipFile);
    }
  }

  @Override
  protected long calculateTotalWork(RemoveFilesFromZipTaskParameters taskParameters) {
    return zipModel.getZipFile().length();
  }

  private List<String> filterNonExistingEntries(List<String> filesToRemove) throws ZipException {
    List<String> filteredFilesToRemove = new ArrayList<>();

    for (String fileToRemove : filesToRemove) {
      if (HeaderUtil.getFileHeader(zipModel, fileToRemove) != null) {
        filteredFilesToRemove.add(fileToRemove);
      }
    }

    return filteredFilesToRemove;
  }

  private boolean shouldEntryBeRemoved(FileHeader fileHeaderToBeChecked, List<String> fileNamesToBeRemoved) {
    for (String fileNameToBeRemoved : fileNamesToBeRemoved) {
      if (fileHeaderToBeChecked.getFileName().startsWith(fileNameToBeRemoved)) {
        return true;
      }
    }

    return false;
  }

  private void updateHeaders(List<FileHeader> sortedFileHeaders, FileHeader fileHeaderThatWasRemoved, long offsetToSubtract) throws ZipException {
    updateOffsetsForAllSubsequentFileHeaders(sortedFileHeaders, zipModel, fileHeaderThatWasRemoved, negate(offsetToSubtract));

    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = zipModel.getEndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(
        endOfCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory() - offsetToSubtract);
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectory(
        endOfCentralDirectoryRecord.getTotalNumberOfEntriesInCentralDirectory() - 1);

    if (endOfCentralDirectoryRecord.getTotalNumberOfEntriesInCentralDirectoryOnThisDisk() > 0) {
      endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
          endOfCentralDirectoryRecord.getTotalNumberOfEntriesInCentralDirectoryOnThisDisk() - 1);
    }

    if (zipModel.isZip64Format()) {
      zipModel.getZip64EndOfCentralDirectoryRecord().setOffsetStartCentralDirectoryWRTStartDiskNumber(
          zipModel.getZip64EndOfCentralDirectoryRecord().getOffsetStartCentralDirectoryWRTStartDiskNumber() - offsetToSubtract);

      zipModel.getZip64EndOfCentralDirectoryRecord().setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
          zipModel.getZip64EndOfCentralDirectoryRecord().getTotalNumberOfEntriesInCentralDirectory() - 1);

      zipModel.getZip64EndOfCentralDirectoryLocator().setOffsetZip64EndOfCentralDirectoryRecord(
          zipModel.getZip64EndOfCentralDirectoryLocator().getOffsetZip64EndOfCentralDirectoryRecord() - offsetToSubtract);
    }
  }

  private long negate(long val) {
    if (val == Long.MIN_VALUE) {
      throw new ArithmeticException("long overflow");
    }

    return -val;
  }

  @Override
  protected ProgressMonitor.Task getTask() {
    return ProgressMonitor.Task.REMOVE_ENTRY;
  }

  public static class RemoveFilesFromZipTaskParameters extends AbstractZipTaskParameters {
    private List<String> filesToRemove;

    public RemoveFilesFromZipTaskParameters(List<String> filesToRemove, Charset charset) {
      super(charset);
      this.filesToRemove = filesToRemove;
    }
  }
}
