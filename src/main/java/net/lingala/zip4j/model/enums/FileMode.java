package net.lingala.zip4j.model.enums;

public enum FileMode {

  NONE(0),
  READ_ONLY(1),
  HIDDEN(2),
  ARCHIVE(32),
  READ_ONLY_AND_HIDDEN(3),
  READ_ONLY_AND_ARCHIVE(33),
  HIDDEN_AND_ARCHIVE(34),
  READ_ONLY_AND_HIDDEN_AND_ARCHIVE(35),
  SYSTEM_FILE(38),
  FOLDER_NONE(16),
  FOLDER_HIDDEN(18),
  FOLDER_ARCHIVE(48),
  FOLDER_HIDDEN_AND_ARCHIVE(50);

  private int value;

  FileMode(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static FileMode getFileModeFromValue(int value) {
    for (FileMode fileMode : values()) {
      if (fileMode.getValue() == value) {
        return fileMode;
      }
    }

    return null;
  }
}
