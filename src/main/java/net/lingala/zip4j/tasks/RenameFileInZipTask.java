package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderReader;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.model.EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.RenameFileInZipTask.RenameFileInZipTaskParameters;
import net.lingala.zip4j.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.lingala.zip4j.headers.HeaderUtil.getIndexOfFileHeader;
import static net.lingala.zip4j.headers.HeaderUtil.getFileHeader;

public class RenameFileInZipTask extends AbstractModifyFileTask<RenameFileInZipTaskParameters> {
  private ZipModel zipModel;

  public RenameFileInZipTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel) {
    super(progressMonitor, runInThread);
    this.zipModel = zipModel;
  }

  @Override
  protected void executeTask(RenameFileInZipTaskParameters taskParameters, ProgressMonitor progressMonitor) throws IOException {
    if (zipModel.isSplitArchive()) {
      throw new ZipException("This is a split archive. Zip file format does not allow updating split/spanned files");
    }

    // no need to update file name if they are the same
    if(taskParameters.newFileName.equalsIgnoreCase(taskParameters.fileHeader.getFileName())) {
      return;
    }

    if(checkIfNewNameIsUsed(taskParameters.newFileName)) {
      throw new ZipException("The new file name already exists in the zip file.");
    }

    File temporaryZipFile = getTemporaryFile(zipModel.getZipFile().getPath());
    boolean successFlag = false;

    try (SplitOutputStream outputStream = new SplitOutputStream(temporaryZipFile);
         RandomAccessFile inputStream = new RandomAccessFile(zipModel.getZipFile(),
                 RandomAccessFileMode.READ.getValue())){

      HeaderWriter headerWriter = new HeaderWriter();
      if(!taskParameters.fileHeader.isDirectory()) {
        Map<FileHeader, Long> localFileHeaderOffsetMap = new HashMap<>();
        localFileHeaderOffsetMap.put(taskParameters.fileHeader, getOffsetLocalFileHeader(taskParameters.fileHeader));

        writeLocalFileHeaderAndUpdateOffsets(taskParameters.fileHeader, null, localFileHeaderOffsetMap, true,
                taskParameters.newFileName, taskParameters.charset, outputStream, inputStream, headerWriter, progressMonitor);
      } else {
        // if the directory is being renamed, the files in the directory should also be renamed
        List<FileHeader> fileHeaders = zipModel.getCentralDirectory().getFileHeaders();
        List<FileHeader> fileHeadersInDirectory = new ArrayList<>();
        String oldName = taskParameters.fileHeader.getFileName();

        Map<FileHeader, Long> localFileHeaderOffsetMap = new HashMap<>();
        for(FileHeader fileHeader : fileHeaders) {
          if(fileHeader.getFileName().startsWith(oldName)) {
            fileHeadersInDirectory.add(fileHeader);
            // store the local file header offset here as it may mutate after update
            localFileHeaderOffsetMap.put(fileHeader, getOffsetLocalFileHeader(fileHeader));
          }
        }

        for(int i = 0; i < fileHeadersInDirectory.size();i++) {
          FileHeader fileHeader = fileHeadersInDirectory.get(i);
          FileHeader nextFileHeader = null;
          if(i < fileHeadersInDirectory.size() - 1) {
            nextFileHeader = fileHeadersInDirectory.get(i + 1);
          }

          boolean isFirstFileHeader = i == 0 ? true : false;
          String newFileName = fileHeader.getFileName().replaceFirst(oldName, taskParameters.newFileName);

          writeLocalFileHeaderAndUpdateOffsets(fileHeader, nextFileHeader, localFileHeaderOffsetMap, isFirstFileHeader, newFileName,
                  taskParameters.charset, outputStream, inputStream, headerWriter, progressMonitor);
        }
      }

      headerWriter.finalizeZipFile(zipModel, outputStream, taskParameters.charset);
      successFlag = true;
    } finally {
      cleanupFile(successFlag, zipModel.getZipFile(), temporaryZipFile);
    }
  }

  private void writeLocalFileHeaderAndUpdateOffsets(FileHeader fileHeader, FileHeader nextFileHeader, Map<FileHeader, Long> localFileHeaderOffsetMap, boolean isFirstHeader, String newFileName, Charset charset,
                                                    SplitOutputStream outputStream, RandomAccessFile inputStream, HeaderWriter headerWriter, ProgressMonitor progressMonitor) throws IOException {
    HeaderReader headerReader = new HeaderReader();
    int indexOfFileHeader = getIndexOfFileHeader(zipModel, fileHeader);
    long offsetLocalFileHeader = localFileHeaderOffsetMap.get(fileHeader);
    inputStream.seek(offsetLocalFileHeader);
    LocalFileHeader localFileHeader = headerReader.readLocalFileHeader(Channels.newInputStream(inputStream.getChannel()), charset);
    long offsetEndOfLocalFileHeader = getOffsetEndOfLocalFileHeader(offsetLocalFileHeader, localFileHeader);

    int newFileNameLength = newFileName.getBytes(charset).length;
    // calculate the diff of length in local file header, which is caused by the change of file name
    int fileNameLengthDiff = newFileNameLength - localFileHeader.getFileNameLength();
    localFileHeader.setFileName(newFileName);
    localFileHeader.setFileNameLength(newFileNameLength);

    if(isFirstHeader && indexOfFileHeader > 0) {
      // copy the content before the changed local file header
      FileUtils.copyFile(inputStream, outputStream, 0, offsetLocalFileHeader, progressMonitor);
    }

    // write the new local file header with new file name
    headerWriter.writeLocalFileHeader(zipModel, localFileHeader, outputStream, charset);

    // copy the rest of the content in this file, and the data of other files
    long writeEndOffset;
    if(nextFileHeader == null) {
      writeEndOffset = getOffsetOfStartOfCentralDirectory(zipModel);
    } else {
      writeEndOffset = localFileHeaderOffsetMap.get(nextFileHeader);
    }
    FileUtils.copyFile(inputStream, outputStream, offsetEndOfLocalFileHeader, writeEndOffset, progressMonitor);

    if(nextFileHeader == null) {
      // update the offset of start of central directory when finish writing all headers
      updateEndOfCentralDirectoryRecord(outputStream);
    }
    updateHeaders(indexOfFileHeader, newFileName, newFileNameLength, fileNameLengthDiff);
  }

  private boolean checkIfNewNameIsUsed(String newFileName) throws ZipException {
    FileHeader fileHeader = getFileHeader(zipModel, newFileName);
    if(fileHeader != null) {
      return true;
    }

    return false;
  }

  /**
   * local file header signature     4 bytes  (0x04034b50)
   * version needed to extract       2 bytes
   * general purpose bit flag        2 bytes
   * compression method              2 bytes
   * last mod file time              2 bytes
   * last mod file date              2 bytes
   * crc-32                          4 bytes
   * compressed size                 4 bytes
   * uncompressed size               4 bytes
   * file name length                2 bytes
   * extra field length              2 bytes
   *
   * file name (variable size)
   * extra field (variable size)
   *
   * @param offsetLocalFileHeader
   * @param localFileHeader
   * @return
   */
  private long getOffsetEndOfLocalFileHeader(long offsetLocalFileHeader, LocalFileHeader localFileHeader) {
    return offsetLocalFileHeader + 4 + 2 + 2 + 2 + 2 + 2 + 4 + 4 + 4 + 2 + 2 + localFileHeader.getFileNameLength() + localFileHeader.getExtraFieldLength();
  }

  /**
   * the file name and file name length in central directory header should be changed accordingly,
   * the offset of the offset of start of central directory should be changed accordingly
   * the offset of the files after the changed file should be changed accordingly
   * @param indexOfFileHeader
   * @param newFileName
   * @param newFileNameLength
   * @param fileNameLengthDiff
   * @throws IOException
   */
  private void updateHeaders(int indexOfFileHeader, String newFileName,
                             int newFileNameLength, int fileNameLengthDiff) throws IOException {
    FileHeader fileHeader = zipModel.getCentralDirectory().getFileHeaders().get(indexOfFileHeader);
    fileHeader.setFileName(newFileName);
    fileHeader.setFileNameLength(newFileNameLength);
    if(fileNameLengthDiff != 0) {
      // no need to update the offsets if the length of 2 file names are same
      updateFileHeadersWithLocalHeaderOffsets(zipModel.getCentralDirectory().getFileHeaders(), fileNameLengthDiff, indexOfFileHeader);
    }
  }

  /**
   * the offset of the offset of start of central directory should be changed accordingly
   * @param splitOutputStream
   * @throws IOException
   */
  private void updateEndOfCentralDirectoryRecord(SplitOutputStream splitOutputStream) throws IOException {
    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = zipModel.getEndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(splitOutputStream.getFilePointer());
  }

  /**
   * the offset of the files after the changed file should be changed accordingly
   * @param fileHeaders
   * @param fileNameLengthDiff
   * @param indexOfFileHeader
   */
  private void updateFileHeadersWithLocalHeaderOffsets(List<FileHeader> fileHeaders, int fileNameLengthDiff,
                                                       int indexOfFileHeader) {
    for (int i = indexOfFileHeader + 1; i < fileHeaders.size(); i ++) {
      // for files after the renamed file, the offset should be updated
      FileHeader fileHeader = fileHeaders.get(i);
      long offsetLocalHdr = fileHeader.getOffsetLocalHeader();
      if (fileHeader.getZip64ExtendedInfo() != null && fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() != -1) {
        offsetLocalHdr = fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader();
      }
      fileHeader.setOffsetLocalHeader(offsetLocalHdr + fileNameLengthDiff);
    }
  }

  @Override
  protected long calculateTotalWork(RenameFileInZipTaskParameters taskParameters) {
    return zipModel.getZipFile().length();
  }

  @Override
  protected ProgressMonitor.Task getTask() {
    return ProgressMonitor.Task.RENAME_FILE;
  }

  public static class RenameFileInZipTaskParameters extends AbstractZipTaskParameters {
    private FileHeader fileHeader;
    private String newFileName;

    public RenameFileInZipTaskParameters(FileHeader fileHeader, String newFileName, Charset charset) {
      super(charset);
      this.fileHeader = fileHeader;
      this.newFileName = newFileName;
    }
  }
}
