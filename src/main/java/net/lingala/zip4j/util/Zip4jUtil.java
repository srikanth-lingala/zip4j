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

import java.io.File;
import java.util.Calendar;

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

  public static byte[] convertCharArrayToByteArray(char[] charArray) {
    byte[] bytes = new byte[charArray.length];
    for (int i = 0; i < charArray.length; i++) {
      bytes[i] = (byte) charArray[i];
    }
    return bytes;
  }

}
