package net.lingala.zip4j.headers;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static net.lingala.zip4j.util.InternalZipConstants.ZIP_STANDARD_CHARSET;
import static net.lingala.zip4j.util.Zip4jUtil.isStringNotNullAndNotEmpty;

public class HeaderUtil {

  public static FileHeader getFileHeader(ZipModel zipModel, String fileName) throws ZipException {
    FileHeader fileHeader = getFileHeaderWithExactMatch(zipModel, fileName);

    if (fileHeader == null) {
      fileName = fileName.replaceAll("\\\\", "/");
      fileHeader = getFileHeaderWithExactMatch(zipModel, fileName);

      if (fileHeader == null) {
        fileName = fileName.replaceAll("/", "\\\\");
        fileHeader = getFileHeaderWithExactMatch(zipModel, fileName);
      }
    }

    return fileHeader;
  }

  public static int getIndexOfFileHeader(ZipModel zipModel, FileHeader fileHeader) throws ZipException {

    if (zipModel == null || fileHeader == null) {
      throw new ZipException("input parameters is null, cannot determine index of file header");
    }

    if (zipModel.getCentralDirectory() == null
        || zipModel.getCentralDirectory().getFileHeaders() == null
        || zipModel.getCentralDirectory().getFileHeaders().size() <= 0) {
      return -1;
    }

    String fileName = fileHeader.getFileName();

    if (!isStringNotNullAndNotEmpty(fileName)) {
      throw new ZipException("file name in file header is empty or null, cannot determine index of file header");
    }

    List<FileHeader> fileHeadersFromCentralDir = zipModel.getCentralDirectory().getFileHeaders();
    for (int i = 0; i < fileHeadersFromCentralDir.size(); i++) {
      FileHeader fileHeaderFromCentralDir = fileHeadersFromCentralDir.get(i);
      String fileNameForHdr = fileHeaderFromCentralDir.getFileName();
      if (!isStringNotNullAndNotEmpty(fileNameForHdr)) {
        continue;
      }

      if (fileName.equalsIgnoreCase(fileNameForHdr)) {
        return i;
      }
    }
    return -1;
  }

  public static String decodeStringWithCharset(byte[] data, boolean isUtf8Encoded) {
    if (isUtf8Encoded) {
      return new String(data, StandardCharsets.UTF_8);
    }

    try {
      return new String(data, ZIP_STANDARD_CHARSET);
    } catch (UnsupportedEncodingException e) {
      return new String(data);
    }
  }

  private static FileHeader getFileHeaderWithExactMatch(ZipModel zipModel, String fileName) throws ZipException {
    if (zipModel == null) {
      throw new ZipException("zip model is null, cannot determine file header with exact match for fileName: "
          + fileName);
    }

    if (!isStringNotNullAndNotEmpty(fileName)) {
      throw new ZipException("file name is null, cannot determine file header with exact match for fileName: "
          + fileName);
    }

    if (zipModel.getCentralDirectory() == null) {
      throw new ZipException("central directory is null, cannot determine file header with exact match for fileName: "
          + fileName);
    }

    if (zipModel.getCentralDirectory().getFileHeaders() == null) {
      throw new ZipException("file Headers are null, cannot determine file header with exact match for fileName: "
          + fileName);
    }

    if (zipModel.getCentralDirectory().getFileHeaders().size() == 0) {
      return null;
    }

    for (FileHeader fileHeader : zipModel.getCentralDirectory().getFileHeaders()) {
      String fileNameForHdr = fileHeader.getFileName();
      if (!isStringNotNullAndNotEmpty(fileNameForHdr)) {
        continue;
      }

      if (fileName.equalsIgnoreCase(fileNameForHdr)) {
        return fileHeader;
      }
    }

    return null;
  }
}
