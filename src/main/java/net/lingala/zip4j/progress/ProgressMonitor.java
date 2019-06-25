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

package net.lingala.zip4j.progress;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * If Zip4j is set to run in thread mode, this class helps retrieve current progress
 */
@Data
public class ProgressMonitor {

  public enum State { READY, BUSY }
  public enum Result { SUCCESS, WORK_IN_PROGRESS, ERROR, CANCELLED }
  public enum Task { NONE, ADD_ENTRY, REMOVE_ENTRY, CALCULATE_CRC, EXTRACT_ENTRY, MERGE_ZIP_FILES, SET_COMMENT}

  private State state;
  private long totalWork;
  @Setter(AccessLevel.NONE)
  private long workCompleted;
  private int percentDone;
  private Task currentTask;
  private String fileName;
  private Result result;
  private Exception exception;
  private boolean cancelAllTasks;
  private boolean pause;

  public ProgressMonitor() {
    reset();
  }

  public void updateWorkCompleted(long workCompleted) {
    this.workCompleted += workCompleted;

    if (totalWork > 0) {
      percentDone = (int) ((this.workCompleted * 100 / totalWork));
      if (percentDone > 100) {
        percentDone = 100;
      }
    }

    while (pause) {
      try {
        Thread.sleep(150);
      } catch (InterruptedException e) {
        //Do nothing
      }
    }
  }

  public void endProgressMonitor() {
    result = Result.SUCCESS;
    percentDone = 100;
    reset();
  }

  public void endProgressMonitor(Exception e) {
    result = Result.ERROR;
    exception = e;
    reset();
  }

  private void reset() {
    currentTask = Task.NONE;
    state = State.READY;
    fileName = null;
    totalWork = 0;
    workCompleted = 0;
    percentDone = 0;
  }
}
