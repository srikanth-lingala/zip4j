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

package net.lingala.zip4j.exception;

import java.io.IOException;

public class ZipException extends IOException {

  private static final long serialVersionUID = 1L;

  private Type type = Type.UNKNOWN;

  public ZipException(String message) {
    super(message);
  }

  public ZipException(Exception rootException) {
    super(rootException);
  }

  public ZipException(String message, Exception rootException) {
    super(message, rootException);
  }

  public ZipException(String message, Type type) {
    super(message);
    this.type = type;
  }

  public ZipException(String message, Throwable throwable, Type type) {
    super(message, throwable);
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  public enum Type {
    WRONG_PASSWORD,
    TASK_CANCELLED_EXCEPTION,
    CHECKSUM_MISMATCH,
    UNKNOWN_COMPRESSION_METHOD,
    FILE_NOT_FOUND,
    UNSUPPORTED_ENCRYPTION,
    UNKNOWN
  }
}
