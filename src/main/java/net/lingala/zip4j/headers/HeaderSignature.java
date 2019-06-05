package net.lingala.zip4j.headers;

public enum HeaderSignature {

  LOCAL_FILE_HEADER(0x04034b50L),  // "PK\003\004"
  EXTRA_DATA_RECORD(0x08074b50L),  // "PK\007\008"
  CENTRAL_DIRECTORY(0x02014b50L),  // "PK\001\002"
  END_OF_CENTRAL_DIRECTORY(0x06054b50L),  // "PK\005\006"
  DIGITAL_SIGNATURE(0x05054b50L),
  ARCEXTDATREC(0x08064b50L),
  SPLIT_ZIP(0x08074b50L),
  ZIP64_END_CENTRAL_DIRECTORY_LOCATOR(0x07064b50L),
  ZIP64_END_CENTRAL_DIRECTORY_RECORD(0x06064b50),
  ZIP64_EXTRA_FIELD_SIGNATURE(0x0001),
  AES_EXTRA_DATA_RECORD(0x9901);

  private long value;

  HeaderSignature(long value) {
    this.value = value;
  }

  public long getValue() {
    return value;
  }
}
