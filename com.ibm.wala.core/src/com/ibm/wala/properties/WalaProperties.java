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
package com.ibm.wala.properties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;

import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.io.FileUtil;

public final class WalaProperties {

  public static final String WALA_REPORT = "WALA_report"; //$NON-NLS-1$

  public static final String INPUT_DIR = "input_dir"; //$NON-NLS-1$

  public static final String OUTPUT_DIR = "output_dir"; //$NON-NLS-1$

  public final static String J2SE_DIR = "java_runtime_dir"; //$NON-NLS-1$

  public final static String J2EE_DIR = "j2ee_runtime_dir"; //$NON-NLS-1$

  public final static String ECLIPSE_PLUGINS_DIR = "eclipse_plugins_dir"; //$NON-NLS-1$

  public final static String ANDROID_RT_DEX_DIR = "android_rt_dir";

  public final static String ANDROID_RT_JAVA_JAR = "android_rt_jar";

  public final static String ANDROID_DEX_TOOL = "android_dx_tool";

  public final static String ANDROID_APK_TOOL = "android_apk_tool";
  
  public final static String DROIDEL_TOOL = "droidel_tool";
  
  public final static String DROIDEL_ANDROID_JAR = "droidel_android_jar";

  /**
   * Determine the classpath noted in wala.properties for J2SE standard libraries
   * 
   * If wala.properties cannot be loaded, returns jar files in boot classpath.
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
    if (dir == null || !(new File(dir)).isDirectory()) {
      System.err.println("WARNING: java_runtime_dir " + dir + " in wala.properties is invalid.  Using boot class path instead.");
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

  final static String PROPERTY_FILENAME = "wala.properties"; //$NON-NLS-1$

  public static Properties loadProperties() throws WalaException {
    try {
      Properties result = loadPropertiesFromFile(WalaProperties.class.getClassLoader(), PROPERTY_FILENAME);

      String outputDir = result.getProperty(OUTPUT_DIR, DefaultPropertiesValues.DEFAULT_OUTPUT_DIR);
      result.setProperty(OUTPUT_DIR, convertToAbsolute(outputDir));

      String walaReport = result.getProperty(WALA_REPORT, DefaultPropertiesValues.DEFAULT_WALA_REPORT_FILENAME);
      result.setProperty(WALA_REPORT, convertToAbsolute(walaReport));

      return result;
    } catch (Exception e) {
//      e.printStackTrace();
      throw new WalaException("Unable to set up wala properties ", e);
    }
  }

  static String convertToAbsolute(String path) {
    final File file = new File(path);
    return (file.isAbsolute()) ? file.getAbsolutePath() : WalaProperties.getWalaHomeDir().concat(File.separator).concat(path);
  }

  public static Properties loadPropertiesFromFile(ClassLoader loader, String fileName) throws IOException {
    if (loader == null) {
      throw new IllegalArgumentException("loader is null");
    }
    if (fileName == null) {
      throw new IllegalArgumentException("null fileName");
    }
    final InputStream propertyStream = loader.getResourceAsStream(fileName);
    if (propertyStream == null) {
      // create default properties
      Properties defprop = new Properties();
      defprop.setProperty(OUTPUT_DIR, "./out");
      defprop.setProperty(INPUT_DIR, "./in");
      defprop.setProperty(ECLIPSE_PLUGINS_DIR, "./plugins");
      defprop.setProperty(WALA_REPORT, "./wala_report.txt");
      defprop.setProperty(J2EE_DIR, "./j2ee");
      final String j2selib = guessJavaLib();
      defprop.setProperty(J2SE_DIR, j2selib);
      
      return defprop;
    }
    Properties result = new Properties();
    result.load(propertyStream);
    
    if (!result.containsKey(J2SE_DIR)) {
      final String j2selib = guessJavaLib();
      result.setProperty(J2SE_DIR, j2selib);
    }
    return result;
  }

  public static String guessJavaLib() throws IOException {
    final Properties p = System.getProperties();
    final String home = System.getProperty("java.home");
    final String bestGuess = home + File.separator + "lib";
    final String os = p.getProperty("os.name");

    if (os.contains("Mac OS X")) {
      final File f = new File(bestGuess);
      if (f.exists() && f.isDirectory()) {
        final File rt = new File(bestGuess + File.separator + "rt.jar");
        if (rt.exists() && rt.isFile()) {
          return bestGuess;
        }
      }

      // no rt.jar? try old osx java version that have their runtime libraries at a different location.
      final File guess1 = new File("/System/Library/Frameworks/JavaVM.framework/Classes");
      if (guess1.exists() && guess1.isDirectory()) {
        return "/System/Library/Frameworks/JavaVM.framework/Classes";
      }

      // no luck either? too bad
      throw new IOException("Could not guess java.home for OSX. "
          + "Please create a wala.properties file and set it manually.");
    } else {
      return bestGuess;
    }
  }
  
  /**
   * @deprecated because when running under eclipse, there may be no such directory.
   * Need to handle that case.
   */
  @Deprecated
  public static String getWalaHomeDir() {
    final String envProperty = System.getProperty("WALA_HOME"); //$NON-NLS-1$
    if (envProperty != null)
      return envProperty;

    final URL url = WalaProperties.class.getClassLoader().getResource("wala.properties"); //$NON-NLS-1$
    if (url == null) {
      return System.getProperty("user.dir"); //$NON-NLS-1$
    } else {
      return new File((new FileProvider()).filePathFromURL(url)).getParentFile().getParentFile().getPath();
    }
  }
  
}
