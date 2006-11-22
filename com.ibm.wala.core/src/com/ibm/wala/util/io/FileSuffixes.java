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
 * 
 * Some simple utilities used to manipulate Strings
 * 
 * @author sfink
 */
public class FileSuffixes {

  private static final String CLASS_SUFFIX = ".class";
  private static final String JAR_SUFFIX = ".jar";
  private static final String WAR_SUFFIX = ".war";
  private static final String CLASSPATH = ".classpath";

  /**
   * Does the file name represent a .class file?
   * 
   * @param fileName
   *          name of a file
   * @return boolean
   */
  public static boolean isClassFile(String fileName) {
    int suffixIndex = fileName.indexOf(CLASS_SUFFIX);
    if (suffixIndex > -1 && fileName.indexOf(CLASSPATH) == -1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Does the file name represent a .java file?
   * 
   * @param fileName
   *          name of a file
   * @return boolean
   */
  public static boolean isSourceFile(String fileName) {
    int suffixIndex = fileName.indexOf(".java");
    if (suffixIndex > -1 && fileName.indexOf(CLASSPATH) == -1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Does the file name represent a .jar file?
   * 
   * @param fileName
   *          name of a file
   * @return boolean
   */
  public static boolean isJarFile(String fileName) {
    int suffixIndex = fileName.indexOf(JAR_SUFFIX);
    if (suffixIndex > -1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Does the file name represent a .war file?
   * 
   * @param fileName
   *          name of a file
   * @return boolean
   */
  public static boolean isWarFile(String fileName) {
    int suffixIndex = fileName.indexOf(WAR_SUFFIX);
    if (suffixIndex > -1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Strip the ".class" or ".java" suffix from a file name
   * 
   * TODO: generalize for all suffixes
   * 
   * @param fileName
   *          the file name
   */
  public static String stripSuffix(String fileName) {
    int suffixIndex = fileName.indexOf(CLASS_SUFFIX);
    suffixIndex = (suffixIndex > -1) ? suffixIndex : fileName.indexOf(".java");
    if (suffixIndex > -1) {
      return fileName.substring(0, suffixIndex);
    } else {
      return fileName;
    }
  }
}