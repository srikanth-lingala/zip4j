package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderSignature;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.model.EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.Zip64EndOfCentralDirectoryLocator;
import net.lingala.zip4j.model.Zip64EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.MergeSplitZipFileTask.MergeSplitZipFileTaskParameters;
import net.lingala.zip4j.util.RawIO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.List;

import static net.lingala.zip4j.util.FileUtils.copyFile;

public class MergeSplitZipFileTask extends AsyncZipTask<MergeSplitZipFileTaskParameters> {

  private ZipModel zipModel;
  private RawIO rawIO = new RawIO();

  public MergeSplitZipFileTask(ZipModel zipModel, AsyncTaskParameters asyncTaskParameters) {
    super(asyncTaskParameters);
    this.zipModel = zipModel;
  }

  @Override
  protected void executeTask(MergeSplitZipFileTaskParameters taskParameters, ProgressMonitor progressMonitor) throws IOException {
    if (!zipModel.isSplitArchive()) {
      ZipException e = new ZipException("archive not a split zip file");
      progressMonitor.endProgressMonitor(e);
      throw e;
    }

    try (OutputStream outputStream = new FileOutputStream(taskParameters.outputZipFile)) {
      long totalBytesWritten = 0;
      int totalNumberOfSplitFiles = zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk();
      if (totalNumberOfSplitFiles <= 0) {
        throw new ZipException("zip archive not a split zip file");
      }

      int splitSignatureOverhead = 0;
      for (int i = 0; i <= totalNumberOfSplitFiles; i++) {
        try (RandomAccessFile randomAccessFile = createSplitZipFileStream(zipModel, i)) {
          int start = 0;
          long end = randomAccessFile.length();

          if (i == 0) {
            if (rawIO.readIntLittleEndian(randomAccessFile) == HeaderSignature.SPLIT_ZIP.getValue()) {
              splitSignatureOverhead = 4;
              start = 4;
            } else {
              randomAccessFile.seek(0);
            }
          }

          if (i == totalNumberOfSplitFiles) {
            end = zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory();
          }

          copyFile(randomAccessFile, outputStream, start, end, progressMonitor);
          totalBytesWritten += (end - start);
          updateFileHeaderOffsetsForIndex(zipModel.getCentralDirectory().getFileHeaders(),
              i == 0 ? 0 : totalBytesWritten, i, splitSignatureOverhead);
          verifyIfTaskIsCancelled();
        }
      }
      updateHeadersForMergeSplitFileAction(zipModel, totalBytesWritten, outputStream, taskParameters.charset);
      progressMonitor.endProgressMonitor();
    } catch (CloneNotSupportedException e) {
      throw new ZipException(e);
    }
  }

  @Override
  protected long calculateTotalWork(MergeSplitZipFileTaskParameters taskParameters) {
    if (!zipModel.isSplitArchive()) {
      return 0;
    }

    long totalSize = 0;
    for (int i = 0; i <= zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk(); i++) {
      totalSize += getNextSplitZipFile(zipModel, i).length();
    }
    return totalSize;
  }

  private void updateFileHeaderOffsetsForIndex(List<FileHeader> fileHeaders, long offsetToAdd, int index,
                                               int splitSignatureOverhead) {
    for (FileHeader fileHeader : fileHeaders) {
      if (fileHeader.getDiskNumberStart() == index) {
        fileHeader.setOffsetLocalHeader(fileHeader.getOffsetLocalHeader() + offsetToAdd - splitSignatureOverhead);
        fileHeader.setDiskNumberStart(0);
      }
    }
  }

  private File getNextSplitZipFile(ZipModel zipModel, int partNumber) {
    if (partNumber == zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk()) {
      return zipModel.getZipFile();
    }

    String splitZipExtension = ".z0";
    if (partNumber >= 9) {
      splitZipExtension = ".z";
    }
    String rootZipFile = zipModel.getZipFile().getPath();
    String nextSplitZipFileName =  zipModel.getZipFile().getPath().substring(0, rootZipFile.lastIndexOf("."))
        + splitZipExtension + (partNumber + 1);
    return new File(nextSplitZipFileName);
  }

  private RandomAccessFile createSplitZipFileStream(ZipModel zipModel, int partNumber) throws FileNotFoundException {
    File splitFile = getNextSplitZipFile(zipModel, partNumber);
    return new RandomAccessFile(splitFile, RandomAccessFileMode.READ.getValue());
  }

  private void updateHeadersForMergeSplitFileAction(ZipModel zipModel, long totalBytesWritten,
                                                    OutputStream outputStream, Charset charset)
      throws IOException, CloneNotSupportedException {

    ZipModel newZipModel = (ZipModel) zipModel.clone();
    newZipModel.getEndOfCentralDirectoryRecord().setOffsetOfStartOfCentralDirectory(totalBytesWritten);

    updateSplitZipModel(newZipModel, totalBytesWritten);

    HeaderWriter headerWriter = new HeaderWriter();
    headerWriter.finalizeZipFileWithoutValidations(newZipModel, outputStream, charset);
  }

  private void updateSplitZipModel(ZipModel zipModel, long totalFileSize) {
    zipModel.setSplitArchive(false);
    updateSplitEndCentralDirectory(zipModel);

    if (zipModel.isZip64Format()) {
      updateSplitZip64EndCentralDirLocator(zipModel, totalFileSize);
      updateSplitZip64EndCentralDirRec(zipModel, totalFileSize);
    }
  }

  private void updateSplitEndCentralDirectory(ZipModel zipModel) {
    int numberOfFileHeaders = zipModel.getCentralDirectory().getFileHeaders().size();
    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = zipModel.getEndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setNumberOfThisDisk(0);
    endOfCentralDirectoryRecord.setNumberOfThisDiskStartOfCentralDir(0);
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectory(numberOfFileHeaders);
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(numberOfFileHeaders);
  }

  private void updateSplitZip64EndCentralDirLocator(ZipModel zipModel, long totalFileSize) {
    if (zipModel.getZip64EndOfCentralDirectoryLocator() == null) {
      return;
    }

    Zip64EndOfCentralDirectoryLocator zip64EndOfCentralDirectoryLocator = zipModel
        .getZip64EndOfCentralDirectoryLocator();
    zip64EndOfCentralDirectoryLocator.setNumberOfDiskStartOfZip64EndOfCentralDirectoryRecord(0);
    zip64EndOfCentralDirectoryLocator.setOffsetZip64EndOfCentralDirectoryRecord(
        zip64EndOfCentralDirectoryLocator.getOffsetZip64EndOfCentralDirectoryRecord() + totalFileSize);
    zip64EndOfCentralDirectoryLocator.setTotalNumberOfDiscs(1);
  }

  private void updateSplitZip64EndCentralDirRec(ZipModel zipModel, long totalFileSize) {
    if (zipModel.getZip64EndOfCentralDirectoryRecord() == null) {
      return;
    }

    Zip64EndOfCentralDirectoryRecord zip64EndOfCentralDirectoryRecord = zipModel.getZip64EndOfCentralDirectoryRecord();
    zip64EndOfCentralDirectoryRecord.setNumberOfThisDisk(0);
    zip64EndOfCentralDirectoryRecord.setNumberOfThisDiskStartOfCentralDirectory(0);
    zip64EndOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
        zipModel.getEndOfCentralDirectoryRecord().getTotalNumberOfEntriesInCentralDirectory());
    zip64EndOfCentralDirectoryRecord.setOffsetStartCentralDirectoryWRTStartDiskNumber(
        zip64EndOfCentralDirectoryRecord.getOffsetStartCentralDirectoryWRTStartDiskNumber() + totalFileSize);
  }

  @Override
  protected ProgressMonitor.Task getTask() {
    return ProgressMonitor.Task.MERGE_ZIP_FILES;
  }

  public static class MergeSplitZipFileTaskParameters extends AbstractZipTaskParameters {
    private File outputZipFile;

    public MergeSplitZipFileTaskParameters(File outputZipFile, Charset charset) {
      super(charset);
      this.outputZipFile = outputZipFile;
    }
  }
}
