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

import net.lingala.zip4j.model.enums.CompressionLevel;

import java.io.IOException;
import java.util.zip.Deflater;

import static net.lingala.zip4j.util.InternalZipConstants.BUFF_SIZE;

class DeflaterOutputStream extends CompressedOutputStream {

  private byte[] buff = new byte[BUFF_SIZE];
  protected Deflater deflater;

  public DeflaterOutputStream(CipherOutputStream cipherOutputStream, CompressionLevel compressionLevel) {
    super(cipherOutputStream);
    deflater = new Deflater(compressionLevel.getLevel(), true);
  }

  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  public void write(int bval) throws IOException {
    byte[] b = new byte[1];
    b[0] = (byte) bval;
    write(b, 0, 1);
  }

  public void write(byte[] buf, int off, int len) throws IOException {
    deflater.setInput(buf, off, len);
    while (!deflater.needsInput()) {
      deflate();
    }
  }

  private void deflate() throws IOException {
    int len = deflater.deflate(buff, 0, buff.length);
    if (len > 0) {
      super.write(buff, 0, len);
    }
  }

  public void closeEntry() throws IOException {
    if (!deflater.finished()) {
      deflater.finish();
      while (!deflater.finished()) {
        deflate();
      }
    }
    deflater.end();
    super.closeEntry();
  }
}
