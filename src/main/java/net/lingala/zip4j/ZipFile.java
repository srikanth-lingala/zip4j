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

package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderReader;
import net.lingala.zip4j.headers.HeaderUtil;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.io.inputstream.NumberedSplitRandomAccessFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.Zip4jConfig;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.tasks.AddFilesToZipTask;
import net.lingala.zip4j.tasks.AddFilesToZipTask.AddFilesToZipTaskParameters;
import net.lingala.zip4j.tasks.AddFolderToZipTask;
import net.lingala.zip4j.tasks.AddFolderToZipTask.AddFolderToZipTaskParameters;
import net.lingala.zip4j.tasks.AddStreamToZipTask;
import net.lingala.zip4j.tasks.AddStreamToZipTask.AddStreamToZipTaskParameters;
import net.lingala.zip4j.tasks.AsyncZipTask;
import net.lingala.zip4j.tasks.ExtractAllFilesTask;
import net.lingala.zip4j.tasks.ExtractAllFilesTask.ExtractAllFilesTaskParameters;
import net.lingala.zip4j.tasks.ExtractFileTask;
import net.lingala.zip4j.tasks.ExtractFileTask.ExtractFileTaskParameters;
import net.lingala.zip4j.tasks.MergeSplitZipFileTask;
import net.lingala.zip4j.tasks.MergeSplitZipFileTask.MergeSplitZipFileTaskParameters;
import net.lingala.zip4j.tasks.RemoveFilesFromZipTask;
import net.lingala.zip4j.tasks.RemoveFilesFromZipTask.RemoveFilesFromZipTaskParameters;
import net.lingala.zip4j.tasks.RenameFilesTask;
import net.lingala.zip4j.tasks.RenameFilesTask.RenameFilesTaskParameters;
import net.lingala.zip4j.tasks.SetCommentTask;
import net.lingala.zip4j.tasks.SetCommentTask.SetCommentTaskTaskParameters;
import net.lingala.zip4j.util.FileUtils;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.RawIO;
import net.lingala.zip4j.util.Zip4jUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static net.lingala.zip4j.util.FileUtils.isNumberedSplitFile;
import static net.lingala.zip4j.util.InternalZipConstants.CHARSET_UTF_8;
import static net.lingala.zip4j.util.InternalZipConstants.MIN_BUFF_SIZE;
import static net.lingala.zip4j.util.UnzipUtil.createZipInputStream;
import static net.lingala.zip4j.util.Zip4jUtil.isStringNotNullAndNotEmpty;

/**
 * Base class to handle zip files. Some of the operations supported
 * in this class are:<br>
 * <ul>
 * <li>Create Zip File</li>
 * <li>Add files to zip file</li>
 * <li>Add folder to zip file</li>
 * <li>Extract files from zip files</li>
 * <li>Remove files from zip file</li>
 * </ul>
 */

public class ZipFile implements Closeable {

  private File zipFile;
  private ZipModel zipModel;
  private boolean isEncrypted;
  private ProgressMonitor progressMonitor;
  private boolean runInThread;
  private char[] password;
  private HeaderWriter headerWriter = new HeaderWriter();
  private Charset charset = null;
  private ThreadFactory threadFactory;
  private ExecutorService executorService;
  private int bufferSize = InternalZipConstants.BUFF_SIZE;
  private List<InputStream> openInputStreams = new ArrayList<>();
  private boolean useUtf8CharsetForPasswords = InternalZipConstants.USE_UTF8_FOR_PASSWORD_ENCODING_DECODING;

  /**
   * Creates a new ZipFile instance with the zip file at the location specified in zipFile.
   * This constructor does not yet create a zip file if it does not exist. Creation happens when adding files
   * to this ZipFile instance
   * @param zipFile
   */
  public ZipFile(String zipFile) {
    this(new File(zipFile), null);
  }

  /**
   * Creates a new ZipFile instance with the zip file at the location specified in zipFile.
   * Input password will be used for any zip operations like adding files or extracting files.
   * This constructor does not yet create a zip file if it does not exist. Creation happens when adding files
   * to this ZipFile instance
   * @param zipFile
   */
  public ZipFile(String zipFile, char[] password) {
    this(new File(zipFile), password);
  }

  /**
   * Creates a new Zip File Object with the input file.
   * If the zip file does not exist, it is not created at this point.
   *
   * @param zipFile file reference to the zip file
   * @throws IllegalArgumentException when zip file parameter is null
   */
  public ZipFile(File zipFile) {
    this(zipFile, null);
  }

  /**
   * Creates a new Zip File Object with the input file.
   * If the zip file does not exist, it is not created at this point.
   *
   * @param zipFile file reference to the zip file
   * @param password password to use for the zip file
   * @throws IllegalArgumentException when zip file parameter is null
   */
  public ZipFile(File zipFile, char[] password) {
    if (zipFile == null) {
      throw new IllegalArgumentException("input zip file parameter is null");
    }

    this.zipFile = zipFile;
    this.password = password;
    this.runInThread = false;
    this.progressMonitor = new ProgressMonitor();
  }

  /**
   * Creates a zip file and adds the list of source file(s) to the zip file. If the zip file
   * exists then this method throws an exception. Parameters such as compression type, etc
   * can be set in the input parameters. While the method addFile/addFiles also creates the
   * zip file if it does not exist, the main functionality of this method is to create a split
   * zip file. To create a split zip file, set the splitArchive parameter to true with a valid
   * splitLength. Split Length has to be more than 65536 bytes
   *
   * @param filesToAdd - File to be added to the zip file
   * @param parameters     - zip parameters for this file list
   * @param splitArchive   - if archive has to be split or not
   * @param splitLength    - if archive has to be split, then length in bytes at which it has to be split
   * @throws ZipException
   */
  public void createSplitZipFile(List<File> filesToAdd, ZipParameters parameters, boolean splitArchive,
                            long splitLength) throws ZipException {

    if (zipFile.exists()) {
      throw new ZipException("zip file: " + zipFile
          + " already exists. To add files to existing zip file use addFile method");
    }

    if (filesToAdd == null || filesToAdd.size() == 0) {
      throw new ZipException("input file List is null, cannot create zip file");
    }

    createNewZipModel();
    zipModel.setSplitArchive(splitArchive);
    zipModel.setSplitLength(splitLength);

    new AddFilesToZipTask(zipModel, password, headerWriter, buildAsyncParameters()).execute(
        new AddFilesToZipTaskParameters(filesToAdd, parameters, buildConfig()));
  }

  /**
   * Creates a zip file and adds the files/folders from the specified folder to the zip file.
   * This method does the same functionality as in addFolder method except that this method
   * can also create split zip files when adding a folder. To create a split zip file, set the
   * splitArchive parameter to true and specify the splitLength. Split length has to be more than
   * or equal to 65536 bytes. Note that this method throws an exception if the zip file already
   * exists.
   *
   * @param folderToAdd
   * @param parameters
   * @param splitArchive
   * @param splitLength
   * @throws ZipException
   */
  public void createSplitZipFileFromFolder(File folderToAdd, ZipParameters parameters, boolean splitArchive,
                                      long splitLength) throws ZipException {
    if (folderToAdd == null) {
      throw new ZipException("folderToAdd is null, cannot create zip file from folder");
    }

    if (parameters == null) {
      throw new ZipException("input parameters are null, cannot create zip file from folder");
    }

    if (zipFile.exists()) {
      throw new ZipException("zip file: " + zipFile
          + " already exists. To add files to existing zip file use addFolder method");
    }

    createNewZipModel();
    zipModel.setSplitArchive(splitArchive);

    if (splitArchive) {
      zipModel.setSplitLength(splitLength);
    }

    addFolder(folderToAdd, parameters, false);
  }

  /**
   * Adds input source file to the zip file with default zip parameters. If zip file does not exist,
   * this method creates a new zip file.
   *
   * @param fileToAdd - File with path to be added to the zip file
   * @throws ZipException
   */
  public void addFile(String fileToAdd) throws ZipException {
    addFile(fileToAdd, new ZipParameters());
  }

  /**
   * Adds input source file to the zip file with provided zip parameters. If zip file does not exist,
   * this method creates a new zip file.
   *
   * @param fileToAdd - File with path to be added to the zip file
   * @param zipParameters - parameters for the entry to be added to zip
   * @throws ZipException
   */
  public void addFile(String fileToAdd, ZipParameters zipParameters) throws ZipException {
    if (!isStringNotNullAndNotEmpty(fileToAdd)) {
      throw new ZipException("file to add is null or empty");
    }

    addFiles(Collections.singletonList(new File(fileToAdd)), zipParameters);
  }

  /**
   * Adds input source file to the zip file with default zip parameters. If zip file does not exist,
   * this method creates a new zip file.
   *
   * @param fileToAdd - File to be added to the zip file
   * @throws ZipException
   */
  public void addFile(File fileToAdd) throws ZipException {
    addFiles(Collections.singletonList(fileToAdd), new ZipParameters());
  }

  /**
   * Adds input source file to the zip file. If zip file does not exist,
   * this method creates a new zip file. Parameters such as compression type, etc
   * can be set in the input parameters.
   *
   * @param fileToAdd - File to be added to the zip file
   * @param parameters - zip parameters for this file
   * @throws ZipException
   */
  public void addFile(File fileToAdd, ZipParameters parameters) throws ZipException {
    addFiles(Collections.singletonList(fileToAdd), parameters);
  }

  /**
   * Adds the list of input files to the zip file with default zip parameters. If zip file does not exist,
   * this method creates a new zip file.
   *
   * @param filesToAdd
   * @throws ZipException
   */
  public void addFiles(List<File> filesToAdd) throws ZipException {
    addFiles(filesToAdd, new ZipParameters());
  }

  /**
   * Adds the list of input files to the zip file. If zip file does not exist, then
   * this method creates a new zip file. Parameters such as compression type, etc
   * can be set in the input parameters.
   *
   * @param filesToAdd
   * @param parameters
   * @throws ZipException
   */
  public void addFiles(List<File> filesToAdd, ZipParameters parameters) throws ZipException {

    if (filesToAdd == null || filesToAdd.size() == 0) {
      throw new ZipException("input file List is null or empty");
    }

    if (parameters == null) {
      throw new ZipException("input parameters are null");
    }

    readZipInfo();

    if (zipModel == null) {
      throw new ZipException("internal error: zip model is null");
    }

    if (zipFile.exists() && zipModel.isSplitArchive()) {
      throw new ZipException("Zip file already exists. Zip file format does not allow updating split/spanned files");
    }

    new AddFilesToZipTask(zipModel, password, headerWriter, buildAsyncParameters()).execute(
        new AddFilesToZipTaskParameters(filesToAdd, parameters, buildConfig()));
  }

  /**
   * Adds the folder in the given file object to the zip file with default zip parameters. If zip file does not exist,
   * then a new zip file is created. If input folder is invalid then an exception
   * is thrown.
   *
   * @param folderToAdd
   * @throws ZipException
   */
  public void addFolder(File folderToAdd) throws ZipException {
    addFolder(folderToAdd, new ZipParameters());
  }

  /**
   * Adds the folder in the given file object to the zip file. If zip file does not exist,
   * then a new zip file is created. If input folder is invalid then an exception
   * is thrown. Zip parameters for the files in the folder to be added can be set in
   * the input parameters
   *
   * @param folderToAdd
   * @param zipParameters
   * @throws ZipException
   */
  public void addFolder(File folderToAdd, ZipParameters zipParameters) throws ZipException {
    if (folderToAdd == null) {
      throw new ZipException("input path is null, cannot add folder to zip file");
    }

    if (!folderToAdd.exists()) {
      throw new ZipException("folder does not exist");
    }

    if (!folderToAdd.isDirectory()) {
      throw new ZipException("input folder is not a directory");
    }

    if (!folderToAdd.canRead()) {
      throw new ZipException("cannot read input folder");
    }

    if (zipParameters == null) {
      throw new ZipException("input parameters are null, cannot add folder to zip file");
    }

    addFolder(folderToAdd, zipParameters, true);
  }

  /**
   * Internal method to add a folder to the zip file.
   *
   * @param folderToAdd
   * @param zipParameters
   * @param checkSplitArchive
   * @throws ZipException
   */
  private void addFolder(File folderToAdd, ZipParameters zipParameters, boolean checkSplitArchive) throws ZipException {

    readZipInfo();

    if (zipModel == null) {
      throw new ZipException("internal error: zip model is null");
    }

    if (checkSplitArchive) {
      if (zipModel.isSplitArchive()) {
        throw new ZipException("This is a split archive. Zip file format does not allow updating split/spanned files");
      }
    }

    new AddFolderToZipTask(zipModel, password, headerWriter, buildAsyncParameters()).execute(
        new AddFolderToZipTaskParameters(folderToAdd, zipParameters, buildConfig()));
  }

  /**
   * Creates a new entry in the zip file and adds the content of the input stream to the
   * zip file. ZipParameters.isSourceExternalStream and ZipParameters.fileNameInZip have to be
   * set before in the input parameters. If the file name ends with / or \, this method treats the
   * content as a directory. Setting the flag ProgressMonitor.setRunInThread to true will have
   * no effect for this method and hence this method cannot be used to add content to zip in
   * thread mode
   *
   * @param inputStream
   * @param parameters
   * @throws ZipException
   */
  public void addStream(InputStream inputStream, ZipParameters parameters) throws ZipException {
    if (inputStream == null) {
      throw new ZipException("inputstream is null, cannot add file to zip");
    }

    if (parameters == null) {
      throw new ZipException("zip parameters are null");
    }

    this.setRunInThread(false);

    readZipInfo();

    if (zipModel == null) {
      throw new ZipException("internal error: zip model is null");
    }

    if (zipFile.exists() && zipModel.isSplitArchive()) {
      throw new ZipException("Zip file already exists. Zip file format does not allow updating split/spanned files");
    }

    new AddStreamToZipTask(zipModel, password, headerWriter, buildAsyncParameters()).execute(
        new AddStreamToZipTaskParameters(inputStream, parameters, buildConfig()));
  }

  /**
   * Extracts all the files in the given zip file to the input destination path.
   * If zip file does not exist or destination path is invalid then an
   * exception is thrown.
   *
   * @param destinationPath path to which the entries of the zip are to be extracted
   * @throws ZipException when an issue occurs during extraction
   */
  public void extractAll(String destinationPath) throws ZipException {
    extractAll(destinationPath, new UnzipParameters());
  }

  /**
   * Extracts all entries in the zip file to the destination path considering the options defined in
   * UnzipParameters
   *
   * @param destinationPath path to which the entries of the zip are to be extracted
   * @param unzipParameters parameters to be considered during extraction
   * @throws ZipException when an issue occurs during extraction
   */
  public void extractAll(String destinationPath, UnzipParameters unzipParameters) throws ZipException {
    if (!isStringNotNullAndNotEmpty(destinationPath)) {
      throw new ZipException("output path is null or invalid");
    }

    if (!Zip4jUtil.createDirectoryIfNotExists(new File(destinationPath))) {
      throw new ZipException("invalid output path");
    }

    if (zipModel == null) {
      readZipInfo();
    }

    // Throw an exception if zipModel is still null
    if (zipModel == null) {
      throw new ZipException("Internal error occurred when extracting zip file");
    }

    new ExtractAllFilesTask(zipModel, password, unzipParameters, buildAsyncParameters()).execute(
        new ExtractAllFilesTaskParameters(destinationPath, buildConfig()));
  }

  /**
   * Extracts a specific file from the zip file to the destination path.
   * If destination path is invalid, then this method throws an exception.
   * <br><br>
   * If fileHeader is a directory, this method extracts all files under this directory
   *
   * @param fileHeader file header corresponding to the entry which has to be extracted
   * @param destinationPath path to which the entries of the zip are to be extracted
   * @throws ZipException when an issue occurs during extraction
   */
  public void extractFile(FileHeader fileHeader, String destinationPath) throws ZipException {
    extractFile(fileHeader, destinationPath, null, new UnzipParameters());
  }

  /**
   * Extracts a specific file from the zip file to the destination path.
   * If destination path is invalid, then this method throws an exception.
   * <br><br>
   * If fileHeader is a directory, this method extracts all files under this directory
   *
   * @param fileHeader file header corresponding to the entry which has to be extracted
   * @param destinationPath path to which the entries of the zip are to be extracted
   * @param unzipParameters any parameters that have to be considered during extraction
   * @throws ZipException when an issue occurs during extraction
   */
  public void extractFile(FileHeader fileHeader, String destinationPath, UnzipParameters unzipParameters)
      throws ZipException {
    extractFile(fileHeader, destinationPath, null, unzipParameters);
  }

  /**
   * Extracts a specific file from the zip file to the destination path.
   * If destination path is invalid, then this method throws an exception.
   * <br><br>
   * If newFileName is not null or empty, newly created file name will be replaced by
   * the value in newFileName. If this value is null, then the file name will be the
   * value in FileHeader.getFileName. If file being extract is a directory, the directory name
   * will be replaced with the newFileName
   * <br><br>
   * If fileHeader is a directory, this method extracts all files under this directory.
   * <br/><br/>
   * Any parameters that have to be considered during extraction can be passed in through unzipParameters
   *
   * @param fileHeader file header corresponding to the entry which has to be extracted
   * @param destinationPath path to which the entries of the zip are to be extracted
   * @param newFileName if not null, this will be the name given to the file upon extraction
   * @param unzipParameters any parameters that have to be considered during extraction
   * @throws ZipException when an issue occurs during extraction
   */
  public void extractFile(FileHeader fileHeader, String destinationPath, String newFileName,
                          UnzipParameters unzipParameters) throws ZipException {
    if (fileHeader == null) {
      throw new ZipException("input file header is null, cannot extract file");
    }

    extractFile(fileHeader.getFileName(), destinationPath, newFileName, unzipParameters);
  }

  /**
   * Extracts a specific file from the zip file to the destination path.
   * This method first finds the necessary file header from the input file name.
   * <br><br>
   * File name is relative file name in the zip file. For example if a zip file contains
   * a file "a.txt", then to extract this file, input file name has to be "a.txt". Another
   * example is if there is a file "b.txt" in a folder "abc" in the zip file, then the
   * input file name has to be abc/b.txt
   * <br><br>
   * If fileHeader is a directory, this method extracts all files under this directory.
   * <br><br>
   * Throws an exception of type {@link ZipException.Type#FILE_NOT_FOUND} if file header could not be found for the given file name.
   * Throws an exception if the destination path is invalid.
   *
   * @param fileName name of the entry which has to be extracted
   * @param destinationPath path to which the entries of the zip are to be extracted
   * @throws ZipException when an issue occurs during extraction
   */
  public void extractFile(String fileName, String destinationPath) throws ZipException {
    extractFile(fileName, destinationPath, null, new UnzipParameters());
  }

  /**
   * Extracts a specific file from the zip file to the destination path.
   * This method first finds the necessary file header from the input file name.
   * <br><br>
   * File name is relative file name in the zip file. For example if a zip file contains
   * a file "a.txt", then to extract this file, input file name has to be "a.txt". Another
   * example is if there is a file "b.txt" in a folder "abc" in the zip file, then the
   * input file name has to be abc/b.txt
   * <br><br>
   * If fileHeader is a directory, this method extracts all files under this directory.
   * <br><br>
   * Any parameters that have to be considered during extraction can be passed in through unzipParameters
   * <br/><br/>
   * Throws an exception of type {@link ZipException.Type#FILE_NOT_FOUND} if file header could not be found for the given file name.
   * Throws an exception if the destination path is invalid.
   *
   * @param fileName name of the entry which has to be extracted
   * @param destinationPath path to which the entries of the zip are to be extracted
   * @param unzipParameters any parameters that have to be considered during extraction
   * @throws ZipException when an issue occurs during extraction
   */
  public void extractFile(String fileName, String destinationPath, UnzipParameters unzipParameters)
      throws ZipException {
    extractFile(fileName, destinationPath, null, unzipParameters);
  }

  /**
   * Extracts a specific file from the zip file to the destination path.
   * This method first finds the necessary file header from the input file name.
   * <br><br>
   * File name is relative file name in the zip file. For example if a zip file contains
   * a file "a.txt", then to extract this file, input file name has to be "a.txt". Another
   * example is if there is a file "b.txt" in a folder "abc" in the zip file, then the
   * input file name has to be abc/b.txt
   * <br><br>
   * If newFileName is not null or empty, newly created file name will be replaced by
   * the value in newFileName. If this value is null, then the file name will be the
   * value in FileHeader.getFileName. If file being extract is a directory, the directory name
   * will be replaced with the newFileName
   * <br><br>
   * If fileHeader is a directory, this method extracts all files under this directory.
   * <br><br>
   * Throws an exception of type {@link ZipException.Type#FILE_NOT_FOUND} if file header could not be found for the given file name.
   * Throws an exception if the destination path is invalid.
   *
   * @param fileName name of the entry which has to be extracted
   * @param destinationPath path to which the entries of the zip are to be extracted
   * @param newFileName if not null, this will be the name given to the file upon extraction
   * @throws ZipException when an issue occurs during extraction
   */
  public void extractFile(String fileName, String destinationPath, String newFileName) throws ZipException {
    extractFile(fileName, destinationPath, newFileName, new UnzipParameters());
  }

  /**
   * Extracts a specific file from the zip file to the destination path.
   * If destination path is invalid, then this method throws an exception.
   * <br><br>
   * If newFileName is not null or empty, newly created file name will be replaced by
   * the value in newFileName. If this value is null, then the file name will be the
   * value in FileHeader.getFileName. If file being extract is a directory, the directory name
   * will be replaced with the newFileName
   * <br><br>
   * If fileHeader is a directory, this method extracts all files under this directory.
   *
   * @param fileHeader file header corresponding to the entry which has to be extracted
   * @param destinationPath path to which the entries of the zip are to be extracted
   * @param newFileName if not null, this will be the name given to the file upon extraction
   * @throws ZipException when an issue occurs during extraction
   */
  public void extractFile(FileHeader fileHeader, String destinationPath, String newFileName) throws ZipException {
    extractFile(fileHeader, destinationPath, newFileName, new UnzipParameters());
  }

  /**
   * Extracts a specific file from the zip file to the destination path.
   * This method first finds the necessary file header from the input file name.
   * <br/><br/>
   * File name is relative file name in the zip file. For example if a zip file contains
   * a file "a.txt", then to extract this file, input file name has to be "a.txt". Another
   * example is if there is a file "b.txt" in a folder "abc" in the zip file, then the
   * input file name has to be abc/b.txt
   * <br/><br/>
   * If newFileName is not null or empty, newly created file name will be replaced by
   * the value in newFileName. If this value is null, then the file name will be the
   * value in FileHeader.getFileName. If file being extract is a directory, the directory name
   * will be replaced with the newFileName
   * <br/><br/>
   * If fileHeader is a directory, this method extracts all files under this directory.
   * <br/><br/>
   * Any parameters that have to be considered during extraction can be passed in through unzipParameters
   * <br/><br/>
   * Throws an exception of type {@link ZipException.Type#FILE_NOT_FOUND} if file header could not be found for the
   * given file name.
   * Throws an exception if the destination path is invalid.
   *
   * @param fileName name of the entry which has to be extracted
   * @param destinationPath path to which the entries of the zip are to be extracted
   * @param newFileName if not null, this will be the name given to the file upon extraction
   * @param unzipParameters any parameters that have to be considered during extraction
   * @throws ZipException when an issue occurs during extraction
   */
  public void extractFile(String fileName, String destinationPath, String newFileName, UnzipParameters unzipParameters)
      throws ZipException {

    if (!isStringNotNullAndNotEmpty(fileName)) {
      throw new ZipException("file to extract is null or empty, cannot extract file");
    }

    if (!isStringNotNullAndNotEmpty(destinationPath)) {
      throw new ZipException("destination path is empty or null, cannot extract file");
    }

    if (unzipParameters == null) {
      unzipParameters = new UnzipParameters();
    }

    readZipInfo();

    new ExtractFileTask(zipModel, password, unzipParameters, buildAsyncParameters()).execute(
        new ExtractFileTaskParameters(destinationPath, fileName, newFileName, buildConfig()));
  }

  /**
   * Returns the list of file headers in the zip file. Returns an empty list if the zip file does not exist.
   *
   * @return list of file headers
   * @throws ZipException
   */
  public List<FileHeader> getFileHeaders() throws ZipException {
    readZipInfo();
    if (zipModel == null || zipModel.getCentralDirectory() == null) {
      return Collections.emptyList();
    }
    return zipModel.getCentralDirectory().getFileHeaders();
  }

  /**
   * Returns FileHeader if a file header with the given fileHeader
   * string exists in the zip model: If not returns null
   *
   * @param fileName
   * @return FileHeader
   * @throws ZipException
   */
  public FileHeader getFileHeader(String fileName) throws ZipException {
    if (!isStringNotNullAndNotEmpty(fileName)) {
      throw new ZipException("input file name is emtpy or null, cannot get FileHeader");
    }

    readZipInfo();
    if (zipModel == null || zipModel.getCentralDirectory() == null) {
      return null;
    }

    return HeaderUtil.getFileHeader(zipModel, fileName);
  }

  /**
   * Checks to see if the zip file is encrypted
   *
   * @return true if encrypted, false if not
   * @throws ZipException
   */
  public boolean isEncrypted() throws ZipException {
    if (zipModel == null) {
      readZipInfo();
      if (zipModel == null) {
        throw new ZipException("Zip Model is null");
      }
    }

    if (zipModel.getCentralDirectory() == null || zipModel.getCentralDirectory().getFileHeaders() == null) {
      throw new ZipException("invalid zip file");
    }

    for (FileHeader fileHeader : zipModel.getCentralDirectory().getFileHeaders()) {
      if (fileHeader != null) {
        if (fileHeader.isEncrypted()) {
          isEncrypted = true;
          break;
        }
      }
    }

    return isEncrypted;
  }

  /**
   * Checks if the zip file is a split archive
   *
   * @return true if split archive, false if not
   * @throws ZipException
   */
  public boolean isSplitArchive() throws ZipException {

    if (zipModel == null) {
      readZipInfo();
      if (zipModel == null) {
        throw new ZipException("Zip Model is null");
      }
    }

    return zipModel.isSplitArchive();
  }

  /**
   * Removes the file provided in the input file header from the zip file.
   *
   * If zip file is a split zip file, then this method throws an exception as
   * zip specification does not allow for updating split zip archives.
   *
   * If this file header is a directory, all files and directories
   * under this directory will be removed as well.
   *
   * @param fileHeader
   * @throws ZipException
   */
  public void removeFile(FileHeader fileHeader) throws ZipException {
    if (fileHeader == null) {
      throw new ZipException("input file header is null, cannot remove file");
    }

    removeFile(fileHeader.getFileName());
  }

  /**
   * Removes the file provided in the input parameters from the zip file.
   * This method first finds the file header and then removes the file.
   *
   * If file does not exist, then this method throws an exception.
   *
   * If zip file is a split zip file, then this method throws an exception as
   * zip specification does not allow for updating split zip archives.
   *
   * If the entry representing this file name is a directory, all files and directories
   * under this directory will be removed as well.
   *
   * @param fileName
   * @throws ZipException
   */
  public void removeFile(String fileName) throws ZipException {
    if (!isStringNotNullAndNotEmpty(fileName)) {
      throw new ZipException("file name is empty or null, cannot remove file");
    }

    removeFiles(Collections.singletonList(fileName));
  }

  /**
   * Removes all files from the zip file that match the names in the input list.
   *
   * If any of the file is a directory, all the files and directories under this directory
   * will be removed as well
   *
   * If zip file is a split zip file, then this method throws an exception as
   * zip specification does not allow for updating split zip archives.
   *
   * @param fileNames
   * @throws ZipException
   */
  public void removeFiles(List<String> fileNames) throws ZipException {
    if (fileNames == null) {
      throw new ZipException("fileNames list is null");
    }

    if (fileNames.isEmpty()) {
      return;
    }

    if (zipModel == null) {
      readZipInfo();
    }

    if (zipModel.isSplitArchive()) {
      throw new ZipException("Zip file format does not allow updating split/spanned files");
    }

    new RemoveFilesFromZipTask(zipModel, headerWriter, buildAsyncParameters()).execute(
        new RemoveFilesFromZipTaskParameters(fileNames, buildConfig()));
  }

  /**
   * Renames file name of the entry represented by file header. If the file name in the input file header does not
   * match any entry in the zip file, the zip file will not be modified.
   *
   * If the file header is a folder in the zip file, all sub-files and sub-folders in the zip file will also be renamed.
   *
   * Zip file format does not allow modifying a split zip file. Therefore if the zip file being dealt with is a split
   * zip file, this method throws an exception
   *
   * @param fileHeader file header to be changed
   * @param newFileName the file name that has to be changed to
   * @throws ZipException if fileHeader is null or newFileName is null or empty or if the zip file is a split file
   */
  public void renameFile(FileHeader fileHeader, String newFileName) throws ZipException {
    if (fileHeader == null) {
      throw new ZipException("File header is null");
    }

    renameFile(fileHeader.getFileName(), newFileName);
  }

  /**
   * Renames file name of the entry represented by input fileNameToRename. If there is no entry in the zip file matching
   * the file name as in fileNameToRename, the zip file will not be modified.
   *
   * If the entry with fileNameToRename is a folder in the zip file, all sub-files and sub-folders in the zip file will
   * also be renamed. For a folder, the fileNameToRename has to end with zip file separator "/". For example, if a
   * folder name "some-folder-name" has to be modified to "new-folder-name", then value of fileNameToRename should be
   * "some-folder-name/". If newFileName does not end with a separator, zip4j will add a separator.
   *
   * Zip file format does not allow modifying a split zip file. Therefore if the zip file being dealt with is a split
   * zip file, this method throws an exception
   *
   * @param fileNameToRename file name in the zip that has to be renamed
   * @param newFileName the file name that has to be changed to
   * @throws ZipException if fileNameToRename is empty or newFileName is empty or if the zip file is a split file
   */
  public void renameFile(String fileNameToRename, String newFileName) throws ZipException {
    if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileNameToRename)) {
      throw new ZipException("file name to be changed is null or empty");
    }

    if (!Zip4jUtil.isStringNotNullAndNotEmpty(newFileName)) {
      throw new ZipException("newFileName is null or empty");
    }

    renameFiles(Collections.singletonMap(fileNameToRename, newFileName));
  }

  /**
   * Renames all the entries in the zip file that match the keys in the map to their corresponding values in the map. If
   * there are no entries matching any of the keys from the map, the zip file is not modified.
   *
   * If any of the entry in the map represents a folder, all files and folders will be renamed so that their parent
   * represents the renamed folder.
   *
   * Zip file format does not allow modifying a split zip file. Therefore if the zip file being dealt with is a split
   * zip file, this method throws an exception
   *
   * @param fileNamesMap map of file names that have to be changed with values in the map being the name to be changed to
   * @throws ZipException if map is null or if the zip file is a split file
   */
  public void renameFiles(Map<String, String> fileNamesMap) throws ZipException {
    if (fileNamesMap == null) {
      throw new ZipException("fileNamesMap is null");
    }

    if (fileNamesMap.size() == 0) {
      return;
    }

    readZipInfo();

    if (zipModel.isSplitArchive()) {
      throw new ZipException("Zip file format does not allow updating split/spanned files");
    }

    AsyncZipTask.AsyncTaskParameters asyncTaskParameters = buildAsyncParameters();
    new RenameFilesTask(zipModel, headerWriter, new RawIO(), asyncTaskParameters).execute(
        new RenameFilesTaskParameters(fileNamesMap, buildConfig()));
  }

  /**
   * Merges split zip files into a single zip file without the need to extract the
   * files in the archive
   *
   * @param outputZipFile
   * @throws ZipException
   */
  public void mergeSplitFiles(File outputZipFile) throws ZipException {
    if (outputZipFile == null) {
      throw new ZipException("outputZipFile is null, cannot merge split files");
    }

    if (outputZipFile.exists()) {
      throw new ZipException("output Zip File already exists");
    }

    readZipInfo();

    if (this.zipModel == null) {
      throw new ZipException("zip model is null, corrupt zip file?");
    }

    new MergeSplitZipFileTask(zipModel, buildAsyncParameters()).execute(
            new MergeSplitZipFileTaskParameters(outputZipFile, buildConfig()));
  }

  /**
   * Sets comment for the Zip file
   *
   * @param comment
   * @throws ZipException
   */
  public void setComment(String comment) throws ZipException {
    if (comment == null) {
      throw new ZipException("input comment is null, cannot update zip file");
    }

    if (!zipFile.exists()) {
      throw new ZipException("zip file does not exist, cannot set comment for zip file");
    }

    readZipInfo();

    if (zipModel == null) {
      throw new ZipException("zipModel is null, cannot update zip file");
    }

    if (zipModel.getEndOfCentralDirectoryRecord() == null) {
      throw new ZipException("end of central directory is null, cannot set comment");
    }

    new SetCommentTask(zipModel, buildAsyncParameters()).execute(
            new SetCommentTaskTaskParameters(comment, buildConfig()));
  }

  /**
   * Returns the comment set for the Zip file
   *
   * @return String
   * @throws ZipException
   */
  public String getComment() throws ZipException {
    if (!zipFile.exists()) {
      throw new ZipException("zip file does not exist, cannot read comment");
    }

    readZipInfo();

    if (zipModel == null) {
      throw new ZipException("zip model is null, cannot read comment");
    }

    if (zipModel.getEndOfCentralDirectoryRecord() == null) {
      throw new ZipException("end of central directory record is null, cannot read comment");
    }

    return zipModel.getEndOfCentralDirectoryRecord().getComment();
  }

  /**
   * Returns an input stream for reading the contents of the Zip file corresponding
   * to the input FileHeader. Throws an exception if the FileHeader does not exist
   * in the ZipFile
   *
   * @param fileHeader
   * @return ZipInputStream
   * @throws ZipException
   */
  public ZipInputStream getInputStream(FileHeader fileHeader) throws IOException {
    if (fileHeader == null) {
      throw new ZipException("FileHeader is null, cannot get InputStream");
    }

    readZipInfo();

    if (zipModel == null) {
      throw new ZipException("zip model is null, cannot get inputstream");
    }

    ZipInputStream zipInputStream = createZipInputStream(zipModel, fileHeader, password);
    openInputStreams.add(zipInputStream);
    return zipInputStream;
  }

  /**
   * Checks to see if the input zip file is a valid zip file. This method
   * will try to read zip headers. If headers are read successfully, this
   * method returns true else false.
   *
   * Since v2.7.0: if the zip file is a split zip file, this method also checks to see if
   * all the split files of the zip exists.
   *
   * @return boolean - true if a valid zip file, i.e, zip4j is able to read the
   * zip headers, and in case of a split zip file, all split files of the zip exists; false otherwise
   *
   * @since 1.2.3
   */
  public boolean isValidZipFile() {
    if (!zipFile.exists()) {
      return false;
    }

    try {
      readZipInfo();

      if (zipModel.isSplitArchive() && !verifyAllSplitFilesOfZipExists(getSplitZipFiles())) {
        return false;
      }

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Returns the full file path+names of all split zip files
   * in an ArrayList. For example: If a split zip file(abc.zip) has a 10 split parts
   * this method returns an array list with path + "abc.z01", path + "abc.z02", etc.
   * Returns null if the zip file does not exist
   *
   * @return List of Split zip Files
   * @throws ZipException
   */
  public List<File> getSplitZipFiles() throws ZipException {
    readZipInfo();
    return FileUtils.getSplitZipFiles(zipModel);
  }

  /**
   * Closes any open streams that were open by an instance of this class.
   *
   * @throws IOException when the underlying input stream throws an exception when trying to close it
   */
  @Override
  public void close() throws IOException {
    for (InputStream inputStream : openInputStreams) {
      inputStream.close();
    }
    openInputStreams.clear();
  }

  /**
   * Sets a password to be used for the zip file. Will override if a password supplied via ZipFile constructor
   * @param password - char array of the password to be used
   */
  public void setPassword(char[] password) {
    this.password = password;
  }

  /**
   * Returns the size of the buffer used to read streams
   *
   * @return size of the buffer used to read streams
   */
  public int getBufferSize() {
    return bufferSize;
  }

  /**
   * Sets the size of buffer that should be used when reading streams. This size cannot be less than the value defined
   * in InternalZipConstants.MIN_BUFF_SIZE
   *
   * @param bufferSize size of the buffer that should be used when reading streams
   * @throws IllegalArgumentException if bufferSize is less than value configured in InternalZipConstants.MIN_BUFF_SIZE
   */
  public void setBufferSize(int bufferSize) {
    if (bufferSize < MIN_BUFF_SIZE) {
      throw new IllegalArgumentException("Buffer size cannot be less than " + MIN_BUFF_SIZE + " bytes");
    }

    this.bufferSize = bufferSize;
  }

  /**
   * Reads the zip header information for this zip file. If the zip file
   * does not exist, it creates an empty zip model.<br><br>
   * <b>Note:</b> This method does not read local file header information
   *
   * @throws ZipException
   */
  private void readZipInfo() throws ZipException {
    if (zipModel != null) {
      return;
    }

    if (!zipFile.exists()) {
      createNewZipModel();
      return;
    }

    if (!zipFile.canRead()) {
      throw new ZipException("no read access for the input zip file");
    }

    try (RandomAccessFile randomAccessFile = initializeRandomAccessFileForHeaderReading()) {
      HeaderReader headerReader = new HeaderReader();
      zipModel = headerReader.readAllHeaders(randomAccessFile, buildConfig());
      zipModel.setZipFile(zipFile);
    } catch (ZipException e) {
      throw e;
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private void createNewZipModel() {
    zipModel = new ZipModel();
    zipModel.setZipFile(zipFile);
  }

  private RandomAccessFile initializeRandomAccessFileForHeaderReading() throws IOException {
    if (isNumberedSplitFile(zipFile)) {
      File[] allSplitFiles = FileUtils.getAllSortedNumberedSplitFiles(zipFile);
      NumberedSplitRandomAccessFile numberedSplitRandomAccessFile =  new NumberedSplitRandomAccessFile(zipFile,
          RandomAccessFileMode.READ.getValue(), allSplitFiles);
      numberedSplitRandomAccessFile.openLastSplitFileForReading();
      return numberedSplitRandomAccessFile;
    }

    return new RandomAccessFile(zipFile, RandomAccessFileMode.READ.getValue());
  }

  private AsyncZipTask.AsyncTaskParameters buildAsyncParameters() {
    if (runInThread) {
      if (threadFactory == null) {
        threadFactory = Executors.defaultThreadFactory();
      }
      executorService = Executors.newSingleThreadExecutor(threadFactory);
    }

    return new AsyncZipTask.AsyncTaskParameters(executorService, runInThread, progressMonitor);
  }

  private boolean verifyAllSplitFilesOfZipExists(List<File> allSplitFiles) {
    for (File splitFile : allSplitFiles) {
      if (!splitFile.exists()) {
        return false;
      }
    }
    return true;
  }

  public ProgressMonitor getProgressMonitor() {
    return progressMonitor;
  }

  public boolean isRunInThread() {
    return runInThread;
  }

  public void setRunInThread(boolean runInThread) {
    this.runInThread = runInThread;
  }

  public File getFile() {
    return zipFile;
  }

  /**
   * Returns user defined charset that was set by setCharset() method. If no charset was explicitly defined
   * (by calling setCharset()), this method returns the default charset which zip4j uses, which is utf-8.
   *
   * @return user-defined charset or utf-8 if no charset explicitly set
   */
  public Charset getCharset() {
    if (charset == null) {
      return CHARSET_UTF_8;
    }
    return charset;
  }

  /**
   * Sets the charset to be used for encoding file names and comments
   *
   * @param charset charset to use to encode file names and comments
   * @throws IllegalArgumentException if charset is null
   */
  public void setCharset(Charset charset) throws IllegalArgumentException {
    if(charset == null) {
      throw new IllegalArgumentException("charset cannot be null");
    }
    this.charset = charset;
  }

  public void setThreadFactory(ThreadFactory threadFactory) {
    this.threadFactory = threadFactory;
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  @Override
  public String toString() {
    return zipFile.toString();
  }

  private Zip4jConfig buildConfig() {
    return new Zip4jConfig(charset, bufferSize, useUtf8CharsetForPasswords);
  }

  public boolean isUseUtf8CharsetForPasswords() {
    return useUtf8CharsetForPasswords;
  }

  public void setUseUtf8CharsetForPasswords(boolean useUtf8CharsetForPasswords) {
    this.useUtf8CharsetForPasswords = useUtf8CharsetForPasswords;
  }
}
