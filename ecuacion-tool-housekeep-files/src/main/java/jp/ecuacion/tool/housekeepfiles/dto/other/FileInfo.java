/*
 * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.ecuacion.tool.housekeepfiles.dto.other;

/**
 * Stores file info.
 */
public class FileInfo {
  private String filePath;
  private boolean isDirectory;
  private long lastUpdTimeInMillis;
  private boolean isLocked;

  /**
   * Constructs a new instance.
   */
  public FileInfo() {

  }

  /**
   * Constructs a new instance.
   */
  public FileInfo(String filePath, boolean isDirectory) {
    this.filePath = filePath;
    this.isDirectory = isDirectory;
  }

  /**
   * Constructs a new instance.
   */
  public FileInfo(String filePath, boolean isDirectory, long lastUpdTimeInMillis,
      boolean isLocked) {
    this.filePath = filePath;
    this.isDirectory = isDirectory;
    this.lastUpdTimeInMillis = lastUpdTimeInMillis;
    this.isLocked = isLocked;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  public void setDirectory(boolean isDirectory) {
    this.isDirectory = isDirectory;
  }

  public long getLastUpdTimeInMillis() {
    return lastUpdTimeInMillis;
  }

  public void setLastUpdTimeInMillis(long lastUpdTimeInMillis) {
    this.lastUpdTimeInMillis = lastUpdTimeInMillis;
  }

  public boolean isLocked() {
    return isLocked;
  }

  public void setLocked(boolean isLocked) {
    this.isLocked = isLocked;
  }
}
