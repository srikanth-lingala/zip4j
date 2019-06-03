package net.lingala.zip4j.util;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.SplitInputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;

import java.io.IOException;
import java.nio.file.Path;

import static net.lingala.zip4j.util.FileUtils.setFileAttributes;
import static net.lingala.zip4j.util.FileUtils.setFileLastModifiedTime;

public class UnzipUtil {

  public static ZipInputStream createZipInputStreamFor(ZipModel zipModel, FileHeader fileHeader, char[] password)
      throws ZipException {
    try {
      SplitInputStream splitInputStream = new SplitInputStream(zipModel.getZipFile(), zipModel.isSplitArchive(),
          zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk());
      splitInputStream.prepareExtractionForFileHeader(fileHeader);
      return new ZipInputStream(splitInputStream, password);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  public static void applyFileAttributes(FileHeader fileHeader, Path file) {
    setFileAttributes(file, fileHeader.getExternalFileAttributes());
    setFileLastModifiedTime(file, fileHeader);
  }

}
