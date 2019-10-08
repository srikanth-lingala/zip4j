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

/**
 * If Zip4j is set to run in thread mode, this class helps retrieve current progress
 */
public class ProgressMonitor {

  public enum State { READY, BUSY }
  public enum Result { SUCCESS, WORK_IN_PROGRESS, ERROR, CANCELLED }
  public enum Task { NONE, ADD_ENTRY, REMOVE_ENTRY, CALCULATE_CRC, EXTRACT_ENTRY, MERGE_ZIP_FILES, SET_COMMENT, RENAME_FILE}

  private State state;
  private long totalWork;
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

  public void fullReset() {
    reset();
    fileName = null;
    totalWork = 0;
    workCompleted = 0;
    percentDone = 0;
  }

  private void reset() {
    currentTask = Task.NONE;
    state = State.READY;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public long getTotalWork() {
    return totalWork;
  }

  public void setTotalWork(long totalWork) {
    this.totalWork = totalWork;
  }

  public long getWorkCompleted() {
    return workCompleted;
  }

  public int getPercentDone() {
    return percentDone;
  }

  public void setPercentDone(int percentDone) {
    this.percentDone = percentDone;
  }

  public Task getCurrentTask() {
    return currentTask;
  }

  public void setCurrentTask(Task currentTask) {
    this.currentTask = currentTask;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public Result getResult() {
    return result;
  }

  public void setResult(Result result) {
    this.result = result;
  }

  public Exception getException() {
    return exception;
  }

  public void setException(Exception exception) {
    this.exception = exception;
  }

  public boolean isCancelAllTasks() {
    return cancelAllTasks;
  }

  public void setCancelAllTasks(boolean cancelAllTasks) {
    this.cancelAllTasks = cancelAllTasks;
  }

  public boolean isPause() {
    return pause;
  }

  public void setPause(boolean pause) {
    this.pause = pause;
  }
}
