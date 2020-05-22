
[![Build Status](https://travis-ci.org/srikanth-lingala/zip4j.svg?branch=master)](https://travis-ci.org/srikanth-lingala/zip4j)
[![Android Build Status](https://circleci.com/gh/srikanth-lingala/zip4j-android-test.svg?style=svg)](https://circleci.com/gh/srikanth-lingala/zip4j-android-test)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.lingala.zip4j/zip4j/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.lingala.zip4j/zip4j)
[![Known Vulnerabilities](https://snyk.io//test/github/srikanth-lingala/zip4j/badge.svg?targetFile=pom.xml)](https://snyk.io//test/github/srikanth-lingala/zip4j?targetFile=pom.xml)



Zip4j - A java library for zip files / streams
=========================

## Thank you

for rating Zip4j as the best java library for zip files <sup>[[1][1], [2][2], [3][3], [4][4]]</sup>. It has encouraged me to 
bring this project to life again after a gap of several years. I tried to add some of the important features that 
were requested over this time, and also made api much more neater. The newer version (> 2.0.0) now supports streams,
which was understandably, one of the most requested feature. If you have any feedback, bugs to report, feature 
requests, etc, please open an issue here on github. I will try to address them as soon as I can. I also monitor the
tag `zip4j` on [stackoverflow][10].

## About

Zip4j is the most comprehensive java library for Zip files or streams. As of this writing, it is the only java library 
which has support for zip encryption, apart from several other features. It tries to make handling zip files/streams 
a lot more easier. No more clunky boiler plate code with input streams and output streams. As you can see in the usage 
section below, working with zip files can now even be a single line of code, compared to [this][5]. I mean no offense
to the Java's in-built zip support. In fact, this library depends on Java's in-built zip code and it would have been 
significantly more ~~complicated~~ challenging if I had to write compression logic as well. But lets be honest, working with zip 
files or streams can be a lot of boiler plate code. The main goal of this library is to provide a simple api for all 
usual actions of a zip file or streams by doing the heavy lifting within the library and not have developers worry about
having to deal with streams, etc. Apart from usability, other important goal of this library is to provide support for
as many zip features as possible, which brings me to:

## Features
~~~
 * Create, Add, Extract, Update, Remove files from a Zip file
 * Support for streams (ZipInputStream and ZipOutputStream)
 * Read/Write password protected Zip files and streams
 * Support for both AES and Zip-Standard encryption methods
 * Support for Zip64 format
 * Store (No Compression) and Deflate compression method
 * Create or extract files from Split Zip files (Ex: z01, z02,...zip)
 * Support for Unicode file names and comments in zip
 * Progress Monitor - for integration into apps and user facing applications
~~~

## Background

Zip4j was started by me (Srikanth Reddy Lingala) back in 2008/2009, when I realized the lack of support for majority of zip format 
features in Java. And also working with zip files was, as mentioned several times above, a lot of boiler plate code, 
having to deal with streams (worse still, it was back in the days when there was no try-with-resources in java). There
was also no comprehensive library which supports zip features. So, I decided to write one, and approximately after a 
year, the first version was out. The response was truly overwhelming, and I got a lot of support right from the next
day of release. It was not put on github as git/github was not as popular as it is now. Code was hosted on my website,
as, guess what, a zip file :). And unfortunately, after a year or two after the initial release, life got busy and I was
not able to support Zip4j as much as I wanted to. But the overwhelming encouragement I got over the years made me start working on Zip4j
once again, and makes me support Zip4j as much as I can.

## Maven

~~~~
<dependency>
    <groupId>net.lingala.zip4j</groupId>
    <artifactId>zip4j</artifactId>
    <version>2.6.0</version>
</dependency>
~~~~

Please check the latest version number on [Zip4j's Maven repository][6]

## Usage

### Creating a zip file with single file in it / Adding single file to an existing zip

~~~~
new ZipFile("filename.zip").addFile("filename.ext");
~~~~

&nbsp;&nbsp; Or

~~~~
new ZipFile("filename.zip").addFile(new File("filename.ext"));
~~~~

### Creating a zip file with multiple files / Adding multiple files to an existing zip

~~~~
new ZipFile("filename.zip").addFiles(Arrays.asList(new File("first_file"), new File("second_file")));
~~~~

### Creating a zip file by adding a folder to it / Adding a folder to an existing zip

~~~~
new ZipFile("filename.zip").addFolder(new File("/user/myuser/folder_to_add"));
~~~~

Since v2.6, it is possible to exclude certain files when adding a folder to zip by using an ExcludeFileFilter

~~~~
List<File> filesToExclude = Arrays.asList(new File("sample.pdf"), new File("sample_2.txt"));
ExcludeFileFilter excludeFileFilter = filesToExclude::contains;
new ZipFile("filename.zip").addFolder(new File("/user/myuser/folder_to_add"), new ZipParameters, excludeFileFilter);
~~~~

### Creating a zip file from stream / Adding a stream to an existing zip

~~~~
new ZipFile("filename.zip").addStream(inputStream, new ZipParameters());
~~~~

Passing in `new ZipParameters()`, as in the above example, will make Zip4j use default zip parameters. Please look at
[ZipParameters][7] to see the default configuration. 

### Creating a zip file of compression method store / Adding entries to zip file of compression method store

By default Zip4j uses Deflate compression algorithm to compress files. However, if you would like to not use any
compression (called STORE compression), you can do so as shown in the example below: 

~~~~
ZipParameters zipParameters = new ZipParameters();
zipParameters.setCompressionMethod(CompressionMethod.STORE);

new ZipFile("filename.zip").addFile("fileToAdd", zipParameters);
~~~~

You can similarly pass in zip parameters to all the other examples to create a zip file of STORE compression.

### Creating a password protected zip file / Adding files to an existing zip with password protection

##### AES encryption

~~~~
ZipParameters zipParameters = new ZipParameters();
zipParameters.setEncryptFiles(true);
zipParameters.setEncryptionMethod(EncryptionMethod.AES);
// Below line is optional. AES 256 is used by default. You can override it to use AES 128. AES 192 is supported only for extracting.
zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256); 

List<File> filesToAdd = Arrays.asList(
    new File("somefile"), 
    new File("someotherfile")
);

ZipFile zipFile = new ZipFile("filename.zip", "password".toCharArray());
zipFile.addFiles(filesToAdd, zipParameters);
~~~~

##### Zip Standard encryption:

Instead of AES, replace `zipParameters.setEncryptionMethod(EncryptionMethod.AES);` with
`zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);`. You can omit the line to set Aes Key strength. As
the name suggests, this is only applicable for AES encryption.

In all the above examples, you can similarly pass in zip parameters with appropriate password configuration to create
a password protected zip file

### Creating a split zip file

If you want to split the zip file over several files when the size exceeds a particular limit, you can do so like this:

~~~~
List<File> filesToAdd = Arrays.asList(
    new File("somefile"), 
    new File("someotherfile")
);

ZipFile zipFile = new ZipFile("filename.zip");
zipFile.createSplitZipFile(filesToAdd, new ZipParameters(), true, 10485760); // using 10MB in this example
~~~~

Passing in `new ZipParameters()`, as in the above example, will make Zip4j use default zip parameters. Please look at
[ZipParameters][7] to see the default configuration. 

Zip file format specifies a minimum of 65536 bytes (64kb) as a minimum length for split files. Zip4j will throw an
exception if anything less than this value is specified.

To create a split zip with password protection, pass in appropriate ZipParameters as shown in the example below:

~~~~
ZipParameters zipParameters = new ZipParameters();
zipParameters.setEncryptFiles(true);
zipParameters.setEncryptionMethod(EncryptionMethod.AES);

List<File> filesToAdd = Arrays.asList(
    new File("somefile"), 
    new File("someotherfile")
);

ZipFile zipFile = new ZipFile("filename.zip", "password".toCharArray());
zipFile.createSplitZipFile(filesToAdd, zipParameters, true, 10485760); // using 10MB in this example
~~~~

### Zip64 format

Zip64 is a zip feature which allows support for zip files when the size of the zip file exceeds the maximum that can be 
stored in 4 bytes (i.e., greater than 4,294,967,295 bytes). Traditionally, zip headers have a provision of 4 bytes to store
for file sizes. But with growing file sizes compared to a few decades back, zip file format extended support of file 
sizes which extends 4 bytes by adding additional headers which uses 8 bytes for file sizes (compressed and 
uncompressed file sizes). This feature is known as Zip64.

Zip4j will automatically make a zip file a Zip64 format and add appropriate headers, when it detects the zip file to be
crossing this file size limit. You do not have to explicitly specify any flag for Zip4j to use this feature. 

### Extracting All files in a zip

~~~~
new ZipFile("filename.zip").extractAll("/destination_directory");
~~~~

### Extracting All files in a password protected zip

~~~~
new ZipFile("filename.zip", "password".toCharArray()).extractAll("/destination_directory");
~~~~

### Extracting a single file from zip

~~~~
new ZipFile("filename.zip").extractFile("fileNameInZip.txt", "/destination_directory");
~~~~

### Extracting a folder from zip (since v2.6.0)

~~~~
new ZipFile("filename.zip").extractFile("folderNameInZip/", "/destination_directory");
~~~~

### Extracting a single file from zip which is password protected

~~~~
new ZipFile("filename.zip", "password".toCharArray()).extractFile("fileNameInZip.txt", "/destination_directory");
~~~~

Since v2.6.0: If the file name represents a directory, zip4j will extract all files in the zip that are part of this directory. 

### Extracting a single file from zip and giving it a new file name

Below example will extract the file `fileNameInZip.txt` from the zip file to the output directory `/destination_directory` 
and will give the file a name `newfileName.txt`. Without the third parameter of the new file name, the same name as the
file in the zip will be used, which in this case is `fileNameInZip.txt`. If the file being extracted is a directory,
`newFileName` parameter will be used as the directory name. 

~~~~
new ZipFile("filename.zip", "password".toCharArray()).extractFile("fileNameInZip.txt", "/destination_directory", "newfileName.txt");
~~~~

### Get an input stream for an entry in a zip file

~~~~
ZipFile zipFile = new ZipFile("filename.zip");
FileHeader fileHeader = zipFile.getFileHeader("entry_name_in_zip.txt");
InputStream inputStream = zipFile.getInputStream(fileHeader);
~~~~

You can now use this input stream to read content from it/write content to an output stream. Please note that the
entry/file name is relative to the directory it is in. If `entry_name_in_zip.txt` is in a folder called "root_folder" in
the zip, then you can use `zipFile.getFileHeader("root_folder/entry_name_in_zip.txt");`

### Remove a file/entry from a zip file

~~~~
new ZipFile("filename.zip").removeFile("fileNameInZipToRemove");
~~~~

If `fileNameInZipToRemove` represents a folder. All the files and folders under this folder will be removed as well
(this is valid since v2.5.0 of zip4j. All prior versions remove just the single entry even if it is a folder). 

Please note that the file name is relative the root folder in zip. That is, if the file you want to remove exists in a 
folder called "folder1", which in-turn exists in a folder called "root-folder", removing this file from zip can be done 
as below:

~~~~
new ZipFile("filename.zip").removeFile("root-folder/folder1/fileNameInZipToRemove");
~~~~

If you want to be sure that the file you want to remove exists in zip file or if you don't want to deal with file names
as string when dealing `removeFile` api, you can use the other overloaded method which takes in a `FileHeader`:

~~~~
ZipFile zipFile = new ZipFile("someZip.zip");
FileHeader fileHeader = zipFile.getFileHeader("fileNameInZipToRemove");

if (fileHeader == null) {
  // file does not exist
}

zipFile.removeFile(fileHeader);
~~~~

Since v2.5.0 of zip4j, it is possible to remove multiple files and folders from a zip file. You can now pass in a list
as shown in the code below:

~~~~
ZipFile zipFile = new ZipFile("someZip.zip");
List<String> filesToRemove = Arrays.asList("file1.txt", "file2.txt", "some-folder/", "some-new-folder-1/somefile.pdf");

zipFile.removeFiles(filesToRemove);
~~~~

The above code will remove `file1.txt`, `file2.txt`, all files and folders under `some-folder` (including `some-folder`)
and just the entry `somefile.pdf` in folder `some-new-folder-1`. All other files and folders are kept intact in the zip
file.

### Rename entries in the zip file

There are three ways to rename an entry in a zip file with zip4j. One way is to pass in a file header and the new file 
name:

~~~~
ZipFile zipFile = new ZipFile("sample.zip");
FileHeader fileHeader = zipFile.getFileHeader("entry-to-be-changed.pdf");
zipFile.renameFile(fileHeader, "new-file-name.pdf");
~~~~

Second way is to pass in just the file name to be changed (instead of the file header), and the new file name. 

~~~~
new ZipFile("filename.zip").renameFile("entry-to-be-changed.pdf", "new-file-name.pdf");
~~~~

It is also possible to change multiple file names at once. In this case you have to use a map, with the key of the entry 
in the map being the entry to be changed, and the value of the map being the new file name:

~~~~
Map<String, String> fileNamesMap = new HashMap<>();
fileNamesMap.put("firstFile.txt", "newFileFirst.txt");
fileNamesMap.put("secondFile.pdf", "newSecondFile.pdf");
fileNamesMap.put("some-folder/thirdFile.bin", "some-folder/newThirdFile.bin");
new ZipFile("filename.zip").renameFile("entry-to-be-changed.pdf", "new-file-name.pdf");
~~~~

To modify an entry name which is inside a folder, the new file name should contain the complete parent path as well.
For example, if an entry by the name `some-entry.pdf` is in the folder `some-folder/some-sub-folder/`, to modify this 
entry name to `some-new-entry.pdf`:

~~~~
new ZipFile("filename.zip").renameFile("some-folder/some-sub-folder/some-entry.pdf", "some-folder/some-sub-folder/new-entry.pdf");
~~~~

if the parent path path is missing, then the file will be put at the root of the zip file. In the below example, after
the file is renamed, `some-new-entry.pdf` will exist at the root of the zip file instead of at `some-folder/some-sub-folder/`:

~~~~
new ZipFile("filename.zip").renameFile("some-folder/some-sub-folder/some-entry.pdf", "some-new-entry.pdf");
~~~~

This also gives the flexibility to "move" the entry to a different folder. The below example will move the 
`some-entry.pdf` from `some-folder/some-sub-folder/` to `folder-to-be-moved-to/sub-folder/` and the file will also be 
renamed to `new-entry.pdf`. To just move the file, use the same file name instead of a new file name.

~~~~
new ZipFile("filename.zip").renameFile("some-folder/some-sub-folder/some-entry.pdf", "folder-to-be-moved-to/sub-folder/new-entry.pdf");
~~~~

If the entry being modified is a directory, all entries that are part of that directory will be renamed so that all of 
them have the new folder name as parent. In zip format, all entry names under a directory will contain the full name as their file name.
For example if there is an entry by the name `filename.txt` inside a directory `directoryName`, the file name for the entry 
will be `directoryName/filename.txt`. And if the name of the directory has now been changed to `newDirectoryName`, the
entry under it will also be changed to `newDirectoryName/filename.txt`, so the when the zip file is extracted, 
`filename.txt` will be under `newDirectoryName`.

Zip file format does not allow modifying split zip files, and zip4j will throw an exception if an attempt is made to 
rename files in a split zip file.

### Merging split zip files into a single zip

This is the reverse of creating a split zip file, that is, this feature will merge a zip file which is split across 
several files into a single zip file

~~~~
new ZipFile("split_zip_file.zip").mergeZipFile("merged_zip_file.zip");
~~~~

This method will throw an exception if the split zip file (in this case `split_zip_file.zip`) is not a split zip file

### List all files in a zip

~~~~
List<FileHeader> fileHeaders = new ZipFile("zipfile.zip").getFileHeaders();
fileHeaders.stream().forEach(fileHeader -> System.out.println(fileHeader.getFileName()));
~~~~

You can get all other information from the `FileHeader` object corresponding to each file/entry in the zip.

### Check if a zip file is password protected

~~~~
new ZipFile("encrypted_zip_file.zip").isEncrypted();
~~~~

### Check if a zip file is a split zip file

~~~~
new ZipFile("split_zip_file.zip").isSplitArchive();
~~~~

### Set comment for a zip file

~~~~
new ZipFile("some_zip_file.zip").setComment("Some comment");
~~~~

### Remove comment of a zip file

~~~~
new ZipFile("some_zip_file.zip").setComment("");
~~~~

### Get comment of a zip file

~~~~
new ZipFile("some_zip_file.zip").getComment();
~~~~

### Check if a zip file is valid

Note: This will only check for the validity of the headers and not the validity of each entry in the zip file.

~~~~
new ZipFile("valid_zip_file.zip").isValidZipFile();
~~~~

## Working with streams

### Adding entries with ZipOutputStream

~~~~
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ZipOutputStreamExample {

  public void zipOutputStreamExample(File outputZipFile, List<File> filesToAdd, char[] password,  
                                     CompressionMethod compressionMethod, boolean encrypt,
                                     EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength)
      throws IOException {

    ZipParameters zipParameters = buildZipParameters(compressionMethod, encrypt, encryptionMethod, aesKeyStrength);
    byte[] buff = new byte[4096];
    int readLen;

    try(ZipOutputStream zos = initializeZipOutputStream(outputZipFile, encrypt, password)) {
      for (File fileToAdd : filesToAdd) {

        // Entry size has to be set if you want to add entries of STORE compression method (no compression)
        // This is not required for deflate compression
        if (zipParameters.getCompressionMethod() == CompressionMethod.STORE) {
          zipParameters.setEntrySize(fileToAdd.length());
        }

        zipParameters.setFileNameInZip(fileToAdd.getName());
        zos.putNextEntry(zipParameters);

        try(InputStream inputStream = new FileInputStream(fileToAdd)) {
          while ((readLen = inputStream.read(buff)) != -1) {
            zos.write(buff, 0, readLen);
          }
        }
        zos.closeEntry();
      }
    }
  }

  private ZipOutputStream initializeZipOutputStream(File outputZipFile, boolean encrypt, char[] password) 
      throws IOException {
    
    FileOutputStream fos = new FileOutputStream(outputZipFile);

    if (encrypt) {
      return new ZipOutputStream(fos, password);
    }

    return new ZipOutputStream(fos);
  }

  private ZipParameters buildZipParameters(CompressionMethod compressionMethod, boolean encrypt,
                                           EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength) {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(compressionMethod);
    zipParameters.setEncryptionMethod(encryptionMethod);
    zipParameters.setAesKeyStrength(aesKeyStrength);
    zipParameters.setEncryptFiles(encrypt);
    return zipParameters;
  }
}
~~~~

### Extract files with ZipInputStream

~~~~
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ZipInputStreamExample {
  
  public void extractWithZipInputStream(File zipFile, char[] password) throws IOException {
    LocalFileHeader localFileHeader;
    int readLen;
    byte[] readBuffer = new byte[4096];

    InputStream inputStream = new FileInputStream(zipFile);
    try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, password)) {
      while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
        File extractedFile = new File(localFileHeader.getFileName());
        try (OutputStream outputStream = new FileOutputStream(extractedFile)) {
          while ((readLen = zipInputStream.read(readBuffer)) != -1) {
            outputStream.write(readBuffer, 0, readLen);
          }
        }
      }
    }
  }
}


~~~~

## Working with Progress Monitor

ProgressMonitor makes it easier for applications (especially user facing) to integrate Zip4j. It is useful to show
progress (example: updating a progress bar, displaying the current action, show file name being worked on, etc). To use
ProgressMonitor, you have to set `ZipFile.setRunInThread(true)`. This will make any actions being done on the zip file
to run in a background thread. You can then access ProgressMonitor `Zipfile.getProgressMonitor()` and get details of the
current action being done along with the percentage work done, etc. Below is an example:

~~~
ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
ProgressMonitor progressMonitor = zipFile.getProgressMonitor();

zipFile.setRunInThread(true);
zipFile.addFolder(new File("/some/folder"));

while (!progressMonitor.getState().equals(ProgressMonitor.State.READY)) {
  System.out.println("Percentage done: " + progressMonitor.getPercentDone());
  System.out.println("Current file: " + progressMonitor.getFileName());
  System.out.println("Current task: " + progressMonitor.getCurrentTask());

  Thread.sleep(100);
}

if (progressMonitor.getResult().equals(ProgressMonitor.Result.SUCCESS)) {
  System.out.println("Successfully added folder to zip");
} else if (progressMonitor.getResult().equals(ProgressMonitor.Result.ERROR)) {
  System.out.println("Error occurred. Error message: " + progressMonitor.getException().getMessage());
} else if (progressMonitor.getResult().equals(ProgressMonitor.Result.CANCELLED)) {
  System.out.println("Task cancelled");
}
~~~

Note that in the above example, `addFolder()` will almost immediately return back the control to the caller. The client
code can then perform a loop until the state gets back to "Ready" as shown in the above example.

Similarly, ProgressMonitor can be used with other actions like, `addFiles`, `removeFiles` and `extractFiles`.

## Contribution

It is hard to find as much free time as I used to have when I first started Zip4j 10 years back in 2009. I would
highly appreciate any support I can get for this project. You can fork this project, and send me pull requests for
any bug fixes, issues mentioned here or new features. If you need any support in understanding the code or zip specification, 
just drop me a mail and I will help you as best as I can. (See FAQ for my email id.)

## FAQ

1. **Why do I have to pass in password as char array and not as a string?**

    [That's why][8] 

2. **How can I contact you?**

    srikanth.mailbox@gmail.com

3. **Are unicode file names supported?**

    Yes, unicode file names (UTF-8) are supported as specified by the zip format specification. Zip4j will use utf-8 file
name and file comment encoding when creating a zip file. When extracting a zip file, Zip4j will only use utf-8 encoding,
only if the appropriate header flag is set as specified by zip file format specification. If this flag is not set, 
Zip4j will use Cp437 encoding which only supports English alphabetical characters.
 
4. **Where can I find Zip file format specification?**

    [Here][9]

5. **Why are there so many changes in version 2.x compared to 1.x?**

    Because 1.x was written about 10 years back, Zip4j was badly in need of a face-lift and code modernization. Also, my 
coding standards have also improved over the years (or at least that's what I like to think). Although I am proud of 
the work I did with Zip4j 10 years back, some parts of the code make me feel like hiding my face in shame. One such example
is the usage of `ArrayList` instead of `List`. Api and code should look much neater now. And also, Zip4j now supports
a minimum of JRE 8, as compared to JRE 5 with 1.x, which obviously will bring some nice features that I can make use of. (For
example: no more explicitly closing the streams all over the code). If you still feel like something can be improved (and
I am pretty sure that there are things to be improved), please let me know by opening an issue here or writing to me 
(My email id is in point #2 above).

6. **What are the licensing conditions for older releases of zip4j?**

    All releases of zip4j, from version 1.0, are licensed under Apache License 2.0


[1]: https://stackoverflow.com/questions/9324933/what-is-a-good-java-library-to-zip-unzip-files
[2]: https://stackoverflow.com/questions/5362364/java-library-to-work-with-zip-files
[3]: https://stackoverflow.com/questions/166340/recommendations-on-a-free-library-to-be-used-for-zipping-files
[4]: https://stackoverflow.com/questions/18201279/file-compression-library-for-java/18201553
[5]: https://www.baeldung.com/java-compress-and-uncompress
[6]: https://mvnrepository.com/artifact/net.lingala.zip4j/zip4j
[7]: https://github.com/srikanth-lingala/zip4j/blob/master/src/main/java/net/lingala/zip4j/model/ZipParameters.java
[8]: https://www.baeldung.com/java-storing-passwords
[9]: https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT
[10]: https://stackoverflow.com/questions/tagged/zip4j
