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
package com.ibm.wala.util.processes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * A Java process launcher
 */
public class JavaLauncher extends Launcher {

  /**
   * @param programArgs
   *          arguments to be passed to the Java program
   * @param mainClass
   *          Declaring class of the main() method to run.
   * @param classpathEntries
   *          Paths that will be added to the default classpath
   */
  public static JavaLauncher make(String programArgs, String mainClass, List<String> classpathEntries, Logger logger) {
    return new JavaLauncher(programArgs, mainClass, true, classpathEntries, false, false, logger);
  }

  /**
   * @param programArgs
   *          arguments to be passed to the Java program
   * @param mainClass
   *          Declaring class of the main() method to run.
   * @param inheritClasspath
   *          Should the spawned process inherit all classpath entries of the
   *          currently running process?
   * @param classpathEntries
   *          Paths that will be added to the default classpath
   * @param captureOutput
   *          should the launcher capture the stdout from the subprocess?
   * @param captureErr
   *          should the launcher capture the stderr from the subprocess?
   */
  public static JavaLauncher make(String programArgs, String mainClass, boolean inheritClasspath, List<String> classpathEntries,
      boolean captureOutput, boolean captureErr, Logger logger) {
    if (mainClass == null) {
      throw new IllegalArgumentException("null mainClass");
    }
    return new JavaLauncher(programArgs, mainClass, inheritClasspath, classpathEntries, captureOutput, captureErr, logger);
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
   * Should the spawned process inherit all classpath entries of the currently
   * running process?
   */
  private final boolean inheritClasspath;

  /**
   * Should assertions be enabled in the subprocess? default false.
   */
  private boolean enableAssertions;

  /**
   * Paths that will be added to the default classpath
   */
  private final List<String> xtraClasspath = new ArrayList<>();

  /**
   * A {@link Thread} which spins and drains stdout of the running process.
   */
  private Thread stdOutDrain;

  /**
   * A {@link Thread} which spins and drains stderr of the running process.
   */
  private Thread stdErrDrain;

  /**
   * Absolute path of the 'java' executable to use.
   */
  private String javaExe;

  /**
   * Extra args to pass to the JVM
   */
  private List<String> vmArgs = new ArrayList<>();

  /**
   * The last process returned by a call to start() on this object.
   */
  private Process lastProcess;

  private JavaLauncher(String programArgs, String mainClass, boolean inheritClasspath, List<String> xtraClasspath,
      boolean captureOutput, boolean captureErr, Logger logger) {
    super(captureOutput, captureErr, logger);
    this.programArgs = programArgs;
    this.mainClass = mainClass;
    this.inheritClasspath = inheritClasspath;
    if (xtraClasspath != null) {
      this.xtraClasspath.addAll(xtraClasspath);
    }
    this.javaExe = defaultJavaExe();
  }

  public String getJavaExe() {
    return javaExe;
  }

  public void setJavaExe(String javaExe) {
    this.javaExe = javaExe;
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
  public static String defaultJavaExe() {
    String java = System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + "java";
    return java;
  }

  /**
   * Launch the java process.
   */
  public Process start() throws IllegalArgumentException, IOException {
    String cp = makeClasspath();

    String heap = "-Xmx800M";

    // on Mac, need to pass an extra parameter so we can cleanly kill child
    // Java process
    String signalParam = PlatformUtil.onMacOSX() ? "-Xrs" : null;

    List<String> cmd = new ArrayList<>();

    cmd.add(javaExe);
    cmd.add(heap);
    if (signalParam != null) {
      cmd.add(signalParam);
    }
    cmd.add("-classpath");
    cmd.add(cp);
    String libPath = makeLibPath();
    if (libPath != null) {
      cmd.add(libPath);
    }
    if (enableAssertions) {
      cmd.add("-ea");
    }
    if (vmArgs != null) {
      for (String s : vmArgs) {
        cmd.add(s);
      }
    }
    cmd.add(getMainClass());
    if (getProgramArgs() != null) {
      String[] pa = getProgramArgs().split(" ");
      for (String s : pa) {
        if (s.length() > 0) {
          cmd.add(s);
        }
      }
    }

    String[] cmds = new String[cmd.size()];
    cmd.toArray(cmds);

    Process p = spawnProcess(cmds);
    stdErrDrain = isCaptureErr() ? captureStdErr(p) : drainStdErr(p);
    stdOutDrain = isCaptureOutput() ? captureStdOut(p) : drainStdOut(p);
    lastProcess = p;
    return p;
  }

  public Process getLastProcess() {
    return lastProcess;
  }

  private static String makeLibPath() {
    String libPath = System.getProperty("java.library.path");
    if (libPath == null) {
      return null;
    } else {
      return "-Djava.library.path=" + libPath.trim();
    }
  }

  /**
   * Wait for the spawned process to terminate.
   * 
   * @throws IllegalStateException
   *           if the process has not been started
   */
  public void join() {
    if (stdOutDrain == null || stdErrDrain == null) {
      throw new IllegalStateException("process not started.  illegal to join()");
    }
    try {
      stdOutDrain.join();
      stdErrDrain.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new InternalError("Internal error in JavaLauncher.join()");
    }
    if (isCaptureErr()) {
      Drainer d = (Drainer) stdErrDrain;
      setStdErr(d.getCapture().toByteArray());
    }
    if (isCaptureOutput()) {
      Drainer d = (Drainer) stdOutDrain;
      setStdOut(d.getCapture().toByteArray());
    }
  }

  /**
   * Compute the classpath for the spawned process
   */
  public String makeClasspath() {
    String cp = inheritClasspath ? System.getProperty("java.class.path") : "";
    if (getXtraClassPath() == null || getXtraClassPath().isEmpty()) {
      return cp.trim();
    } else {
      for (String p : Iterator2Iterable.make(getXtraClassPath().iterator())) {
        cp += File.pathSeparatorChar;
        cp += p;
      }
      return cp.trim();
    }
  }

  /**
   * If the input string contains a space, quote it (for use as a classpath).
   * TODO: Figure out how to make a Mac happy with quotes. Trailing separators
   * are unsafe, so we have to escape the last backslash (if present and
   * unescaped), so it doesn't escape the closing quote.
   */
  @Deprecated
  public static String quoteStringIfNeeded(String s) {
    s = s.trim();
    // s = s.replaceAll(" ", "\\\\ ");
    return s;
    // Check if there's a space. If not, skip quoting to make Macs happy.
    // TODO: Add the check for an escaped space.
    // if (s.indexOf(' ') == -1) {
    // return s;
    // }
    // if (s.charAt(s.length() - 1) == '\\' && s.charAt(s.length() - 2) != '\\')
    // {
    // s += '\\'; // Escape the last backslash, so it doesn't escape the quote.
    // }
    // return '\"' + s + '\"';
  }

  public boolean isEnableAssertions() {
    return enableAssertions;
  }

  public void setEnableAssertions(boolean enableAssertions) {
    this.enableAssertions = enableAssertions;
  }

  public void addVmArg(String arg) {
    this.vmArgs.add(arg);
  }

  public List<String> getVmArgs() {
    return Collections.unmodifiableList(vmArgs);
  }

}
