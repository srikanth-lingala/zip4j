package net.lingala.zip4j.headers;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.InternalZipConstants;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
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

  public static String decodeStringWithCharset(byte[] data, boolean isUtf8Encoded, Charset charset) {
    if (InternalZipConstants.CHARSET_UTF_8.equals(charset) && !isUtf8Encoded) {
      try {
        return new String(data, ZIP_STANDARD_CHARSET);
      } catch (UnsupportedEncodingException e) {
        return new String(data);
      }
    }

    if(charset != null) {
      return new String(data, charset);
    }

    return new String(data, InternalZipConstants.CHARSET_UTF_8);
  }

  public static long getOffsetStartOfCentralDirectory(ZipModel zipModel) {
    if (zipModel.isZip64Format()) {
      return zipModel.getZip64EndOfCentralDirectoryRecord().getOffsetStartCentralDirectoryWRTStartDiskNumber();
    }

    return zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory();
  }

  public static List<FileHeader> getFileHeadersUnderDirectory(List<FileHeader> allFileHeaders, FileHeader rootFileHeader) {
    if (!rootFileHeader.isDirectory()) {
      return Collections.emptyList();
    }
    if (allFileHeaders == null || allFileHeaders.isEmpty()) {
      return Collections.emptyList();
    }
    List<FileHeader> filterFileHeaders = new ArrayList<>();
    for (FileHeader allFileHeader : allFileHeaders) {
      if (allFileHeader.getFileName().startsWith(rootFileHeader.getFileName())) {
        filterFileHeaders.add(allFileHeader);
      }
    }
    return filterFileHeaders;
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
