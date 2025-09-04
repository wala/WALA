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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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

  /** are we running on <a href="http://www.ikvm.net">IKVM</a>? */
  public static boolean onIKVM() {
    return "IKVM.NET".equals(System.getProperty("java.runtime.name"));
  }

  /**
   * Gets the standard JDK modules shipped with the running JDK
   *
   * @param justBase if {@code true}, only include the file corresponding to the {@code java.base}
   *     module
   * @return array of {@code .jmod} module files
   * @throws IllegalStateException if modules cannot be found
   */
  public static String[] getJDKModules(boolean justBase) {
    List<String> jmods;
    if (justBase) {
      Path basePath = Paths.get(System.getProperty("java.home"), "jmods", "java.base.jmod");
      if (!Files.exists(basePath)) {
        throw new IllegalStateException("could not find java.base.jmod");
      }
      jmods = List.of(basePath.toString());
    } else {
      try (Stream<Path> stream = Files.list(Paths.get(System.getProperty("java.home"), "jmods"))) {
        jmods =
            stream
                .map(Path::toString)
                .filter(p -> p.endsWith(".jmod"))
                .collect(Collectors.toList());
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return jmods.toArray(new String[0]);
  }

  /**
   * Returns the filesystem path for a JDK module from the running JVM
   *
   * @param moduleName name of the module, e.g., {@code "java.sql"}
   * @return path to the module
   */
  public static Path getPathForJDKModule(String moduleName) {
    return Paths.get(System.getProperty("java.home"), "jmods", moduleName + ".jmod");
  }

  /**
   * @return the major version of the Java runtime we are running on.
   */
  public static int getJavaRuntimeVersion() {
    String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf('.');
      if (dot != -1) {
        version = version.substring(0, dot);
      }
    }
    return Integer.parseInt(version);
  }
}
