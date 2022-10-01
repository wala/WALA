/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Platform-specific utility functions. */
public class PlatformUtil {

  /** are we running on Mac OS X? */
  public static boolean onMacOSX() {
    String osname = System.getProperty("os.name");
    return osname.toLowerCase().contains("mac");
    // return System.getProperty("mrj.version") != null;
  }

  /** are we running on Linux? */
  public static boolean onLinux() {
    String osname = System.getProperty("os.name");
    return osname.equalsIgnoreCase("linux");
  }

  /** are we running on Windows? */
  public static boolean onWindows() {
    String osname = System.getProperty("os.name");
    return osname.toLowerCase().contains("windows");
  }

  /** are we running on IKVM? see http://www.ikvm.net */
  public static boolean onIKVM() {
    return "IKVM.NET".equals(System.getProperty("java.runtime.name"));
  }

  /**
   * get the jars in the boot classpath. TODO test on more JVMs
   *
   * @throws IllegalStateException if boot classpath cannot be found
   */
  public static String[] getBootClassPathJars() {
    String classpath = null;
    String javaVersion = System.getProperty("java.specification.version");
    if (!javaVersion.equals("1.8")) {
      // java11 support for jmod files
      try (Stream<Path> stream = Files.list(Paths.get(System.getProperty("java.home"), "jmods"))) {
        classpath =
            String.join(
                File.pathSeparator, stream.map(Path::toString).collect(Collectors.toList()));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    } else {
      classpath = System.getProperty("sun.boot.class.path");
    }
    if (classpath == null) {
      throw new IllegalStateException("could not find boot classpath");
    }
    String[] jars = classpath.split(File.pathSeparator);
    ArrayList<String> result = new ArrayList<>();
    for (String jar : jars) {
      if ((jar.endsWith(".jar") || jar.endsWith(".jmod")) && new File(jar).exists()) {
        result.add(jar);
      }
    }
    return result.toArray(new String[0]);
  }

  /** @return the major version of the Java runtime we are running on. */
  public static int getJavaRuntimeVersion() {
    String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf(".");
      if (dot != -1) {
        version = version.substring(0, dot);
      }
    }
    return Integer.parseInt(version);
  }
}
