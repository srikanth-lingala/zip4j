package net.lingala.zip4j.model;

public class UnzipParameters {

  private boolean extractSymbolicLinks = true;

  private boolean setFileAttributes = true;

  public boolean isExtractSymbolicLinks() {
    return extractSymbolicLinks;
  }

  public void setExtractSymbolicLinks(boolean extractSymbolicLinks) {
    this.extractSymbolicLinks = extractSymbolicLinks;
  }

  public boolean setFileAttributes() {
    return setFileAttributes;
  }

  public void setSetFileAttributes(boolean setFileAttributes) {
    this.setFileAttributes = setFileAttributes;
  }
}
