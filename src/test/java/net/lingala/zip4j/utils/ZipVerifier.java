package net.lingala.zip4j.utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipParameters;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipVerifier {

  public void verifyZipFile(File generatedZipFile, ZipParameters zipParameters,
                            TemporaryFolder temporaryFolder) throws ZipException, IOException {
    assertThat(generatedZipFile).isNotNull();

    ZipFile zipFile = new ZipFile(generatedZipFile);
    if (zipParameters.isEncryptFiles()) {
      zipFile.setPassword(zipParameters.getPassword());
    }
    zipFile.extractAll(temporaryFolder.newFolder().getAbsolutePath(), new UnzipParameters());
  }

}
