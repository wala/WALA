/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Simple utilities for accessing files.
 */
public class FileUtil  {
  

  public static Collection<File> listFiles(String dir, String regex, boolean recurse) {
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
    HashSet<File> result = new HashSet<File>();
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
} 