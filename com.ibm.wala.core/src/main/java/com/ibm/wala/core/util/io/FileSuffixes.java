/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.util.io;

import java.net.URI;

/** Some simple utilities used to manipulate Strings */
public class FileSuffixes {

  private static final String CLASS_SUFFIX = ".class";

  private static final String JAR_SUFFIX = ".jar";

  private static final String WAR_SUFFIX = ".war";

  private static final String DEX_SUFFIX = ".dex";
  private static final String APK_SUFFIX = ".apk";

  /**
   * Does the URI refer to a .dex file?
   *
   * @throws IllegalArgumentException if uri is null
   */
  public static boolean isDexFile(final URI uri) {
    if (uri == null) {
      throw new IllegalArgumentException("uri is null");
    }

    if (uri.toString().startsWith("jar:")) {
      try {
        final String filePart = uri.toURL().getFile().toLowerCase();
        return isDexFile(filePart);
      } catch (java.net.MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
    } else {
      assert (uri.getPath() != null);
      return isDexFile(uri.getPath());
    }
  }

  /**
   * Does the file name represent a .dex file?
   *
   * @param fileName name of a file
   * @return boolean
   * @throws IllegalArgumentException if fileName is null
   */
  public static boolean isDexFile(String fileName) {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    return fileName.toLowerCase().endsWith(DEX_SUFFIX);
  }

  /**
   * Does the file name represent a .dex file?
   *
   * @param fileName name of a file
   * @return boolean
   * @throws IllegalArgumentException if fileName is null
   */
  public static boolean isApkFile(String fileName) {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    return fileName.toLowerCase().endsWith(APK_SUFFIX);
  }

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
   * <p>TODO: generalize for all suffixes
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

  /** Does the URI point to a ressource in a jar-file */
  public static boolean isRessourceFromJar(final URI uri) {
    return uri.toString().startsWith("jar:"); // How Pretty
  }
}
