package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.FileHeaderFactory;
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
import java.util.List;

import static net.lingala.zip4j.headers.HeaderUtil.getIndexOfFileHeader;

public class RenameFileInZipTask extends AbstractModifyFileTask<RenameFileInZipTaskParameters> {
  private ZipModel zipModel;
  private FileHeaderFactory fileHeaderFactory = new FileHeaderFactory();

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

    File temporaryZipFile = getTemporaryFile(zipModel.getZipFile().getPath());
    boolean successFlag = false;

    try (SplitOutputStream outputStream = new SplitOutputStream(temporaryZipFile);
         RandomAccessFile inputStream = new RandomAccessFile(zipModel.getZipFile(),
                 RandomAccessFileMode.READ.getValue())){
      HeaderWriter headerWriter = new HeaderWriter();
      HeaderReader headerReader = new HeaderReader();
      int indexOfFileHeader = getIndexOfFileHeader(zipModel, taskParameters.fileHeader);
      long offsetLocalFileHeader = getOffsetLocalFileHeader(taskParameters.fileHeader);
      long offsetStartOfCentralDirectory = getOffsetOfStartOfCentralDirectory(zipModel);
      // read the local file header
      inputStream.seek(offsetLocalFileHeader);
      LocalFileHeader localFileHeader = headerReader.readLocalFileHeader(Channels.newInputStream(inputStream.getChannel()), taskParameters.charset);
      long offsetEndOfLocalFileHeader = getOffsetEndOfLocalFileHeader(offsetLocalFileHeader, localFileHeader);

      int newFileNameLength = taskParameters.newFileName.getBytes(taskParameters.charset).length;
      // calculate the diff of length in local file header, which is caused by the change of file name
      int fileNameLengthDiff = newFileNameLength - localFileHeader.getFileNameLength();
      localFileHeader.setFileName(taskParameters.newFileName);
      localFileHeader.setFileNameLength(newFileNameLength);

      if(indexOfFileHeader > 0) {
        // copy the content before the changed local file header
        FileUtils.copyFile(inputStream, outputStream, 0, offsetLocalFileHeader, progressMonitor);
      }

      // write the new local file header with new file name
      headerWriter.writeLocalFileHeader(zipModel, localFileHeader, outputStream, taskParameters.charset);

      // copy the rest of the content in this file, and the data of other files
      FileUtils.copyFile(inputStream, outputStream, offsetEndOfLocalFileHeader, offsetStartOfCentralDirectory, progressMonitor);

      updateHeaders(outputStream, indexOfFileHeader, taskParameters.newFileName, newFileNameLength, fileNameLengthDiff);
      headerWriter.finalizeZipFile(zipModel, outputStream, taskParameters.charset);

      successFlag = true;
    } finally {
      cleanupFile(successFlag, zipModel.getZipFile(), temporaryZipFile);
    }
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
   * @param splitOutputStream
   * @param indexOfFileHeader
   * @param newFileName
   * @param newFileNameLength
   * @param fileNameLengthDiff
   * @throws IOException
   */
  private void updateHeaders(SplitOutputStream splitOutputStream, int indexOfFileHeader, String newFileName,
                             int newFileNameLength, int fileNameLengthDiff) throws IOException {
    FileHeader fileHeader = zipModel.getCentralDirectory().getFileHeaders().get(indexOfFileHeader);
    fileHeader.setFileName(newFileName);
    fileHeader.setFileNameLength(newFileNameLength);
    if(fileNameLengthDiff != 0) {
      // no need to update the offsets if the length of 2 file names are same
      updateEndOfCentralDirectoryRecord(splitOutputStream);
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
