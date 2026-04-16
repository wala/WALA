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
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Platform-specific utility functions. */
public class PlatformUtil {

  private static final java.net.URI JRT_URI = java.net.URI.create("jrt:/");
  private static final FileSystem JRT_FILE_SYSTEM = initJrtFileSystem();

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

  public static class NoJDKModulesFoundException extends Exception {

    public NoJDKModulesFoundException(String msg) {
      super(msg);
    }
  }

  /**
   * Gets the standard JDK modules shipped with the running JDK
   *
   * @param justBase if {@code true}, only include the file corresponding to the {@code java.base}
   *     module
   * @return array of {@code .jmod} module files, or an empty array if the files cannot be loaded
   * @throws IllegalStateException if the running JDK does not include jmod files
   */
  public static String[] getJDKModules(boolean justBase) throws NoJDKModulesFoundException {
    Path jmodsDir = Paths.get(System.getProperty("java.home"), "jmods");
    if (!Files.isDirectory(jmodsDir)) {
      throw new NoJDKModulesFoundException("could not find jmods directory at " + jmodsDir);
    }

    List<String> jmods;
    if (justBase) {
      Path basePath = jmodsDir.resolve("java.base.jmod");
      if (!Files.exists(basePath)) {
        throw new IllegalStateException("could not find java.base.jmod");
      }
      jmods = List.of(basePath.toString());
    } else {
      try (Stream<Path> stream = Files.list(jmodsDir)) {
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
   * Gets the standard JDK module names exposed by the running JDK image.
   *
   * @param justBase if {@code true}, only include {@code java.base}
   * @return array of module names from the {@code jrt:/modules} filesystem
   */
  public static String[] getJDKModuleNames(boolean justBase) {
    List<String> modules;
    try {
      if (justBase) {
        modules = List.of("java.base");
      } else {
        try (Stream<Path> modulePaths = Files.list(JRT_FILE_SYSTEM.getPath("modules"))) {
          modules =
              modulePaths.map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return modules.toArray(new String[0]);
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

  /** Returns the shared {@code jrt:/} filesystem for this JVM. Callers should not close it. */
  public static FileSystem getJrtFileSystem() {
    return JRT_FILE_SYSTEM;
  }

  private static FileSystem initJrtFileSystem() {
    try {
      return FileSystems.getFileSystem(JRT_URI);
    } catch (FileSystemNotFoundException e) {
      try {
        return FileSystems.newFileSystem(JRT_URI, Collections.emptyMap());
      } catch (FileSystemAlreadyExistsException ignored) {
        // Another caller won the race to install the filesystem; reuse that instance.
        return FileSystems.getFileSystem(JRT_URI);
      } catch (IOException ioException) {
        throw new IllegalStateException("unable to initialize jrt filesystem", ioException);
      }
    }
  }
}
