package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.model.FileHeader;

import java.io.IOException;
import java.io.InputStream;

// Even though this abstract class has only abstract method definitions, it is not implemented as an interface because
// implementations of this class has to be used as an inputstream to ZipInputStream
public abstract class SplitFileInputStream extends InputStream {

  public abstract void prepareExtractionForFileHeader(FileHeader fileHeader) throws IOException;
}
