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
package com.ibm.wala.dynamic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Java process launcher
 * 
 * @author sfink
 */
public class JavaLauncher extends Launcher {

  /**
   * @param programArgs arguments to be passed to the Java program
   * @param mainClass Declaring class of the main() method to run.
   * @param classpathEntries  Paths that will be added to the default classpath
   */
  public static JavaLauncher make(String programArgs, String mainClass, List<String> classpathEntries) {
    return new JavaLauncher(programArgs, mainClass, true, classpathEntries, false);
  }

  /**
   * @param programArgs arguments to be passed to the Java program
   * @param mainClass Declaring class of the main() method to run.
   * @param inheritClasspath Should the spawned process inherit all classpath entries of the currently running process?
   * @param classpathEntries  Paths that will be added to the default classpath
   * @param captureOutput should the launcher capture the stdout and stderr from the subprocess?
   */
  public static JavaLauncher make(String programArgs, String mainClass, boolean inheritClasspath, List<String> classpathEntries, boolean captureOutput) {
    return new JavaLauncher(programArgs, mainClass, inheritClasspath, classpathEntries, captureOutput);
  }

  /**
   * arguments to be passed to the Java program
   */
  private String programArgs;

  /**
   * Declaring class of the main() method to run.
   */
  private final String mainClass;

  /**
   * Should the spawned process inherit all classpath entries of the currently running process?
   */
  private final boolean inheritClasspath;
  
  
  /**
   * Paths that will be added to the default classpath
   */
  private final List<String> xtraClasspath = new ArrayList<String>();

  /**
   * A {@link Thread} which spins and drains stdout of the running process.
   */
  private Thread stdOutDrain;

  /**
   * A {@link Thread} which spins and drains stderr of the running process.
   */
  private Thread stdInDrain;

  private JavaLauncher(String programArgs, String mainClass, boolean inheritClasspath, List<String> xtraClasspath, boolean captureOutput) {
    super(captureOutput);
    this.programArgs = programArgs;
    this.mainClass = mainClass;
    this.inheritClasspath = inheritClasspath;
    if (xtraClasspath != null) {
      this.xtraClasspath.addAll(xtraClasspath);
    }
  }
  
  public void setProgramArgs(String s) {
    this.programArgs = s;
  }

  public String getProgramArgs() {
    return programArgs;
  }

  public String getMainClass() {
    return mainClass;
  }

  public List<String> getXtraClassPath() {
    return xtraClasspath;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (programArgs: ");
    result.append(programArgs);
    result.append(", mainClass: ");
    result.append(mainClass);
    result.append(", xtraClasspath: ");
    result.append(xtraClasspath);
    result.append(')');
    return result.toString();
  }

  /**
   * @return the string that identifies the java executable file
   */
  protected String getJavaExe() {
    String java = System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + "java";
    return java;
  }

  /**
   * Launch the java process.
   * @throws IOException 
   * @throws IllegalArgumentException 
   */
  public Process start() throws IllegalArgumentException, IOException  {
    System.err.println(System.getProperty("user.dir"));

    String cp = makeClasspath();

    String heap = " -Xmx800M ";

    String cmd = getJavaExe() + heap + cp + " " + makeLibPath() + " " + getMainClass() + " " + getProgramArgs();

    Process p = spawnProcess(cmd);
    stdOutDrain = isCaptureOutput() ? captureStdOut(p) : drainStdOut(p);
    stdInDrain = drainStdErr(p);
    return p;
  }

  private String makeLibPath() {
    String libPath = System.getProperty("java.library.path");
    if (libPath == null) {
      return "";
    } else {
      return "-Djava.library.path=" + libPath;
    }
  }

  /**
   * Wait for the spawned process to terminate.
   */
  public void join() {
    try {
      stdOutDrain.join();
      stdInDrain.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new InternalError("Internal error in JavaLauncher.join()");
    }
    if (isCaptureOutput()) {
      Drainer d = (Drainer) stdOutDrain;
      setOutput(d.getCapture().toByteArray());
    }
  }

  /**
   * Compute the classpath for the spawned process
   */
  private String makeClasspath() {
    String cp = inheritClasspath ? System.getProperty("java.class.path") : "" ;
    if (getXtraClassPath() == null || getXtraClassPath().isEmpty()) {
      return " -classpath " + quoteStringIfNeeded(cp);
    } else {
      for (Iterator it = getXtraClassPath().iterator(); it.hasNext();) {
        cp += File.pathSeparatorChar;
        cp += (String) it.next();
      }
      return " -classpath " + quoteStringIfNeeded(cp);
    }
  }

  /**
   * If the input string contains a space, quote it (for use as a classpath).
   * TODO: Figure out how to make a Mac happy with quotes.
   * Trailing separators are unsafe, so we have to escape the last backslash
   * (if present and unescaped), so it doesn't escape the closing quote.
   */
  private String quoteStringIfNeeded(String s) {
    s = s.trim();
    // Check if there's a space.  If not, skip quoting to make Macs happy.
    // TODO: Add the check for an escaped space.
    if (s.indexOf(' ') == -1) {
      return s;
    }
    if (s.charAt(s.length()-1) == '\\' && s.charAt(s.length()-2) != '\\') {
      s += '\\';  // Escape the last backslash, so it doesn't escape the quote.
    }
    return '\"' + s + '\"';
  }

}
