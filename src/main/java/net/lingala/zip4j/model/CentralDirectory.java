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

package net.lingala.zip4j.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.lingala.zip4j.util.Zip4jUtil.isStringNotNullAndNotEmpty;

public class CentralDirectory {

  private List<FileHeader> fileHeaders = new ArrayList<>();
  private HashMap<String, FileHeader> fileNameMap = new HashMap<>();
  private DigitalSignature digitalSignature = new DigitalSignature();

  public List<FileHeader> getFileHeaders() {
    return fileHeaders;
  }

  public HashMap<String, FileHeader> getFileNameHeaderMap() {
    return fileNameMap;
  }

  public void setFileHeaders(List<FileHeader> fileHeaders) {
    if(fileHeaders != null) {
      this.fileHeaders.clear();
      fileNameMap.clear();
      for(FileHeader header : fileHeaders) {
        add(header);
      }
    }
  }

  public void add(FileHeader header) {
    if(header != null) {
      fileHeaders.add(header);
      if(isStringNotNullAndNotEmpty(header.getFileName())) {
        fileNameMap.put(header.getFileName(), header);
      }
    }
  }

  public boolean remove(FileHeader header) {
    if(header != null) {
      return fileNameMap.values().remove(header) && fileHeaders.remove(header);
    }
    return false;
  }

  public void rename(FileHeader header) {
    if(header != null) {
      fileNameMap.values().remove(header);
      if(isStringNotNullAndNotEmpty(header.getFileName())) {
        fileNameMap.put(header.getFileName(), header);
      }
    }
  }

  public DigitalSignature getDigitalSignature() {
    return digitalSignature;
  }

  public void setDigitalSignature(DigitalSignature digitalSignature) {
    this.digitalSignature = digitalSignature;
  }


}
