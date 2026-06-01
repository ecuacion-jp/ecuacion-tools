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
package jp.ecuacion.tool.housekeepfiles.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Provides zip and unzip function.
 */
public class CompressUtil {

  /** default encoding is set to "UTF-8". */
  public void zipDirectory(String fromDirPath, String toFilePath) throws IOException {
    zipDirectory(fromDirPath, toFilePath, "UTF-8");
  }

  /**
   * Zip files in specified directory and create zip file.
   *
   * @param fromDirPath directory to be archived (for example: C:/sample)
   * @param toFilePath zipfile fullpath (for example: C:/sample.zip)
   * @param encoding "Shift-JIS", "UTF-8", etc...
   */
  public void zipDirectory(String fromDirPath, String toFilePath, String encoding)
      throws IOException {
    File baseFile = new File(toFilePath);
    File file = new File(fromDirPath);
    try (ZipOutputStream outZip =
        new ZipOutputStream(new FileOutputStream(baseFile), Charset.forName(encoding))) {
      archive(outZip, baseFile, file);
    }
  }


  /** default encoding is set to "UTF-8". */
  public void zipFile(String fromFilePath, String toFilePath) throws IOException {
    zipFile(fromFilePath, toFilePath, "UTF-8");
  }

  /**
   * Zip specified file.
   */
  public void zipFile(String fromFilePath, String toFilePath, String encoding) throws IOException {
    List<String> arr = new ArrayList<String>();
    arr.add(fromFilePath);
    zipFileList(arr, toFilePath, encoding);
  }

  /** default encoding is set to "UTF-8". */
  public void zipFileList(List<String> fromFileList, String filePath) throws IOException {
    zipFileList(fromFileList, filePath, "UTF-8");
  }

  /**
   * Executes zip-archive files specified by ArrayList and create zip file.
   *
   * @param filePath zipfile fullpath (for example: C:/sample.zip)
   * @param fromFileList file list to archive (for example: {C:/sample1.txt, C:/sample2.txt})
   */
  public void zipFileList(List<String> fromFileList, String filePath, String encoding)
      throws IOException {
    File baseFile = new File(filePath);
    try (ZipOutputStream outZip =
        new ZipOutputStream(new FileOutputStream(baseFile), Charset.forName(encoding))) {
      // Compress files in the list sequentially.
      for (int i = 0; i < fromFileList.size(); i++) {
        File file = new File(fromFileList.get(i));
        archive(outZip, file, file.getName());
      }
    }
  }

  /*
   * Recursive processing for directory compression.
   * baseFile is the output ZIP file and targetFile is the file or directory to compress.
   */
  private void archive(ZipOutputStream outZip, File baseFile, File targetFile) throws IOException {
    if (targetFile.isDirectory()) {
      File[] files = targetFile.listFiles();
      for (File f : files) {
        if (f.isDirectory()) {
          archive(outZip, f, f.getAbsolutePath().replace(baseFile.getParent(), "").substring(1));
          archive(outZip, baseFile, f);
        } else {
          if (!f.getAbsoluteFile().equals(baseFile)) {
            // Compress.
            archive(outZip, f, f.getAbsolutePath().replace(baseFile.getParent(), "").substring(1));
          }
        }
      }
    }
  }

  /**
   * Provides {@code zip} function.
   *
   * @param outZip ZipOutputStream outputStream
   * @param targetFile The file you want to zip
   * @param entryName saved zip file name
   */
  private void archive(ZipOutputStream outZip, File targetFile, String entryName)
      throws IOException {
    // Set compression level.
    outZip.setLevel(5);

    // Create ZIP entry.
    String entryNameForZipUtil = targetFile.isDirectory() ? entryName + "/" : entryName;
    ZipEntry ze = new ZipEntry(entryNameForZipUtil);
    ze.setTime(targetFile.lastModified());
    outZip.putNextEntry(ze);

    if (!targetFile.isDirectory()) {
      // Get input stream for the file to compress.
      BufferedInputStream in = new BufferedInputStream(new FileInputStream(targetFile));
      // Write the file to the ZIP output.
      int readSize = 0;
      // Read buffer.
      byte[] buffer = new byte[1024];
      while ((readSize = in.read(buffer, 0, buffer.length)) != -1) {
        outZip.write(buffer, 0, readSize);
      }
      // Close.
      in.close();
    }
    // Close ZIP entry.
    outZip.closeEntry();
  }

  /**
   * Provides {@code unzip} function.
   * 
   * <p>This method uses java.util.zip.ZipEntry considering backward compatibility.
   *     Notice that this app mainly uses org.apache.tools.zip.ZipEntry.</p>
   */
  public void unzip(String fromFullFilePath, String toFullDirPath) throws IOException {
    // Unzip.
    try (ZipFile zf = new ZipFile(fromFullFilePath);) {

      for (Enumeration<? extends java.util.zip.ZipEntry> e = zf.entries(); e.hasMoreElements();) {
        java.util.zip.ZipEntry ze = e.nextElement();
        if (ze.isDirectory()) {
          new File(toFullDirPath + ze.getName()).mkdirs();

        } else {
          try (InputStream input = zf.getInputStream(ze);
              FileOutputStream output = new FileOutputStream(toFullDirPath + ze.getName())) {

            byte[] buf = new byte[256];
            int len;

            while ((len = input.read(buf)) != -1) {
              output.write(buf, 0, len);
            }
          } catch (IOException ioe) {
            throw new RuntimeException(ioe);
          }
        }
      }
    }
  }
}
