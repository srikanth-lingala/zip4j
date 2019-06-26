package net.lingala.zip4j.util;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.SplitInputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static net.lingala.zip4j.util.FileUtils.setFileAttributes;
import static net.lingala.zip4j.util.FileUtils.setFileLastModifiedTime;
import static net.lingala.zip4j.util.FileUtils.setFileLastModifiedTimeWithoutNio;

public class UnzipUtil {

  public static ZipInputStream createZipInputStream(ZipModel zipModel, FileHeader fileHeader, char[] password)
      throws ZipException {
    try {
      SplitInputStream splitInputStream = new SplitInputStream(zipModel.getZipFile(), zipModel.isSplitArchive(),
          zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk());
      splitInputStream.prepareExtractionForFileHeader(fileHeader);

      ZipInputStream zipInputStream = new ZipInputStream(splitInputStream, password);
      if (zipInputStream.getNextEntry() == null) {
        throw new ZipException("Could not locate local file header for corresponding file header");
      }

      return zipInputStream;
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  public static void applyFileAttributes(FileHeader fileHeader, File file) {

    try {
      Path path = file.toPath();
      setFileAttributes(path, fileHeader.getExternalFileAttributes());
      setFileLastModifiedTime(path, fileHeader.getLastModifiedTime());
    } catch (NoSuchMethodError e) {
      setFileLastModifiedTimeWithoutNio(file, fileHeader.getLastModifiedTime());
    }
  }

}
