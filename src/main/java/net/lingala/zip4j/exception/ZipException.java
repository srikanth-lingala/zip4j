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

public class ZipException extends Exception {

  private static final long serialVersionUID = 1L;

  private ZipExceptionType exceptionType;

  public ZipException(String message) {
    super(message);
  }

  public ZipException(Exception rootException) {
    super(rootException);
  }

  public ZipException(String message, Exception rootException) {
    super(message, rootException);
  }

  public ZipException(String message, ZipExceptionType exceptionType) {
    super(message);
    this.exceptionType = exceptionType;
  }

  public ZipExceptionType getExceptionType() {
    return exceptionType;
  }
}
