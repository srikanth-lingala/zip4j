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
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
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
import net.lingala.zip4j.tasks.ExtractAllFilesTask;
import net.lingala.zip4j.tasks.ExtractAllFilesTask.ExtractAllFilesTaskParameters;
import net.lingala.zip4j.tasks.ExtractFileTask;
import net.lingala.zip4j.tasks.ExtractFileTask.ExtractFileTaskParameters;
import net.lingala.zip4j.tasks.MergeSplitZipFileTask;
import net.lingala.zip4j.tasks.RemoveEntryFromZipFileTask;
import net.lingala.zip4j.tasks.SetCommentTask;
import net.lingala.zip4j.util.FileUtils;
import net.lingala.zip4j.util.Zip4jUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.List;

import static net.lingala.zip4j.util.FileUtils.assertFilesExist;
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

public class ZipFile {

  private File zipFile;
  private ZipModel zipModel;
  private boolean isEncrypted;
  private ProgressMonitor progressMonitor;
  private boolean runInThread;
  private char[] password;
  private HeaderWriter headerWriter = new HeaderWriter();

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
   * @param zipFile
   */
  public ZipFile(File zipFile) {
    this(zipFile, null);
  }

  public ZipFile(File zipFile, char[] password) {
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

    new AddFilesToZipTask(progressMonitor, runInThread, zipModel, password, headerWriter).execute(
        new AddFilesToZipTaskParameters(filesToAdd, parameters));
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

    if (progressMonitor.getState() == ProgressMonitor.State.BUSY) {
      throw new ZipException("invalid operation - Zip4j is in busy state");
    }

    assertFilesExist(filesToAdd);

    checkZipModel();

    if (zipModel == null) {
      throw new ZipException("internal error: zip model is null");
    }

    if (zipFile.exists() && zipModel.isSplitArchive()) {
      throw new ZipException("Zip file already exists. Zip file format does not allow updating split/spanned files");
    }

    new AddFilesToZipTask(progressMonitor, runInThread, zipModel, password, headerWriter).execute(
        new AddFilesToZipTaskParameters(filesToAdd, parameters));
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

    checkZipModel();

    if (zipModel == null) {
      throw new ZipException("internal error: zip model is null");
    }

    if (checkSplitArchive) {
      if (zipModel.isSplitArchive()) {
        throw new ZipException("This is a split archive. Zip file format does not allow updating split/spanned files");
      }
    }

    new AddFolderToZipTask(progressMonitor, runInThread, zipModel, password, headerWriter).execute(
        new AddFolderToZipTaskParameters(folderToAdd, zipParameters));
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

    checkZipModel();

    if (zipModel == null) {
      throw new ZipException("internal error: zip model is null");
    }

    if (zipFile.exists() && zipModel.isSplitArchive()) {
      throw new ZipException("Zip file already exists. Zip file format does not allow updating split/spanned files");
    }

    new AddStreamToZipTask(progressMonitor, runInThread, zipModel, password, headerWriter).execute(
        new AddStreamToZipTaskParameters(inputStream, parameters));
  }

  /**
   * Extracts all the files in the given zip file to the input destination path.
   * If zip file does not exist or destination path is invalid then an
   * exception is thrown.
   *
   * @param destinationPath
   * @throws ZipException
   */
  public void extractAll(String destinationPath) throws ZipException {

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

    if (progressMonitor.getState() == ProgressMonitor.State.BUSY) {
      throw new ZipException("invalid operation - Zip4j is in busy state");
    }

    new ExtractAllFilesTask(progressMonitor, runInThread, zipModel, password).execute(
        new ExtractAllFilesTaskParameters(destinationPath));
  }

  /**
   * Extracts a specific file from the zip file to the destination path.
   * If destination path is invalid, then this method throws an exception.
   * <br><br>
   * If newFileName is not null or empty, newly created file name will be replaced by
   * the value in newFileName. If this value is null, then the file name will be the
   * value in FileHeader.getFileName
   *
   * @param fileHeader
   * @param destinationPath
   * @throws ZipException
   */
  public void extractFile(FileHeader fileHeader, String destinationPath) throws ZipException {
    extractFile(fileHeader, destinationPath, null);
  }

  /**
   * Extracts a specific file from the zip file to the destination path.
   * If destination path is invalid, then this method throws an exception.
   *
   * @param fileHeader
   * @param destinationPath
   * @param newFileName
   * @throws ZipException
   */
  public void extractFile(FileHeader fileHeader, String destinationPath, String newFileName) throws ZipException {
    if (fileHeader == null) {
      throw new ZipException("input file header is null, cannot extract file");
    }

    if (!isStringNotNullAndNotEmpty(destinationPath)) {
      throw new ZipException("destination path is empty or null, cannot extract file");
    }

    if (progressMonitor.getState() == ProgressMonitor.State.BUSY) {
      throw new ZipException("invalid operation - Zip4j is in busy state");
    }

    readZipInfo();

    new ExtractFileTask(progressMonitor, runInThread, zipModel, password).execute(
        new ExtractFileTaskParameters(destinationPath, fileHeader, newFileName));
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
   * Throws an exception if file header could not be found for the given file name or if
   * the destination path is invalid
   *
   * @param fileName
   * @param destinationPath
   * @throws ZipException
   */
  public void extractFile(String fileName, String destinationPath) throws ZipException {
    extractFile(fileName, destinationPath, null);
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
   * value in FileHeader.getFileName
   * <br><br>
   * Throws an exception if file header could not be found for the given file name or if
   * the destination path is invalid
   *
   * @param fileName
   * @param destinationPath
   * @param newFileName
   * @throws ZipException
   */
  public void extractFile(String fileName, String destinationPath, String newFileName) throws ZipException {

    if (!isStringNotNullAndNotEmpty(fileName)) {
      throw new ZipException("file to extract is null or empty, cannot extract file");
    }

    readZipInfo();

    FileHeader fileHeader = HeaderUtil.getFileHeader(zipModel, fileName);

    if (fileHeader == null) {
      throw new ZipException("No file found with name " + fileName + " in zip file");
    }

    extractFile(fileHeader, destinationPath, newFileName);
  }

  /**
   * Returns the list of file headers in the zip file. Throws an exception if the
   * zip file does not exist
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
   * Removes the file provided in the input parameters from the zip file.
   * This method first finds the file header and then removes the file.
   * If file does not exist, then this method throws an exception.
   * If zip file is a split zip file, then this method throws an exception as
   * zip specification does not allow for updating split zip archives.
   *
   * @param fileName
   * @throws ZipException
   */
  public void removeFile(String fileName) throws ZipException {

    if (!isStringNotNullAndNotEmpty(fileName)) {
      throw new ZipException("file name is empty or null, cannot remove file");
    }

    if (zipModel == null) {
      readZipInfo();
    }

    if (zipModel.isSplitArchive()) {
      throw new ZipException("Zip file format does not allow updating split/spanned files");
    }

    FileHeader fileHeader = HeaderUtil.getFileHeader(zipModel, fileName);
    if (fileHeader == null) {
      throw new ZipException("could not find file header for file: " + fileName);
    }

    removeFile(fileHeader);
  }

  /**
   * Removes the file provided in the input file header from the zip file.
   * If zip file is a split zip file, then this method throws an exception as
   * zip specification does not allow for updating split zip archives.
   *
   * @param fileHeader
   * @throws ZipException
   */
  public void removeFile(FileHeader fileHeader) throws ZipException {
    if (fileHeader == null) {
      throw new ZipException("input file header is null, cannot remove file");
    }

    if (zipModel == null) {
      readZipInfo();
    }

    if (zipModel.isSplitArchive()) {
      throw new ZipException("Zip file format does not allow updating split/spanned files");
    }

    new RemoveEntryFromZipFileTask(progressMonitor, runInThread, zipModel).execute(fileHeader);
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

    checkZipModel();

    if (this.zipModel == null) {
      throw new ZipException("zip model is null, corrupt zip file?");
    }

    new MergeSplitZipFileTask(progressMonitor, runInThread, zipModel).execute(outputZipFile);
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

    new SetCommentTask(progressMonitor, runInThread, zipModel).execute(comment);
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

    checkZipModel();

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
  public ZipInputStream getInputStream(FileHeader fileHeader) throws ZipException {
    if (fileHeader == null) {
      throw new ZipException("FileHeader is null, cannot get InputStream");
    }

    checkZipModel();

    if (zipModel == null) {
      throw new ZipException("zip model is null, cannot get inputstream");
    }

    return createZipInputStream(zipModel, fileHeader, password);
  }

  /**
   * Checks to see if the input zip file is a valid zip file. This method
   * will try to read zip headers. If headers are read successfully, this
   * method returns true else false
   *
   * @return boolean
   * @since 1.2.3
   */
  public boolean isValidZipFile() {
    if (!zipFile.exists()) {
      return false;
    }

    try {
      readZipInfo();
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
    checkZipModel();
    return FileUtils.getSplitZipFiles(zipModel);
  }

  /**
   * Reads the zip header information for this zip file. If the zip file
   * does not exist, then this method throws an exception.<br><br>
   * <b>Note:</b> This method does not read local file header information
   *
   * @throws ZipException
   */
  private void readZipInfo() throws ZipException {
    if (!zipFile.exists()) {
      createNewZipModel();
      return;
    }

    if (!zipFile.canRead()) {
      throw new ZipException("no read access for the input zip file");
    }

    try (RandomAccessFile randomAccessFile = new RandomAccessFile(zipFile, RandomAccessFileMode.READ.getValue())) {
      HeaderReader headerReader = new HeaderReader();
      zipModel = headerReader.readAllHeaders(randomAccessFile);
      zipModel.setZipFile(zipFile);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  /**
   * Loads the zip model if zip model is null and if zip file exists.
   *
   * @throws ZipException
   */
  private void checkZipModel() throws ZipException {
    if (zipModel == null) {
      readZipInfo();
    }
  }

  /**
   * Creates a new instance of zip model
   *
   * @throws ZipException
   */
  private void createNewZipModel() {
    zipModel = new ZipModel();
    zipModel.setZipFile(zipFile);
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

  @Override
  public String toString() {
    return zipFile.toString();
  }
}
