package net.lingala.zip4j.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.model.EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static net.lingala.zip4j.util.InternalZipConstants.DEFAULT_COMMENT_CHARSET;
import static net.lingala.zip4j.util.InternalZipConstants.MAX_ALLOWED_ZIP_COMMENT_LENGTH;
import static net.lingala.zip4j.util.Zip4jUtil.isSupportedCharset;

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

    String encodedComment = convertCommentToDefaultCharset(comment);

    if (encodedComment.length() > MAX_ALLOWED_ZIP_COMMENT_LENGTH) {
      throw new ZipException("comment length exceeds maximum length");
    }

    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = zipModel.getEndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setComment(encodedComment);
    endOfCentralDirectoryRecord.setCommentBytes(encodedComment.getBytes());
    endOfCentralDirectoryRecord.setCommentLength(encodedComment.length());

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

  private String convertCommentToDefaultCharset(String comment) throws ZipException {
    if (isSupportedCharset(DEFAULT_COMMENT_CHARSET)) {
      try {
        return new String(comment.getBytes(DEFAULT_COMMENT_CHARSET), DEFAULT_COMMENT_CHARSET);
      } catch (UnsupportedEncodingException e) {
        return comment;
      }
    }
    return comment;
  }
}
