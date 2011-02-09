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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

import com.ibm.wala.util.collections.HashSetFactory;

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

  public static void copy(String srcFileName, String destFileName)  throws IOException {
    if (srcFileName == null) {
      throw new IllegalArgumentException("srcFileName is null");
    }
    if (destFileName == null) {
      throw new IllegalArgumentException("destFileName is null");
    }
    FileChannel src = null;
    FileChannel dest = null;
    try {
      src = new FileInputStream(srcFileName).getChannel();
      dest = new FileOutputStream(destFileName).getChannel();
      long n = src.size();
      MappedByteBuffer buf = src.map(FileChannel.MapMode.READ_ONLY, 0, n);
      dest.write(buf);
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
   * @throws IOException if there's a problem deleting some file
   */
  public static void deleteContents(String directory) throws IOException  {
    Collection<File> fl = listFiles(directory, null, true);

    for (Iterator<File> it = fl.iterator(); it.hasNext();) {
      File f = (File) it.next();
      // add the check for f.exists(), just in case there's a race going on.
      if (!f.isDirectory() && f.exists()) {
        boolean result = f.delete();
        if (!result) {
          throw new IOException("Failed to delete " + f);
        }
      }
    } 
    int lastCount = Integer.MAX_VALUE;
    do {
      Collection<File> f2 = listFiles(directory, null, true);
      if (f2.size() == lastCount) {
        throw new IOException("got stuck deleting directories. Probably some other process is preventing deletion.");
      }
      lastCount = f2.size();

      for (Iterator<File> it = f2.iterator(); it.hasNext();) {
        File f = (File) it.next();
        f.delete();
      }
    } while (listFiles(directory, null, true).size() > 0);

  }

  /**
   * Create a {@link FileOutputStream} corresponding to a particular file name. Delete the existing file if one exists.
   */
  public static final FileOutputStream createFile(String fileName) throws IOException {
    if (fileName == null) {
      throw new IllegalArgumentException("null file");
    }
    File f = new File(fileName);
    if (f.getParentFile() != null && !f.getParentFile().exists()) {
      boolean result = f.getParentFile().mkdirs();
      if (!result) {
        throw new IOException("failed to create " + f.getParentFile());
      }
    }
    if (f.exists()) {
      f.delete();
    }
    boolean result = f.createNewFile();
    if (!result) {
      throw new IOException("failed to create " + f);
    }
    return new FileOutputStream(f);
  }

  /**
   * read fully the contents of s and return a byte array holding the result
   * @throws IOException
   */
  public static byte[] readBytes(InputStream s) throws IOException {
    if (s == null) {
      throw new IllegalArgumentException("null s");
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] b = new byte[1024];
    int n = s.read(b);
    while (n != -1) {
      out.write(b, 0, n);
      n = s.read(b);
    }
    byte[] bb = out.toByteArray();
    return bb;
  }
}