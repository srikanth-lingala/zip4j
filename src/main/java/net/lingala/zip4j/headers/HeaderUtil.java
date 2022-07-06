package net.lingala.zip4j.headers;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.InternalZipConstants;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static net.lingala.zip4j.util.InternalZipConstants.ZIP4J_DEFAULT_CHARSET;
import static net.lingala.zip4j.util.InternalZipConstants.ZIP_STANDARD_CHARSET_NAME;
import static net.lingala.zip4j.util.Zip4jUtil.isStringNotNullAndNotEmpty;

public class HeaderUtil {

  public static FileHeader getFileHeader(ZipModel zipModel, String fileName) throws ZipException {
    FileHeader fileHeader = getFileHeaderWithExactMatch(zipModel, fileName);

    String backslashReplaced;
    if (fileHeader == null && !(backslashReplaced = fileName.replaceAll("\\\\", "/")).equals(fileName)) {
      fileHeader = getFileHeaderWithExactMatch(zipModel, backslashReplaced);

      String slashesReplaced;
      if (fileHeader == null && !(slashesReplaced = backslashReplaced.replaceAll("/", "\\\\")).equals(backslashReplaced)) {
        fileHeader = getFileHeaderWithExactMatch(zipModel, slashesReplaced);
      }
    }

    return fileHeader;
  }

  public static String decodeStringWithCharset(byte[] data, boolean isUtf8Encoded, Charset charset) {
    if (charset != null) {
      return new String(data, charset);
    }

    if (isUtf8Encoded) {
      return new String(data, InternalZipConstants.CHARSET_UTF_8);
    }

    try {
      return new String(data, ZIP_STANDARD_CHARSET_NAME);
    } catch (UnsupportedEncodingException e) {
      return new String(data);
    }
  }

  public static byte[] getBytesFromString(String string, Charset charset) {
    if (charset == null) {
      return string.getBytes(ZIP4J_DEFAULT_CHARSET);
    }

    return string.getBytes(charset);
  }

  public static long getOffsetStartOfCentralDirectory(ZipModel zipModel) {
    if (zipModel.isZip64Format()) {
      return zipModel.getZip64EndOfCentralDirectoryRecord().getOffsetStartCentralDirectoryWRTStartDiskNumber();
    }

    return zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory();
  }

  public static List<FileHeader> getFileHeadersUnderDirectory(List<FileHeader> allFileHeaders, String fileName) {
    List<FileHeader> fileHeadersUnderDirectory = new ArrayList<>();
    for (FileHeader fileHeader : allFileHeaders) {
      if (fileHeader.getFileName().startsWith(fileName)) {
        fileHeadersUnderDirectory.add(fileHeader);
      }
    }
    return fileHeadersUnderDirectory;
  }

  public static long getTotalUncompressedSizeOfAllFileHeaders(List<FileHeader> fileHeaders) {
    long totalUncompressedSize = 0;
    for (FileHeader fileHeader : fileHeaders) {
      if (fileHeader.getZip64ExtendedInfo() != null &&
          fileHeader.getZip64ExtendedInfo().getUncompressedSize() > 0) {
        totalUncompressedSize += fileHeader.getZip64ExtendedInfo().getUncompressedSize();
      } else {
        totalUncompressedSize += fileHeader.getUncompressedSize();
      }
    }
    return totalUncompressedSize;
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

    if (zipModel.getCentralDirectory().getFileNameHeaderMap().isEmpty()) {
      return null;
    }

    return zipModel.getCentralDirectory().getFileNameHeaderMap().get(fileName);
  }
}
