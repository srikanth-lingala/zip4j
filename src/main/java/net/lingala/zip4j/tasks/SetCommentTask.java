package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.model.EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.IOException;

public class SetCommentTask extends AsyncZipTask<String> {

  private ZipModel zipModel;

  public SetCommentTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel) {
    super(progressMonitor, runInThread);
    this.zipModel = zipModel;
  }

  @Override
  protected void executeTask(String comment, ProgressMonitor progressMonitor) throws ZipException {
    if (comment == null) {
      throw new ZipException("comment is null, cannot update Zip file with comment");
    }

    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = zipModel.getEndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setComment(comment);

    try (SplitOutputStream outputStream = new SplitOutputStream(zipModel.getZipFile())) {
      if (zipModel.isZip64Format()) {
        outputStream.seek(zipModel.getZip64EndOfCentralDirectoryRecord()
            .getOffsetStartCentralDirectoryWRTStartDiskNumber());
      } else {
        outputStream.seek(endOfCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory());
      }

      HeaderWriter headerWriter = new HeaderWriter();
      headerWriter.finalizeZipFileWithoutValidations(zipModel, outputStream);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  @Override
  protected long calculateTotalWork(String taskParameters) {
    return 0;
  }

  @Override
  protected ProgressMonitor.Task getTask() {
    return ProgressMonitor.Task.SET_COMMENT;
  }
}
