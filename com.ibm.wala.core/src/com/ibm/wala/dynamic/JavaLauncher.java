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
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.util.warnings.WalaException;

/**
 * A Java process launcher
 * 
 * @author sfink
 */
public class JavaLauncher extends Launcher {

  public static JavaLauncher make(String programArgs, String mainClass, List<String> classpathEntries) {
    return new JavaLauncher(programArgs, mainClass, classpathEntries);
  }

  private final String programArgs;

  private final String mainClass;

  /**
   * Paths that will be added to the current process's classpath
   */
  private final List<String> xtraClasspath;
  
  private Thread stdOutDrain;
  
  private Thread stdInDrain;

  private JavaLauncher(String programArgs, String mainClass, List<String> xtraClasspath) {
    super();
    this.programArgs = programArgs;
    this.mainClass = mainClass;
    this.xtraClasspath = xtraClasspath;
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
   * @return the string that identifies the java.exe file
   */
  protected String getJavaExe() {
    String java = System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + "java.exe";
    return java;
  }

  /**
   * Launch the java process.
   * @throws WalaException
   */
  public Process start() throws WalaException{
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
   * @throws WalaException
   */
  public void join() throws WalaException {
    try {
      stdOutDrain.join();
      stdInDrain.join();
    } catch (InterruptedException e) {
      throw new WalaException("Internal error", e);
    }
    if (isCaptureOutput()) {
      Drainer d = (Drainer) stdOutDrain;
      setOutput(d.getCapture().toByteArray());
    }
  }
  
  private String makeClasspath() {
    String cp = " -classpath " + System.getProperty("java.class.path");
    if (getXtraClassPath() == null || getXtraClassPath().isEmpty()) {
      return cp;
    } else {
      for (Iterator it = getXtraClassPath().iterator(); it.hasNext();) {
        cp += (String) it.next();
        if (it.hasNext()) {
          cp += ";";
        }
      }
      cp += " ";
      return cp;
    }
  }
}
