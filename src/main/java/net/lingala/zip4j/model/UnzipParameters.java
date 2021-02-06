package net.lingala.zip4j.model;

public class UnzipParameters {

  private boolean extractSymbolicLinks = true;

  public boolean isExtractSymbolicLinks() {
    return extractSymbolicLinks;
  }

  public void setExtractSymbolicLinks(boolean extractSymbolicLinks) {
    this.extractSymbolicLinks = extractSymbolicLinks;
  }
}
