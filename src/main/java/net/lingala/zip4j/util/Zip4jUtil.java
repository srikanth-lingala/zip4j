/*
 * Copyright 2010 Srikanth Reddy Lingala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lingala.zip4j.util;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static net.lingala.zip4j.util.InternalZipConstants.FILE_SEPARATOR;
import static net.lingala.zip4j.util.InternalZipConstants.ZIP_FILE_SEPARATOR;
import static net.lingala.zip4j.util.InternalZipConstants.ZIP_STANDARD_CHARSET;

public class Zip4jUtil {

  public static boolean isStringNotNullAndNotEmpty(String str) {
    return str != null && str.trim().length() > 0;
  }

  public static boolean createDirectoryIfNotExists(String path) throws ZipException {
    if (!isStringNotNullAndNotEmpty(path)) {
      throw new ZipException(new NullPointerException("output path is null"));
    }

    File file = new File(path);

    if (file.exists()) {
      if (!file.isDirectory()) {
        throw new ZipException("output directory is not valid");
      }
    } else {
      if (!file.mkdirs()) {
        throw new ZipException("Cannot create output directories");
      }
    }

    return true;
  }

  /**
   * Converts input time from Java to DOS format
   *
   * @param time
   * @return time in DOS format
   */
  public static long javaToDosTime(long time) {

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(time);

    int year = cal.get(Calendar.YEAR);
    if (year < 1980) {
      return (1 << 21) | (1 << 16);
    }
    return (year - 1980) << 25 | (cal.get(Calendar.MONTH) + 1) << 21 |
        cal.get(Calendar.DATE) << 16 | cal.get(Calendar.HOUR_OF_DAY) << 11 | cal.get(Calendar.MINUTE) << 5 |
        cal.get(Calendar.SECOND) >> 1;
  }

  /**
   * Converts time in dos format to Java format
   *
   * @param dosTime
   * @return time in java format
   */
  public static long dosToJavaTme(int dosTime) {
    int sec = 2 * (dosTime & 0x1f);
    int min = (dosTime >> 5) & 0x3f;
    int hrs = (dosTime >> 11) & 0x1f;
    int day = (dosTime >> 16) & 0x1f;
    int mon = ((dosTime >> 21) & 0xf) - 1;
    int year = ((dosTime >> 25) & 0x7f) + 1980;

    Calendar cal = Calendar.getInstance();
    cal.set(year, mon, day, hrs, min, sec);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime().getTime();
  }

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

  public static int getIndexOfFileHeader(ZipModel zipModel, FileHeader fileHeader) throws ZipException {

    if (zipModel == null || fileHeader == null) {
      throw new ZipException("input parameters is null, cannot determine index of file header");
    }

    if (zipModel.getCentralDirectory() == null) {
      throw new ZipException("central directory is null, cannot determine index of file header");
    }

    if (zipModel.getCentralDirectory().getFileHeaders() == null) {
      throw new ZipException("file Headers are null, cannot determine index of file header");
    }

    if (zipModel.getCentralDirectory().getFileHeaders().size() <= 0) {
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

  public static List<File> getFilesInDirectoryRecursive(File path, boolean readHiddenFiles) throws ZipException {
    if (path == null) {
      throw new ZipException("input path is null, cannot read files in the directory");
    }

    List<File> result = new ArrayList<>();
    File[] filesAndDirs = path.listFiles();

    if (!path.isDirectory() || !path.canRead() || filesAndDirs == null) {
      return result;
    }

    for (File file : filesAndDirs) {
      if (file.isHidden() && !readHiddenFiles) {
        return result;
      }
      result.add(file);
      if (file.isDirectory()) {
        result.addAll(getFilesInDirectoryRecursive(file, readHiddenFiles));
      }
    }
    return result;
  }

  public static String getZipFileNameWithoutExt(String zipFile) throws ZipException {
    if (!isStringNotNullAndNotEmpty(zipFile)) {
      throw new ZipException("zip file name is empty or null, cannot determine zip file name");
    }
    String tmpFileName = zipFile;
    if (zipFile.contains(System.getProperty("file.separator"))) {
      tmpFileName = zipFile.substring(zipFile.lastIndexOf(System.getProperty("file.separator")));
    }

    if (tmpFileName.endsWith(".zip")) {
      tmpFileName = tmpFileName.substring(0, tmpFileName.lastIndexOf("."));
    }
    return tmpFileName;
  }

  public static List<File> getSplitZipFiles(ZipModel zipModel) throws ZipException {
    if (zipModel == null) {
      throw new ZipException("cannot get split zip files: zipmodel is null");
    }

    if (zipModel.getEndOfCentralDirectoryRecord() == null) {
      return null;
    }

    if (!zipModel.getZipFile().exists()) {
      throw new ZipException("zip file does not exist");
    }

    List<File> splitZipFiles = new ArrayList<>();
    File currZipFile = zipModel.getZipFile();
    String partFile;

    if (!zipModel.isSplitArchive()) {
      splitZipFiles.add(currZipFile);
      return splitZipFiles;
    }

    int numberOfThisDisk = zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk();

    if (numberOfThisDisk == 0) {
      splitZipFiles.add(currZipFile);
      return splitZipFiles;
    } else {
      for (int i = 0; i <= numberOfThisDisk; i++) {
        if (i == numberOfThisDisk) {
          splitZipFiles.add(zipModel.getZipFile());
        } else {
          String fileExt = ".z0";
          if (i > 9) {
            fileExt = ".z";
          }
          partFile = (currZipFile.getName().contains("."))
              ? currZipFile.getPath().substring(0, currZipFile.getPath().lastIndexOf(".")) : currZipFile.getPath();
          partFile = partFile + fileExt + (i + 1);
          splitZipFiles.add(new File(partFile));
        }
      }
    }
    return splitZipFiles;
  }

  public static String getRelativeFileName(String file, String rootFolderInZip, String rootFolderPath)
      throws ZipException {

    if (!Zip4jUtil.isStringNotNullAndNotEmpty(file)) {
      throw new ZipException("input file path/name is empty, cannot calculate relative file name");
    }

    String fileName;
    if (Zip4jUtil.isStringNotNullAndNotEmpty(rootFolderPath)) {
      File rootFolderFile = new File(rootFolderPath);
      String rootFolderFileRef = rootFolderFile.getPath();

      if (!rootFolderFileRef.endsWith(FILE_SEPARATOR)) {
        rootFolderFileRef += FILE_SEPARATOR;
      }

      String tmpFileName = file.substring(rootFolderFileRef.length());
      if (tmpFileName.startsWith(System.getProperty("file.separator"))) {
        tmpFileName = tmpFileName.substring(1);
      }

      File tmpFile = new File(file);

      if (tmpFile.isDirectory()) {
        tmpFileName = tmpFileName.replaceAll("\\\\", "/");
        tmpFileName += ZIP_FILE_SEPARATOR;
      } else {
        String bkFileName = tmpFileName.substring(0, tmpFileName.lastIndexOf(tmpFile.getName()));
        bkFileName = bkFileName.replaceAll("\\\\", "/");
        tmpFileName = bkFileName + tmpFile.getName();
      }

      fileName = tmpFileName;
    } else {
      File relFile = new File(file);
      if (relFile.isDirectory()) {
        fileName = relFile.getName() + ZIP_FILE_SEPARATOR;
      } else {
        fileName = relFile.getName();
      }
    }

    if (Zip4jUtil.isStringNotNullAndNotEmpty(rootFolderInZip)) {
      fileName = rootFolderInZip + fileName;
    }

    if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
      throw new ZipException("Error determining file name");
    }

    return fileName;
  }

  public static boolean isZipEntryDirectory(String fileNameInZip) {
    return fileNameInZip.endsWith("/") || fileNameInZip.endsWith("\\");
  }

  public static byte[] convertCharArrayToByteArray(char[] charArray) {
    byte[] bytes = new byte[charArray.length];
    for (int i = 0; i < charArray.length; i++) {
      bytes[i] = (byte) charArray[i];
    }
    return bytes;
  }

  public static String decodeFileName(byte[] data, boolean isUtf8Encoded) {
    if (isUtf8Encoded) {
      return new String(data, StandardCharsets.UTF_8);
    }

    try {
      return new String(data, ZIP_STANDARD_CHARSET);
    } catch (UnsupportedEncodingException e) {
      return new String(data);
    }
  }
}
