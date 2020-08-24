package net.lingala.zip4j.util;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ExcludeFileFilter;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.GROUP_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static net.lingala.zip4j.util.BitUtils.isBitSet;
import static net.lingala.zip4j.util.InternalZipConstants.BUFF_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.FILE_SEPARATOR;
import static net.lingala.zip4j.util.InternalZipConstants.ZIP_FILE_SEPARATOR;
import static net.lingala.zip4j.util.Zip4jUtil.isStringNotNullAndNotEmpty;

public class FileUtils {

  public static final byte[] DEFAULT_POSIX_FILE_ATTRIBUTES = new byte[] {0, 0, -128, -127}; //-rw-------
  public static final byte[] DEFAULT_POSIX_FOLDER_ATTRIBUTES = new byte[] {0, 0, -128, 65}; //drw-------

  public static void setFileAttributes(Path file, byte[] fileAttributes) {
    if (fileAttributes == null || fileAttributes.length == 0) {
      return;
    }

    if (isWindows()) {
      applyWindowsFileAttributes(file, fileAttributes);
    } else if (isMac() || isUnix()) {
      applyPosixFileAttributes(file, fileAttributes);
    }
  }

  public static void setFileLastModifiedTime(Path file, long lastModifiedTime) {
    if (lastModifiedTime <= 0 || !Files.exists(file)) {
      return;
    }

    try {
      Files.setLastModifiedTime(file, FileTime.fromMillis(Zip4jUtil.dosToExtendedEpochTme(lastModifiedTime)));
    } catch (Exception e) {
      // Ignore
    }
  }

  public static void setFileLastModifiedTimeWithoutNio(File file, long lastModifiedTime) {
    file.setLastModified(Zip4jUtil.dosToExtendedEpochTme(lastModifiedTime));
  }

  public static byte[] getFileAttributes(File file) {
    try {
      if (file == null || (!Files.isSymbolicLink(file.toPath()) && !file.exists())) {
        return new byte[4];
      }

      Path path = file.toPath();

      if (isWindows()) {
        return getWindowsFileAttributes(path);
      } else if (isMac() || isUnix()) {
        return getPosixFileAttributes(path);
      } else {
        return new byte[4];
      }
    } catch (NoSuchMethodError e) {
      return new byte[4];
    }
  }

  public static List<File> getFilesInDirectoryRecursive(File path, boolean readHiddenFiles, boolean readHiddenFolders) throws ZipException {
    return getFilesInDirectoryRecursive(path, readHiddenFiles, readHiddenFolders, null);
  }

  public static List<File> getFilesInDirectoryRecursive(File path, boolean readHiddenFiles, boolean readHiddenFolders, ExcludeFileFilter excludedFiles)
      throws ZipException {

    if (path == null) {
      throw new ZipException("input path is null, cannot read files in the directory");
    }

    List<File> result = new ArrayList<>();
    File[] filesAndDirs = path.listFiles();

    if (!path.isDirectory() || !path.canRead() || filesAndDirs == null) {
      return result;
    }

    for (File file : filesAndDirs) {
      if (excludedFiles != null && excludedFiles.isExcluded(file)) {
        continue;
      }

      if (file.isHidden()) {
        if (file.isDirectory()) {
          if (!readHiddenFolders) {
            continue;
          }
        } else if (!readHiddenFiles) {
          continue;
        }
      }
      result.add(file);
      if (file.isDirectory()) {
        result.addAll(getFilesInDirectoryRecursive(file, readHiddenFiles, readHiddenFolders, excludedFiles));
      }
    }

    return result;
  }

  public static String getFileNameWithoutExtension(String fileName) {
    int pos = fileName.lastIndexOf(".");
    if (pos == -1) {
      return fileName;
    }

    return fileName.substring(0, pos);
  }

  public static String getZipFileNameWithoutExtension(String zipFile) throws ZipException {
    if (!isStringNotNullAndNotEmpty(zipFile)) {
      throw new ZipException("zip file name is empty or null, cannot determine zip file name");
    }
    String tmpFileName = zipFile;
    if (zipFile.contains(System.getProperty("file.separator"))) {
      tmpFileName = zipFile.substring(zipFile.lastIndexOf(System.getProperty("file.separator")) + 1);
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
          if (i >= 9) {
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

  public static String getRelativeFileName(File fileToAdd, ZipParameters zipParameters) throws ZipException {

    String fileName;
    try {
      String fileCanonicalPath = fileToAdd.getCanonicalPath();
      if (isStringNotNullAndNotEmpty(zipParameters.getDefaultFolderPath())) {
        File rootFolderFile = new File(zipParameters.getDefaultFolderPath());
        String rootFolderFileRef = rootFolderFile.getCanonicalPath();

        if (!rootFolderFileRef.endsWith(FILE_SEPARATOR)) {
          rootFolderFileRef += FILE_SEPARATOR;
        }

        String tmpFileName;

        if (isSymbolicLink(fileToAdd)) {
          String rootPath = new File(fileToAdd.getParentFile().getCanonicalFile().getPath() + File.separator + fileToAdd.getCanonicalFile().getName()).getPath();
          tmpFileName = rootPath.substring(rootFolderFileRef.length());
        } else {
           tmpFileName = fileCanonicalPath.substring(rootFolderFileRef.length());
        }

        if (tmpFileName.startsWith(System.getProperty("file.separator"))) {
          tmpFileName = tmpFileName.substring(1);
        }

        File tmpFile = new File(fileCanonicalPath);

        if (tmpFile.isDirectory()) {
          tmpFileName = tmpFileName.replaceAll("\\\\", "/");
          tmpFileName += ZIP_FILE_SEPARATOR;
        } else {
          String bkFileName = tmpFileName.substring(0, tmpFileName.lastIndexOf(tmpFile.getName()));
          bkFileName = bkFileName.replaceAll("\\\\", "/");
          tmpFileName = bkFileName + getNameOfFileInZip(tmpFile, zipParameters.getFileNameInZip());
        }

        fileName = tmpFileName;
      } else {
        File relFile = new File(fileCanonicalPath);
        fileName = getNameOfFileInZip(relFile, zipParameters.getFileNameInZip());
        if (relFile.isDirectory()) {
          fileName += ZIP_FILE_SEPARATOR;
        }
      }
    } catch (IOException e) {
      throw new ZipException(e);
    }

    String rootFolderNameInZip = zipParameters.getRootFolderNameInZip();
    if (Zip4jUtil.isStringNotNullAndNotEmpty(rootFolderNameInZip)) {
      if (!rootFolderNameInZip.endsWith("\\") && !rootFolderNameInZip.endsWith("/")) {
        rootFolderNameInZip = rootFolderNameInZip + InternalZipConstants.FILE_SEPARATOR;
      }

      rootFolderNameInZip = rootFolderNameInZip.replaceAll("\\\\", ZIP_FILE_SEPARATOR);
      fileName = rootFolderNameInZip + fileName;
    }

    return fileName;
  }

  private static String getNameOfFileInZip(File fileToAdd, String fileNameInZip) throws IOException {
    if (isStringNotNullAndNotEmpty(fileNameInZip)) {
      return fileNameInZip;
    }

    if (isSymbolicLink(fileToAdd)) {
      return fileToAdd.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).getFileName().toString();
    }

    return fileToAdd.getName();
  }

  public static boolean isZipEntryDirectory(String fileNameInZip) {
    return fileNameInZip.endsWith("/") || fileNameInZip.endsWith("\\");
  }

  public static void copyFile(RandomAccessFile randomAccessFile, OutputStream outputStream, long start, long end,
                              ProgressMonitor progressMonitor) throws ZipException {

    if (start < 0 || end < 0 || start > end) {
      throw new ZipException("invalid offsets");
    }

    if (start == end) {
      return;
    }

    try {
      randomAccessFile.seek(start);

      int readLen;
      byte[] buff;
      long bytesRead = 0;
      long bytesToRead = end - start;

      if ((end - start) < BUFF_SIZE) {
        buff = new byte[(int) bytesToRead];
      } else {
        buff = new byte[BUFF_SIZE];
      }

      while ((readLen = randomAccessFile.read(buff)) != -1) {
        outputStream.write(buff, 0, readLen);

        progressMonitor.updateWorkCompleted(readLen);
        if (progressMonitor.isCancelAllTasks()) {
          progressMonitor.setResult(ProgressMonitor.Result.CANCELLED);
          return;
        }

        bytesRead += readLen;

        if (bytesRead == bytesToRead) {
          break;
        } else if (bytesRead + buff.length > bytesToRead) {
          buff = new byte[(int) (bytesToRead - bytesRead)];
        }
      }

    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  public static void assertFilesExist(List<File> files, ZipParameters.SymbolicLinkAction symLinkAction) throws ZipException {
    for (File file : files) {
      if (isSymbolicLink(file)) {
        // If symlink is INCLUDE_LINK_ONLY, and if the above condition is true, it means that the link exists and there
        // will be no need to check for link existence explicitly, check only for target file existence if required
        if (symLinkAction.equals(ZipParameters.SymbolicLinkAction.INCLUDE_LINK_AND_LINKED_FILE)
            || symLinkAction.equals(ZipParameters.SymbolicLinkAction.INCLUDE_LINKED_FILE_ONLY)) {
          assertSymbolicLinkTargetExists(file);
        }
      } else {
        assertFileExists(file);
      }
    }
  }

  public static boolean isNumberedSplitFile(File file) {
    return file.getName().endsWith(InternalZipConstants.SEVEN_ZIP_SPLIT_FILE_EXTENSION_PATTERN);
  }

  public static String getFileExtension(File file) {
    String fileName = file.getName();

    if (!fileName.contains(".")) {
      return "";
    }

    return fileName.substring(fileName.lastIndexOf(".") + 1);
  }

  /**
   * A helper method to retrieve all split files which are of the format split by 7-zip, i.e, .zip.001, .zip.002, etc.
   * This method also sorts all the files by their split part
   * @param firstNumberedFile - first split file
   * @return sorted list of split files. Returns an empty list if no files of that pattern are found in the current directory
   */
  public static File[] getAllSortedNumberedSplitFiles(File firstNumberedFile) {
    String zipFileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(firstNumberedFile.getName());
    File[] allSplitFiles = firstNumberedFile.getParentFile().listFiles((dir, name) -> name.startsWith(zipFileNameWithoutExtension + "."));

    if(allSplitFiles == null) {
      return new File[0];
    }

    Arrays.sort(allSplitFiles);

    return allSplitFiles;
  }

  public static String getNextNumberedSplitFileCounterAsExtension(int index) {
    return "." + getExtensionZerosPrefix(index) + (index + 1);
  }

  public static boolean isSymbolicLink(File file) {
    try {
      return Files.isSymbolicLink(file.toPath());
    } catch (Exception | Error e) {
      return false;
    }
  }

  public static String readSymbolicLink(File file) {
    try {
      return Files.readSymbolicLink(file.toPath()).toString();
    } catch (Exception | Error e) {
      return "";
    }
  }

  public static byte[] getDefaultFileAttributes(boolean isDirectory) {
    byte[] permissions = new byte[4];
    if (isUnix() || isMac()) {
      if (isDirectory) {
        System.arraycopy(DEFAULT_POSIX_FOLDER_ATTRIBUTES, 0, permissions, 0, permissions.length);
      } else {
        System.arraycopy(DEFAULT_POSIX_FILE_ATTRIBUTES, 0, permissions, 0, permissions.length);
      }
    }
    return permissions;
  }

  public static boolean isWindows() {
    String os = System.getProperty("os.name").toLowerCase();
    return (os.contains("win"));
  }

  public static boolean isMac() {
    String os = System.getProperty("os.name").toLowerCase();
    return (os.contains("mac"));
  }

  public static boolean isUnix() {
    String os = System.getProperty("os.name").toLowerCase();
    return (os.contains("nux"));
  }

  private static String getExtensionZerosPrefix(int index) {
    if (index < 9) {
      return "00";
    } else if (index < 99) {
      return "0";
    } else {
      return "";
    }
  }

  private static void applyWindowsFileAttributes(Path file, byte[] fileAttributes) {
    if (fileAttributes[0] == 0) {
      // No file attributes defined in the archive
      return;
    }

    DosFileAttributeView fileAttributeView = Files.getFileAttributeView(file, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
    try {
      fileAttributeView.setReadOnly(isBitSet(fileAttributes[0], 0));
      fileAttributeView.setHidden(isBitSet(fileAttributes[0], 1));
      fileAttributeView.setSystem(isBitSet(fileAttributes[0], 2));
      fileAttributeView.setArchive(isBitSet(fileAttributes[0], 5));
    } catch (IOException e) {
      //Ignore
    }
  }

  private static void applyPosixFileAttributes(Path file, byte[] fileAttributes) {
    if (fileAttributes[2] == 0 && fileAttributes[3] == 0) {
      // No file attributes defined
      return;
    }

    try {
      Set<PosixFilePermission> posixFilePermissions = new HashSet<>();
      addIfBitSet(fileAttributes[3], 0, posixFilePermissions, PosixFilePermission.OWNER_READ);
      addIfBitSet(fileAttributes[2], 7, posixFilePermissions, PosixFilePermission.OWNER_WRITE);
      addIfBitSet(fileAttributes[2], 6, posixFilePermissions, PosixFilePermission.OWNER_EXECUTE);
      addIfBitSet(fileAttributes[2], 5, posixFilePermissions, PosixFilePermission.GROUP_READ);
      addIfBitSet(fileAttributes[2], 4, posixFilePermissions, PosixFilePermission.GROUP_WRITE);
      addIfBitSet(fileAttributes[2], 3, posixFilePermissions, PosixFilePermission.GROUP_EXECUTE);
      addIfBitSet(fileAttributes[2], 2, posixFilePermissions, PosixFilePermission.OTHERS_READ);
      addIfBitSet(fileAttributes[2], 1, posixFilePermissions, PosixFilePermission.OTHERS_WRITE);
      addIfBitSet(fileAttributes[2], 0, posixFilePermissions, PosixFilePermission.OTHERS_EXECUTE);
      Files.setPosixFilePermissions(file, posixFilePermissions);
    } catch (IOException e) {
      // Ignore
    }
  }

  private static byte[] getWindowsFileAttributes(Path file) {
    byte[] fileAttributes = new byte[4];

    try {
      DosFileAttributeView dosFileAttributeView = Files.getFileAttributeView(file, DosFileAttributeView.class,
          LinkOption.NOFOLLOW_LINKS);
      DosFileAttributes dosFileAttributes = dosFileAttributeView.readAttributes();

      byte windowsAttribute = 0;

      windowsAttribute = setBitIfApplicable(dosFileAttributes.isReadOnly(), windowsAttribute, 0);
      windowsAttribute = setBitIfApplicable(dosFileAttributes.isHidden(), windowsAttribute, 1);
      windowsAttribute = setBitIfApplicable(dosFileAttributes.isSystem(), windowsAttribute, 2);
      windowsAttribute = setBitIfApplicable(dosFileAttributes.isArchive(), windowsAttribute, 5);
      fileAttributes[0] = windowsAttribute;
    } catch (IOException e) {
      // ignore
    }

    return fileAttributes;
  }

  private static void assertFileExists(File file) throws ZipException {
    if (!file.exists()) {
      throw new ZipException("File does not exist: " + file);
    }
  }

  private static void assertSymbolicLinkTargetExists(File file) throws ZipException {
    if (!file.exists()) {
      throw new ZipException("Symlink target '" + readSymbolicLink(file) + "' does not exist for link '" + file + "'");
    }
  }

  private static byte[] getPosixFileAttributes(Path file) {
    byte[] fileAttributes = new byte[4];

    try {
      PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(file, PosixFileAttributeView.class,
          LinkOption.NOFOLLOW_LINKS);
      Set<PosixFilePermission> posixFilePermissions = posixFileAttributeView.readAttributes().permissions();

      fileAttributes[3] = setBitIfApplicable(Files.isRegularFile(file), fileAttributes[3], 7);
      fileAttributes[3] = setBitIfApplicable(Files.isDirectory(file), fileAttributes[3], 6);
      fileAttributes[3] = setBitIfApplicable(Files.isSymbolicLink(file), fileAttributes[3], 5);
      fileAttributes[3] = setBitIfApplicable(posixFilePermissions.contains(OWNER_READ), fileAttributes[3], 0);
      fileAttributes[2] = setBitIfApplicable(posixFilePermissions.contains(OWNER_WRITE), fileAttributes[2], 7);
      fileAttributes[2] = setBitIfApplicable(posixFilePermissions.contains(OWNER_EXECUTE), fileAttributes[2], 6);
      fileAttributes[2] = setBitIfApplicable(posixFilePermissions.contains(GROUP_READ), fileAttributes[2], 5);
      fileAttributes[2] = setBitIfApplicable(posixFilePermissions.contains(GROUP_WRITE), fileAttributes[2], 4);
      fileAttributes[2] = setBitIfApplicable(posixFilePermissions.contains(GROUP_EXECUTE), fileAttributes[2], 3);
      fileAttributes[2] = setBitIfApplicable(posixFilePermissions.contains(OTHERS_READ), fileAttributes[2], 2);
      fileAttributes[2] = setBitIfApplicable(posixFilePermissions.contains(OTHERS_WRITE), fileAttributes[2], 1);
      fileAttributes[2] = setBitIfApplicable(posixFilePermissions.contains(OTHERS_EXECUTE), fileAttributes[2], 0);
    } catch (IOException e) {
      // Ignore
    }

    return fileAttributes;
  }

  private static byte setBitIfApplicable(boolean applicable, byte b, int pos) {
    if (applicable) {
      b = BitUtils.setBit(b, pos);
    }

    return b;
  }

  private static void addIfBitSet(byte b, int pos, Set<PosixFilePermission> posixFilePermissions,
                                  PosixFilePermission posixFilePermissionToAdd) {
    if (isBitSet(b, pos)) {
      posixFilePermissions.add(posixFilePermissionToAdd);
    }
  }
}
