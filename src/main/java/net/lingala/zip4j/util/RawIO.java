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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class RawIO {

  private byte[] shortBuff = new byte[2];
  private byte[] intBuff = new byte[4];
  private byte[] longBuff = new byte[8];

  public long readLongLittleEndian(RandomAccessFile randomAccessFile) throws IOException {
    randomAccessFile.readFully(longBuff);
    return readLongLittleEndian(longBuff, 0);
  }

  public long readLongLittleEndian(RandomAccessFile randomAccessFile, int readLen) throws IOException {
    resetBytes(longBuff);
    randomAccessFile.readFully(longBuff, 0, readLen);
    return readLongLittleEndian(longBuff, 0);
  }

  public long readLongLittleEndian(InputStream inputStream) throws IOException {
    readFully(inputStream, longBuff, longBuff.length);
    return readLongLittleEndian(longBuff, 0);
  }

  public long readLongLittleEndian(InputStream inputStream, int readLen) throws IOException {
    resetBytes(longBuff);
    readFully(inputStream, longBuff, readLen);
    return readLongLittleEndian(longBuff, 0);
  }

  public long readLongLittleEndian(byte[] array, int pos) {
    if (array.length < 8) {
      resetBytes(longBuff);
    }
    System.arraycopy(array, pos, longBuff, 0, array.length < 8 ? array.length : 8);

    long temp = 0;
    temp |= longBuff[7] & 0xff;
    temp <<= 8;
    temp |= longBuff[6] & 0xff;
    temp <<= 8;
    temp |= longBuff[5] & 0xff;
    temp <<= 8;
    temp |= longBuff[4] & 0xff;
    temp <<= 8;
    temp |= longBuff[3] & 0xff;
    temp <<= 8;
    temp |= longBuff[2] & 0xff;
    temp <<= 8;
    temp |= longBuff[1] & 0xff;
    temp <<= 8;
    temp |= longBuff[0] & 0xff;
    return temp;
  }

  public int readIntLittleEndian(RandomAccessFile randomAccessFile) throws IOException {
    randomAccessFile.readFully(intBuff);
    return readIntLittleEndian(intBuff);
  }

  public int readIntLittleEndian(InputStream inputStream) throws IOException {
    readFully(inputStream, intBuff, 4);
    return readIntLittleEndian(intBuff);
  }

  public int readIntLittleEndian(byte[] b) {
    return readIntLittleEndian(b, 0);
  }

  public int readIntLittleEndian(byte[] b, int pos) {
    return ((b[pos] & 0xff) | (b[1 + pos] & 0xff) << 8)
        | ((b[2 + pos] & 0xff) | (b[3 + pos] & 0xff) << 8) << 16;
  }

  public int readShortLittleEndian(RandomAccessFile randomAccessFile) throws IOException {
    randomAccessFile.readFully(shortBuff);
    return readShortLittleEndian(shortBuff, 0);
  }

  public int readShortLittleEndian(InputStream inputStream) throws IOException {
    readFully(inputStream, shortBuff, shortBuff.length);
    return readShortLittleEndian(shortBuff, 0);
  }

  public int readShortLittleEndian(byte[] buff, int position) {
    return (buff[position] & 0xff) | (buff[1 + position] & 0xff) << 8;
  }

  public void writeShortLittleEndian(OutputStream outputStream, int value) throws IOException {
    writeShortLittleEndian(shortBuff, 0, value);
    outputStream.write(shortBuff);
  }

  public void writeShortLittleEndian(byte[] array, int pos, int value) {
    array[pos + 1] = (byte) (value >>> 8);
    array[pos] = (byte) (value & 0xFF);

  }

  public void writeIntLittleEndian(OutputStream outputStream, int value) throws IOException {
    writeIntLittleEndian(intBuff, 0, value);
    outputStream.write(intBuff);
  }

  public void writeIntLittleEndian(byte[] array, int pos, int value) {
    array[pos + 3] = (byte) (value >>> 24);
    array[pos + 2] = (byte) (value >>> 16);
    array[pos + 1] = (byte) (value >>> 8);
    array[pos] = (byte) (value & 0xFF);

  }

  public void writeLongLittleEndian(OutputStream outputStream, long value) throws IOException {
    writeLongLittleEndian(longBuff, 0, value);
    outputStream.write(longBuff);
  }

  public void writeLongLittleEndian(byte[] array, int pos, long value) {
    array[pos + 7] = (byte) (value >>> 56);
    array[pos + 6] = (byte) (value >>> 48);
    array[pos + 5] = (byte) (value >>> 40);
    array[pos + 4] = (byte) (value >>> 32);
    array[pos + 3] = (byte) (value >>> 24);
    array[pos + 2] = (byte) (value >>> 16);
    array[pos + 1] = (byte) (value >>> 8);
    array[pos] = (byte) (value & 0xFF);
  }

  private void readFully(InputStream inputStream, byte[] buff, int readLen) throws IOException {
    int actualReadLength = Zip4jUtil.readFully(inputStream, buff, 0, readLen);
    if (actualReadLength != readLen) {
      throw new ZipException("Could not fill buffer");
    }
  }

  private void resetBytes(byte[] b) {
    for(int i = 0; i < b.length; i++) {
      b[i] = 0;
    }
  }
}
