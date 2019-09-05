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

package net.lingala.zip4j.io.outputstream;

import net.lingala.zip4j.crypto.Encrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import java.io.IOException;
import java.io.OutputStream;

abstract class CipherOutputStream<T extends Encrypter> extends OutputStream {

  private ZipEntryOutputStream zipEntryOutputStream;
  private T encrypter;

  public CipherOutputStream(ZipEntryOutputStream zipEntryOutputStream, ZipParameters zipParameters, char[] password)
      throws IOException, ZipException {
    this.zipEntryOutputStream = zipEntryOutputStream;
    this.encrypter = initializeEncrypter(zipEntryOutputStream, zipParameters, password);
  }

  @Override
  public void write(int b) throws IOException {
    zipEntryOutputStream.write(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    zipEntryOutputStream.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    encrypter.encryptData(b, off, len);
    zipEntryOutputStream.write(b, off, len);
  }

  public void writeHeaders(byte[] b) throws IOException {
    zipEntryOutputStream.write(b);
  }

  public void closeEntry() throws IOException {
    zipEntryOutputStream.closeEntry();
  }

  @Override
  public void close() throws IOException {
    zipEntryOutputStream.close();
  }

  public long getNumberOfBytesWrittenForThisEntry() {
    return zipEntryOutputStream.getNumberOfBytesWrittenForThisEntry();
  }

  protected T getEncrypter() {
    return encrypter;
  }

  protected abstract T initializeEncrypter(OutputStream outputStream, ZipParameters zipParameters, char[] password)
      throws IOException, ZipException;
}
