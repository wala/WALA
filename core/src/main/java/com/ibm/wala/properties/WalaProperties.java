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
package com.ibm.wala.properties;

import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;

public final class WalaProperties {

  public static final String WALA_REPORT = "WALA_report"; // $NON-NLS-1$

  public static final String INPUT_DIR = "input_dir"; // $NON-NLS-1$

  public static final String OUTPUT_DIR = "output_dir"; // $NON-NLS-1$

  public static final String J2SE_DIR = "java_runtime_dir"; // $NON-NLS-1$

  public static final String J2EE_DIR = "j2ee_runtime_dir"; // $NON-NLS-1$

  public static final String ECLIPSE_PLUGINS_DIR = "eclipse_plugins_dir"; // $NON-NLS-1$

  public static final String ANDROID_RT_DEX_DIR = "android_rt_dir";

  public static final String ANDROID_RT_JAVA_JAR = "android_rt_jar";

  public static final String ANDROID_DEX_TOOL = "android_dx_tool";

  public static final String ANDROID_APK_TOOL = "android_apk_tool";

  public static final String DROIDEL_TOOL = "droidel_tool";

  public static final String DROIDEL_ANDROID_JAR = "droidel_android_jar";

  /**
   * Determine the classpath noted in wala.properties for J2SE standard libraries
   *
   * <p>If wala.properties cannot be loaded, returns jar files in boot classpath.
   *
   * @throws IllegalStateException if jar files cannot be discovered
   * @see PlatformUtil#getBootClassPathJars()
   */
  public static String[] getJ2SEJarFiles() {
    Properties p = null;
    try {
      p = WalaProperties.loadProperties();
    } catch (WalaException e) {
      return PlatformUtil.getBootClassPathJars();
    }

    String dir = p.getProperty(WalaProperties.J2SE_DIR);
    if (dir == null) {
      return PlatformUtil.getBootClassPathJars();
    }
    if (!new File(dir).isDirectory()) {
      System.err.println(
          "WARNING: java_runtime_dir "
              + dir
              + " in wala.properties is invalid.  Using boot class path instead.");
      return PlatformUtil.getBootClassPathJars();
    }
    return getJarsInDirectory(dir);
  }

  /**
   * @return names of the Jar files holding J2EE libraries
   * @throws IllegalStateException if the J2EE_DIR property is not set
   */
  public static String[] getJ2EEJarFiles() {
    Properties p = null;
    try {
      p = WalaProperties.loadProperties();
    } catch (WalaException e) {
      e.printStackTrace();
      throw new IllegalStateException("problem loading wala.properties");
    }
    String dir = p.getProperty(WalaProperties.J2EE_DIR);
    if (dir == null) {
      throw new IllegalStateException("No J2EE directory specified");
    }
    return getJarsInDirectory(dir);
  }

  public static String[] getJarsInDirectory(String dir) {
    File f = new File(dir);
    Assertions.productionAssertion(f.isDirectory(), "not a directory: " + dir);
    Collection<File> col = FileUtil.listFiles(dir, ".*\\.jar$", true);
    String[] result = new String[col.size()];
    int i = 0;
    for (File jarFile : col) {
      result[i++] = jarFile.getAbsolutePath();
    }
    return result;
  }

  static final String PROPERTY_FILENAME = "wala.properties"; // $NON-NLS-1$

  public static Properties loadProperties() throws WalaException {
    try {
      Properties result =
          loadPropertiesFromFile(WalaProperties.class.getClassLoader(), PROPERTY_FILENAME);
      String outputDir = result.getProperty(OUTPUT_DIR, DefaultPropertiesValues.DEFAULT_OUTPUT_DIR);
      result.setProperty(OUTPUT_DIR, convertToAbsolute(outputDir));

      String walaReport =
          result.getProperty(WALA_REPORT, DefaultPropertiesValues.DEFAULT_WALA_REPORT_FILENAME);
      result.setProperty(WALA_REPORT, convertToAbsolute(walaReport));

      return result;
    } catch (Exception e) {
      //      e.printStackTrace();
      throw new WalaException("Unable to set up wala properties ", e);
    }
  }

  static String convertToAbsolute(String path) {
    final File file = new File(path);
    return file.isAbsolute()
        ? file.getAbsolutePath()
        : WalaProperties.getWalaHomeDir().concat(File.separator).concat(path);
  }

  public static Properties loadPropertiesFromFile(ClassLoader loader, String fileName)
      throws IOException {
    if (loader == null) {
      throw new IllegalArgumentException("loader is null");
    }
    if (fileName == null) {
      throw new IllegalArgumentException("null fileName");
    }
    try (final InputStream propertyStream = loader.getResourceAsStream(fileName)) {
      if (propertyStream == null) {
        // create default properties
        Properties defprop = new Properties();
        defprop.setProperty(OUTPUT_DIR, "./out");
        defprop.setProperty(INPUT_DIR, "./in");
        defprop.setProperty(ECLIPSE_PLUGINS_DIR, "./plugins");
        defprop.setProperty(WALA_REPORT, "./wala_report.txt");
        defprop.setProperty(J2EE_DIR, "./j2ee");

        return defprop;
      }
      Properties result = new Properties();
      result.load(propertyStream);

      return result;
    }
  }

  /**
   * @deprecated because when running under eclipse, there may be no such directory. Need to handle
   *     that case.
   */
  @Deprecated
  public static String getWalaHomeDir() {
    final String envProperty = System.getProperty("WALA_HOME"); // $NON-NLS-1$
    if (envProperty != null) return envProperty;

    final URL url =
        WalaProperties.class.getClassLoader().getResource("wala.properties"); // $NON-NLS-1$
    if (url == null) {
      return System.getProperty("user.dir"); // $NON-NLS-1$
    } else {
      return new File(new FileProvider().filePathFromURL(url))
          .getParentFile()
          .getParentFile()
          .getPath();
    }
  }
}
