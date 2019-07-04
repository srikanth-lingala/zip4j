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
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

public class CrcUtil {

  private static final int BUF_SIZE = 1 << 14; //16384

  public static long computeFileCrc(File inputFile, ProgressMonitor progressMonitor) throws IOException {

    if (inputFile == null || !inputFile.exists() || !inputFile.canRead()) {
      throw new ZipException("input file is null or does not exist or cannot read. " +
          "Cannot calculate CRC for the file");
    }

    byte[] buff = new byte[BUF_SIZE];
    CRC32 crc32 = new CRC32();

    try(InputStream inputStream = new FileInputStream(inputFile)) {
      int readLen;
      while ((readLen = inputStream.read(buff)) != -1) {
        crc32.update(buff, 0, readLen);

        if (progressMonitor != null) {
          progressMonitor.updateWorkCompleted(readLen);
          if (progressMonitor.isCancelAllTasks()) {
            progressMonitor.setResult(ProgressMonitor.Result.CANCELLED);
            progressMonitor.setState(ProgressMonitor.State.READY);
            return 0;
          }
        }
      }
      return crc32.getValue();
    }
  }

}
