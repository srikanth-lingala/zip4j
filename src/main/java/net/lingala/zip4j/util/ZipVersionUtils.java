package net.lingala.zip4j.util;

import net.lingala.zip4j.headers.VersionMadeBy;
import net.lingala.zip4j.headers.VersionNeededToExtract;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

public class ZipVersionUtils {

  public static int determineVersionMadeBy(ZipParameters zipParameters, RawIO rawIO) {
    byte[] versionMadeBy = new byte[2];
    versionMadeBy[0] = VersionMadeBy.SPECIFICATION_VERSION.getCode();
    versionMadeBy[1] = VersionMadeBy.UNIX.getCode();
    if (FileUtils.isWindows() && !zipParameters.isUnixMode()) {  // skip setting windows mode if unix mode is forced
      versionMadeBy[1] = VersionMadeBy.WINDOWS.getCode();
    }

    return rawIO.readShortLittleEndian(versionMadeBy, 0);
  }

  public static VersionNeededToExtract determineVersionNeededToExtract(ZipParameters zipParameters) {
    VersionNeededToExtract versionRequired = VersionNeededToExtract.DEFAULT;

    if (zipParameters.getCompressionMethod() == CompressionMethod.DEFLATE) {
      versionRequired = VersionNeededToExtract.DEFLATE_COMPRESSED;
    }

    if (zipParameters.getEntrySize() > InternalZipConstants.ZIP_64_SIZE_LIMIT) {
      versionRequired = VersionNeededToExtract.ZIP_64_FORMAT;
    }

    if (zipParameters.isEncryptFiles() && zipParameters.getEncryptionMethod().equals(EncryptionMethod.AES)) {
      versionRequired = VersionNeededToExtract.AES_ENCRYPTED;
    }

    return versionRequired;
  }
}
