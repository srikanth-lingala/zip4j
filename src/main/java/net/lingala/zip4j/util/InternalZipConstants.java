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

import java.io.File;
import java.nio.charset.Charset;

public final class InternalZipConstants {

  private InternalZipConstants() {

  }

  public static final int ENDHDR = 22;	// END header size
  public static final int STD_DEC_HDR_SIZE = 12;

  //AES Constants
  public static final int AES_AUTH_LENGTH = 10;
  public static final int AES_BLOCK_SIZE = 16;
  public static final int AES_EXTRA_DATA_RECORD_SIZE = 11;

  public static final int MIN_SPLIT_LENGTH = 65536;
  public static final long ZIP_64_SIZE_LIMIT = 4294967295L;
  public static final int ZIP_64_NUMBER_OF_ENTRIES_LIMIT = 65535;

  public static final int BUFF_SIZE = 1024 * 4;

  // Update local file header constants
  // This value holds the number of bytes to skip from
  // the offset of start of local header
  public static final int UPDATE_LFH_CRC = 14;

  public static final int UPDATE_LFH_COMP_SIZE = 18;

  public static final int UPDATE_LFH_UNCOMP_SIZE = 22;

  public static final String ZIP_STANDARD_CHARSET = "Cp437";

  public static final String FILE_SEPARATOR = File.separator;

  public static final String ZIP_FILE_SEPARATOR = "/";

  public static final int MAX_ALLOWED_ZIP_COMMENT_LENGTH = 0xFFFF;

  public static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");

  public static final String SEVEN_ZIP_SPLIT_FILE_EXTENSION_PATTERN = ".zip.001";
}
