/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.warnings.WalaException;

/**
 * Simple utilities for accessing files.
 */
public class FileUtil {

  /**
   * List all the files in a directory that match a regular expression
   * 
   * @param recurse recurse to subdirectories?
   * @throws IllegalArgumentException  if dir is null
   */
  public static Collection<File> listFiles(String dir, String regex, boolean recurse) {
    if (dir == null) {
      throw new IllegalArgumentException("dir is null");
    }
    File d = new File(dir);
    Pattern p = null;
    if (regex != null) {
      p = Pattern.compile(regex);
    }
    return listFiles(d, recurse, p);
  }

  private static Collection<File> listFiles(File directory, boolean recurse, Pattern p) {
    File[] files = directory.listFiles();
    if (files == null) {
      return Collections.emptyList();
    }
    HashSet<File> result = HashSetFactory.make();
    for (int i = 0; i < files.length; i++) {
      if (p == null || p.matcher(files[i].getAbsolutePath()).matches()) {
        result.add(files[i]);
      }
      if (recurse && files[i].isDirectory()) {
        result.addAll(listFiles(files[i], recurse, p));
      }
    }
    return result;
  }

  public static void copy(String srcFileName, String destFileName) throws WalaException {
    FileChannel src = null;
    FileChannel dest = null;
    try {
      src = new FileInputStream(srcFileName).getChannel();
      dest = new FileOutputStream(destFileName).getChannel();
      long n = src.size();
      MappedByteBuffer buf = src.map(FileChannel.MapMode.READ_ONLY, 0, n);
      dest.write(buf);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new WalaException("Failed to copy " + srcFileName + " to " + destFileName);
    } catch (IOException e) {
      e.printStackTrace();
      throw new WalaException("Failed to copy " + srcFileName + " to " + destFileName);
    } finally {
      if (dest != null) {
        try {
          dest.close();
        } catch (IOException e1) {
        }
      }
      if (src != null) {
        try {
          src.close();
        } catch (IOException e1) {
        }
      }
    }
  }

  /**
   * delete all files (recursively) in a directory.
   * This is dangerous. Use with care.
   */
  public static void deleteContents(String directory) throws WalaException {
    Collection fl = listFiles(directory, null, true);

    for (Iterator it = fl.iterator(); it.hasNext();) {
      File f = (File) it.next();
      if (!f.isDirectory()) {
        f.delete();
      }
    }
    do {
      Collection f2 = listFiles(directory, null, true);

      for (Iterator it = f2.iterator(); it.hasNext();) {
        File f = (File) it.next();
        f.delete();
      }
    } while (listFiles(directory, null, true).size() > 0);

  }
}