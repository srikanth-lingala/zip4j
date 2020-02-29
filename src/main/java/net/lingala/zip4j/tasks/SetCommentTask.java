package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.model.EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.SetCommentTask.SetCommentTaskTaskParameters;

import java.io.IOException;
import java.nio.charset.Charset;

public class SetCommentTask extends AsyncZipTask<SetCommentTaskTaskParameters> {

  private ZipModel zipModel;

  public SetCommentTask(ZipModel zipModel, AsyncTaskParameters asyncTaskParameters) {
    super(asyncTaskParameters);
    this.zipModel = zipModel;
  }

  @Override
  protected void executeTask(SetCommentTaskTaskParameters taskParameters, ProgressMonitor progressMonitor) throws IOException {
    if (taskParameters.comment == null) {
      throw new ZipException("comment is null, cannot update Zip file with comment");
    }

    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = zipModel.getEndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setComment(taskParameters.comment);

    try (SplitOutputStream outputStream = new SplitOutputStream(zipModel.getZipFile())) {
      if (zipModel.isZip64Format()) {
        outputStream.seek(zipModel.getZip64EndOfCentralDirectoryRecord()
            .getOffsetStartCentralDirectoryWRTStartDiskNumber());
      } else {
        outputStream.seek(endOfCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory());
      }

      HeaderWriter headerWriter = new HeaderWriter();
      headerWriter.finalizeZipFileWithoutValidations(zipModel, outputStream, taskParameters.charset);
    }
  }

  @Override
  protected long calculateTotalWork(SetCommentTaskTaskParameters taskParameters) {
    return 0;
  }

  @Override
  protected ProgressMonitor.Task getTask() {
    return ProgressMonitor.Task.SET_COMMENT;
  }

  public static class SetCommentTaskTaskParameters extends AbstractZipTaskParameters {
    private String comment;

    public SetCommentTaskTaskParameters(String comment, Charset charset) {
      super(charset);
      this.comment = comment;
    }
  }
}
