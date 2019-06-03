package net.lingala.zip4j.util;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
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

public class FileUtils {

  private static final String OS = System.getProperty("os.name").toLowerCase();

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

  public static void setFileLastModifiedTime(Path file, FileHeader fileHeader) {
    if (fileHeader.getLastModifiedTime() <= 0) {
      return;
    }

    if (Files.exists(file)) {
      try {
        Files.setLastModifiedTime(file, FileTime.fromMillis(Zip4jUtil.dosToJavaTme(fileHeader.getLastModifiedTime())));
      } catch (IOException e) {
        // Ignore
      }
    }
  }

  public static byte[] getFileAttributes(Path file) {
    if (file == null || !Files.exists(file)) {
      return new byte[4];
    }

    if (isWindows()) {
      return getWindowsFileAttributes(file);
    } else if (isMac() || isUnix()) {
      return getPosixFileAttributes(file);
    } else {
      return new byte[4];
    }
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

  private static void applyWindowsFileAttributes(Path file, byte[] fileAttributes) {
    if (fileAttributes[0] == 0) {
      // No file attributes defined in the archive
      return;
    }

    DosFileAttributeView fileAttributeView = Files.getFileAttributeView(file, DosFileAttributeView.class);
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
      addIfBitSet(fileAttributes[3], 0, posixFilePermissions, OWNER_READ);
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
      DosFileAttributeView dosFileAttributeView = Files.getFileAttributeView(file, DosFileAttributeView.class);
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

  private static byte[] getPosixFileAttributes(Path file) {
    byte[] fileAttributes = new byte[4];

    try {
      PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(file, PosixFileAttributeView.class);
      Set<PosixFilePermission> posixFilePermissions = posixFileAttributeView.readAttributes().permissions();

      fileAttributes[3] = setBitIfApplicable(Files.isRegularFile(file), fileAttributes[3], 7);
      fileAttributes[3] = setBitIfApplicable(Files.isDirectory(file), fileAttributes[3], 6);
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
      b = BitUtils.setBitOfByte(b, pos);
    }

    return b;
  }

  private static void addIfBitSet(byte b, int pos, Set<PosixFilePermission> posixFilePermissions,
                                  PosixFilePermission posixFilePermissionToAdd) {
    if (isBitSet(b, pos)) {
      posixFilePermissions.add(posixFilePermissionToAdd);
    }
  }

  private static boolean isWindows() {
    return (OS.contains("win"));
  }

  private static boolean isMac() {
    return (OS.contains("mac"));
  }

  private static boolean isUnix() {
    return (OS.contains("nux"));
  }

}
