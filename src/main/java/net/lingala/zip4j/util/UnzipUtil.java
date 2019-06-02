package net.lingala.zip4j.util;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.SplitInputStream;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.enums.FileMode;

import java.io.File;
import java.io.IOException;

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

  public static void applyFileAttributes(FileHeader fileHeader, File file) throws ZipException {
    applyFileAttributes(fileHeader, file, null);
  }

  public static void applyFileAttributes(FileHeader fileHeader, File file,
                                         UnzipParameters unzipParameters) throws ZipException {

    if (fileHeader == null) {
      throw new ZipException("cannot set file properties: file header is null");
    }

    if (file == null) {
      throw new ZipException("cannot set file properties: output file is null");
    }

    if (!file.exists()) {
      throw new ZipException("cannot set file properties: file doesnot exist");
    }

    if (unzipParameters == null || !unzipParameters.isIgnoreDateTimeAttributes()) {
      setFileLastModifiedTime(fileHeader, file);
    }

    if (unzipParameters == null) {
      setFileAttributes(fileHeader, file, true, true, true, true);
    } else {
      if (unzipParameters.isIgnoreAllFileAttributes()) {
        setFileAttributes(fileHeader, file, false, false, false, false);
      } else {
        setFileAttributes(fileHeader, file, !unzipParameters.isIgnoreReadOnlyFileAttribute(),
            !unzipParameters.isIgnoreHiddenFileAttribute(),
            !unzipParameters.isIgnoreArchiveFileAttribute(),
            !unzipParameters.isIgnoreSystemFileAttribute());
      }
    }
  }

  private static void setFileAttributes(FileHeader fileHeader, File file, boolean setReadOnly,
                                        boolean setHidden, boolean setArchive, boolean setSystem) throws ZipException {
    if (fileHeader == null) {
      throw new ZipException("invalid file header. cannot set file attributes");
    }

    byte[] externalAttrbs = fileHeader.getExternalFileAttributes();
    if (externalAttrbs == null) {
      return;
    }

    FileMode fileMode = FileMode.getFileModeFromValue(externalAttrbs[0]);

    if (file == null) {
      return;
    }

    //TODO set attributes of file
  }

  private static void setFileLastModifiedTime(FileHeader fileHeader, File file) {
    if (fileHeader.getLastModifiedTime() <= 0) {
      return;
    }

    if (file.exists()) {
      file.setLastModified(Zip4jUtil.dosToJavaTme(fileHeader.getLastModifiedTime()));
    }
  }

}
