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

/**
 * Some simple utilities used to manipulate Strings
 */
public class FileSuffixes {

  private static final String CLASS_SUFFIX = ".class";

  private static final String JAR_SUFFIX = ".jar";

  private static final String WAR_SUFFIX = ".war";

  /**
   * Does the file name represent a .class file?
   * 
   * @param fileName name of a file
   * @return boolean
   * @throws IllegalArgumentException if fileName is null
   */
  public static boolean isClassFile(String fileName) {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    return fileName.endsWith(CLASS_SUFFIX);
  }

  /**
   * Does the file name represent a .java file?
   * 
   * @param fileName name of a file
   * @return boolean
   * @throws IllegalArgumentException if fileName is null
   */
  public static boolean isSourceFile(String fileName) {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    return fileName.endsWith(".java");
  }

  /**
   * Does the file name represent a .jar file?
   * 
   * @param fileName name of a file
   * @return boolean
   * @throws IllegalArgumentException if fileName is null
   */
  public static boolean isJarFile(String fileName) {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    return fileName.endsWith(JAR_SUFFIX);
  }

  /**
   * Does the file name represent a .war file?
   * 
   * @param fileName name of a file
   * @return boolean
   * @throws IllegalArgumentException if fileName is null
   */
  public static boolean isWarFile(String fileName) {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    return fileName.endsWith(WAR_SUFFIX);
  }

  /**
   * Strip the ".class" or ".java" suffix from a file name
   * 
   * TODO: generalize for all suffixes
   * 
   * @param fileName the file name
   * @throws IllegalArgumentException if fileName is null
   */
  public static String stripSuffix(String fileName) {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    int suffixIndex = fileName.indexOf(CLASS_SUFFIX);
    suffixIndex = (suffixIndex > -1) ? suffixIndex : fileName.indexOf(".java");
    if (suffixIndex > -1) {
      return fileName.substring(0, suffixIndex);
    } else {
      return fileName;
    }
  }
}