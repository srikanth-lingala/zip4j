package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderUtil;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.RawIO;
import net.lingala.zip4j.util.Zip4jUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenameFilesTask extends AbstractModifyFileTask<RenameFilesTask.RenameFilesTaskParameters> {

  private ZipModel zipModel;
  private HeaderWriter headerWriter;
  private RawIO rawIO;
  private Charset charset;

  public RenameFilesTask(ZipModel zipModel, HeaderWriter headerWriter, RawIO rawIO, Charset charset, AsyncTaskParameters asyncTaskParameters) {
    super(asyncTaskParameters);
    this.zipModel = zipModel;
    this.headerWriter = headerWriter;
    this.rawIO = rawIO;
    this.charset = charset;
  }

  @Override
  protected void executeTask(RenameFilesTaskParameters taskParameters, ProgressMonitor progressMonitor) throws IOException {
    Map<String, String> fileNamesMap = filterNonExistingEntriesAndAddSeparatorIfNeeded(taskParameters.fileNamesMap);
    if (fileNamesMap.size() == 0) {
      return;
    }

    File temporaryFile = getTemporaryFile(zipModel.getZipFile().getPath());
    boolean successFlag = false;
    try(RandomAccessFile inputStream = new RandomAccessFile(zipModel.getZipFile(), RandomAccessFileMode.WRITE.getValue());
        SplitOutputStream outputStream = new SplitOutputStream(temporaryFile)) {

      long currentFileCopyPointer = 0;

      // Maintain a different list to iterate, so that when the file name is changed in the central directory
      // we still have access to the original file names. If iterating on the original list from central directory,
      // it might be that a file name has changed because of other file name, ex: if a directory name has to be changed
      // and the file is part of that directory, by the time the file has to be changed, its name might have changed
      // when changing the name of the directory. There is some overhead with this approach, but is safer.
      List<FileHeader> allUnchangedFileHeaders = new ArrayList<>(zipModel.getCentralDirectory().getFileHeaders());

      for (FileHeader fileHeader : allUnchangedFileHeaders) {
        Map.Entry<String, String> fileNameMapForThisEntry = getCorrespondingEntryFromMap(fileHeader, fileNamesMap);
        progressMonitor.setFileName(fileHeader.getFileName());

        long lengthToCopy = HeaderUtil.getOffsetOfNextEntry(zipModel, fileHeader) - outputStream.getFilePointer();
        if (fileNameMapForThisEntry == null) {
          // copy complete entry without any changes
          currentFileCopyPointer += copyFile(inputStream, outputStream, currentFileCopyPointer, lengthToCopy, progressMonitor);
        } else {
          String newFileName = getNewFileName(fileNameMapForThisEntry.getValue(), fileNameMapForThisEntry.getKey(), fileHeader.getFileName());
          byte[] newFileNameBytes = newFileName.getBytes(charset);
          int headersOffset = newFileNameBytes.length - fileHeader.getFileNameLength();

          currentFileCopyPointer = copyEntryAndChangeFileName(newFileNameBytes, fileHeader, currentFileCopyPointer, lengthToCopy,
              inputStream, outputStream, progressMonitor);

          updateHeadersInZipModel(fileHeader, newFileName, newFileNameBytes, headersOffset);
        }

        verifyIfTaskIsCancelled();
      }

      headerWriter.finalizeZipFile(zipModel, outputStream, charset);
      successFlag = true;
    } finally {
      cleanupFile(successFlag, zipModel.getZipFile(), temporaryFile);
    }

  }

  @Override
  protected long calculateTotalWork(RenameFilesTaskParameters taskParameters) {
    return zipModel.getZipFile().length();
  }

  @Override
  protected ProgressMonitor.Task getTask() {
    return ProgressMonitor.Task.RENAME_FILE;
  }

  private long copyEntryAndChangeFileName(byte[] newFileNameBytes, FileHeader fileHeader, long start, long totalLengthOfEntry,
                                          RandomAccessFile inputStream, OutputStream outputStream,
                                          ProgressMonitor progressMonitor) throws IOException {
    long currentFileCopyPointer = start;

    currentFileCopyPointer += copyFile(inputStream, outputStream, currentFileCopyPointer, 26, progressMonitor); // 26 is offset until file name length

    rawIO.writeShortLittleEndian(outputStream, newFileNameBytes.length);

    currentFileCopyPointer += 2; // length of file name length
    currentFileCopyPointer += copyFile(inputStream, outputStream, currentFileCopyPointer, 2, progressMonitor); // 2 is for length of extra field length

    outputStream.write(newFileNameBytes);
    currentFileCopyPointer += fileHeader.getFileNameLength();

    long remainingLengthToCopy = totalLengthOfEntry - (currentFileCopyPointer - start);

    currentFileCopyPointer += copyFile(inputStream, outputStream, currentFileCopyPointer,
       remainingLengthToCopy, progressMonitor);

    return currentFileCopyPointer;
  }

  private Map.Entry<String, String> getCorrespondingEntryFromMap(FileHeader fileHeaderToBeChecked, Map<String,
      String> fileNamesMap) {

    for (Map.Entry<String, String> fileHeaderToBeRenamed : fileNamesMap.entrySet()) {
      if (fileHeaderToBeChecked.getFileName().startsWith(fileHeaderToBeRenamed.getKey())) {
        return fileHeaderToBeRenamed;
      }
    }

    return null;
  }

  private void updateHeadersInZipModel(FileHeader fileHeader, String newFileName, byte[] newFileNameBytes,
                                       int headersOffset) throws ZipException {

    FileHeader fileHeaderToBeChanged = HeaderUtil.getFileHeader(zipModel, fileHeader.getFileName());

    if (fileHeaderToBeChanged == null) {
      // If this is the case, then the file name in the header that was passed to this method was already changed.
      // In theory, should never be here.
      throw new ZipException("could not find any header with name: " + fileHeader.getFileName());
    }

    fileHeaderToBeChanged.setFileName(newFileName);
    fileHeaderToBeChanged.setFileNameLength(newFileNameBytes.length);

    updateOffsetsForAllSubsequentFileHeaders(zipModel, fileHeaderToBeChanged, headersOffset);

    zipModel.getEndOfCentralDirectoryRecord().setOffsetOfStartOfCentralDirectory(
        zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory() + headersOffset);

    if (zipModel.isZip64Format()) {
      zipModel.getZip64EndOfCentralDirectoryRecord().setOffsetStartCentralDirectoryWRTStartDiskNumber(
          zipModel.getZip64EndOfCentralDirectoryRecord().getOffsetStartCentralDirectoryWRTStartDiskNumber() + headersOffset
      );

      zipModel.getZip64EndOfCentralDirectoryLocator().setOffsetZip64EndOfCentralDirectoryRecord(
          zipModel.getZip64EndOfCentralDirectoryLocator().getOffsetZip64EndOfCentralDirectoryRecord() + headersOffset
      );
    }
  }

  private Map<String, String> filterNonExistingEntriesAndAddSeparatorIfNeeded(Map<String, String> inputFileNamesMap) throws ZipException {
    Map<String, String> fileNamesMapToBeChanged = new HashMap<>();
    for (Map.Entry<String, String> allNamesToBeChanged : inputFileNamesMap.entrySet()) {
      if (!Zip4jUtil.isStringNotNullAndNotEmpty(allNamesToBeChanged.getKey())) {
        continue;
      }

      FileHeader fileHeaderToBeChanged = HeaderUtil.getFileHeader(zipModel, allNamesToBeChanged.getKey());
      if (fileHeaderToBeChanged != null) {
        if (fileHeaderToBeChanged.isDirectory() && !allNamesToBeChanged.getValue().endsWith(InternalZipConstants.ZIP_FILE_SEPARATOR)) {
          fileNamesMapToBeChanged.put(allNamesToBeChanged.getKey(), allNamesToBeChanged.getValue() + InternalZipConstants.ZIP_FILE_SEPARATOR);
        } else {
          fileNamesMapToBeChanged.put(allNamesToBeChanged.getKey(), allNamesToBeChanged.getValue());
        }
      }
    }
    return fileNamesMapToBeChanged;
  }

  private String getNewFileName(String newFileName, String oldFileName, String fileNameFromHeaderToBeChanged) throws ZipException {
    if (fileNameFromHeaderToBeChanged.equals(oldFileName)) {
      return newFileName;
    } else if (fileNameFromHeaderToBeChanged.startsWith(oldFileName)) {
      String fileNameWithoutOldName = fileNameFromHeaderToBeChanged.substring(oldFileName.length());
      return newFileName + fileNameWithoutOldName;
    }

    // Should never be here.
    // If here by any chance, it means that the file header was marked as to-be-modified, even when the file names do not
    // match. Logic in the method getCorrespondingEntryFromMap() has to be checked
    throw new ZipException("old file name was neither an exact match nor a partial match");
  }

  public static class RenameFilesTaskParameters {
    private Map<String, String> fileNamesMap;

    public RenameFilesTaskParameters(Map<String, String> fileNamesMap) {
      this.fileNamesMap = fileNamesMap;
    }
  }
}
