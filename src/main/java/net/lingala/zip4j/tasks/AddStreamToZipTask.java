package net.lingala.zip4j.tasks;

import lombok.AllArgsConstructor;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.AddStreamToZipTask.AddStreamToZipTaskParameters;

import java.io.IOException;
import java.io.InputStream;

import static net.lingala.zip4j.util.InternalZipConstants.BUFF_SIZE;

public class AddStreamToZipTask extends AbstractAddFileToZipTask<AddStreamToZipTaskParameters> {

  public AddStreamToZipTask(ProgressMonitor progressMonitor, boolean runInThread, ZipModel zipModel, char[] password,
                            HeaderWriter headerWriter) {
    super(progressMonitor, runInThread, zipModel, password, headerWriter);
  }

  @Override
  protected void executeTask(AddStreamToZipTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws ZipException {

    verifyZipParameters(taskParameters.zipParameters);

    try(SplitOutputStream splitOutputStream = new SplitOutputStream(getZipModel().getZipFile(), getZipModel().getSplitLength());
        ZipOutputStream zipOutputStream = initializeOutputStream(splitOutputStream)) {

      byte[] readBuff = new byte[BUFF_SIZE];
      int readLen = -1;

      ZipParameters zipParameters = taskParameters.zipParameters;
      zipOutputStream.putNextEntry(zipParameters);

      if (!zipParameters.getFileNameInZip().endsWith("/") &&
          !zipParameters.getFileNameInZip().endsWith("\\")) {
        while ((readLen = taskParameters.inputStream.read(readBuff)) != -1) {
          zipOutputStream.write(readBuff, 0, readLen);
        }
      }

      zipOutputStream.closeEntry();
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  @Override
  protected long calculateTotalWork(AddStreamToZipTaskParameters taskParameters) {
    return 0;
  }

  @AllArgsConstructor
  public static class AddStreamToZipTaskParameters {
    private InputStream inputStream;
    private ZipParameters zipParameters;
  }
}
