package net.lingala.zip4j.tasks;

import net.lingala.zip4j.model.Zip4jConfig;

public abstract class AbstractZipTaskParameters {

  protected Zip4jConfig zip4jConfig;

  protected AbstractZipTaskParameters(Zip4jConfig zip4jConfig) {
    this.zip4jConfig = zip4jConfig;
  }
}
