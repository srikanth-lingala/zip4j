package net.lingala.zip4j.io.outputstream;

import net.lingala.zip4j.exception.ZipException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static net.lingala.zip4j.util.InternalZipConstants.MIN_SPLIT_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SplitOutputStreamTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testOversizedBufferDoesNotStartAnotherSplitFile() throws IOException {
    File zipFile = new File(temporaryFolder.getRoot(), "test.zip");
    try (SplitOutputStream outputStream = new SplitOutputStream(zipFile, MIN_SPLIT_LENGTH)) {
      assertThatThrownBy(() -> outputStream.checkBufferSizeAndStartNextSplitFile((int) MIN_SPLIT_LENGTH + 1))
          .isInstanceOf(ZipException.class)
          .hasMessageContaining("exceeds split length");
      assertThat(outputStream.getCurrentSplitFileCounter()).isZero();
      assertThat(new File(temporaryFolder.getRoot(), "test.z01")).doesNotExist();
    }
  }

  @Test
  public void testBufferAtSplitLengthFitsAfterRollover() throws IOException {
    File zipFile = new File(temporaryFolder.getRoot(), "test.zip");
    try (SplitOutputStream outputStream = new SplitOutputStream(zipFile, MIN_SPLIT_LENGTH)) {
      assertThat(outputStream.checkBufferSizeAndStartNextSplitFile((int) MIN_SPLIT_LENGTH)).isFalse();
      outputStream.write(new byte[1]);
      assertThat(outputStream.checkBufferSizeAndStartNextSplitFile((int) MIN_SPLIT_LENGTH)).isTrue();
      assertThat(outputStream.checkBufferSizeAndStartNextSplitFile((int) MIN_SPLIT_LENGTH)).isFalse();
    }
  }

  @Test
  public void testNonSplitStreamAcceptsLargeBufferSize() throws IOException {
    try (SplitOutputStream outputStream = new SplitOutputStream(temporaryFolder.newFile())) {
      assertThat(outputStream.checkBufferSizeAndStartNextSplitFile(Integer.MAX_VALUE)).isFalse();
    }
  }
}
