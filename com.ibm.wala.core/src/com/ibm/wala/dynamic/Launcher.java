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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.util.warnings.WalaException;

/**
 * Abstract base class for a process launcher
 */
public abstract class Launcher {

  protected File workingDir = null;

  protected Map env = null;

  protected byte[] output = null;
  
  private byte[] input = null;

  private final boolean captureOutput;

  protected Launcher() {
    super();
    this.captureOutput = false;
  }

  protected Launcher(boolean captureOutput) {
    super();
    this.captureOutput = captureOutput;
  }

  public File getWorkingDir() {
    return workingDir;
  }

  public void setWorkingDir(File newWorkingDir) {
    workingDir = newWorkingDir;
  }

  public Map getEnv() {
    return env;
  }

  public void setEnv(Map newEnv) {
    env = newEnv;
  }

  public String toString() {
    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (workingDir: ");
    result.append(workingDir);
    result.append(", env: ");
    result.append(env);
    result.append(')');
    return result.toString();
  }

  /**
   * Spawn a process to execute the given command
   * @param cmd
   * @return an object representing the process
   * @throws WalaException
   * @throws IllegalArgumentException
   */
  protected Process spawnProcess(String cmd) throws WalaException, IllegalArgumentException {
    if (cmd == null) {
      throw new IllegalArgumentException("cmd cannot be null");
    }
    System.out.println("spawning process " + cmd);
    String[] env = getEnv() == null ? null : buildEnv(getEnv());
    try {
      Process p = Runtime.getRuntime().exec(cmd, env, getWorkingDir());
      return p;
    } catch (IOException e) {
      e.printStackTrace();
      throw new WalaException("IOException in " + getClass());
    }
  }

  private String[] buildEnv(Map env) {
    String[] result = new String[env.size()];
    int i = 0;
    for (Iterator it = env.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      result[i++] = e.getKey() + "=" + e.getValue();
    }
    return result;
  }

  protected Thread drainStdOut(Process p) {
    final BufferedInputStream output = new BufferedInputStream(p.getInputStream());
    Thread result = new Drainer(p) {
      void drain() throws IOException {
        drainAndPrint(output, System.out);
      }
    };
    result.start();
    return result;
  }

  protected Drainer captureStdOut(Process p) {
    final BufferedInputStream output = new BufferedInputStream(p.getInputStream());
    final ByteArrayOutputStream b = new ByteArrayOutputStream();
    Drainer result = new Drainer(p) {
      void drain() throws IOException {
        drainAndCatch(output, b);
      }
    };
    result.setCapture(b);
    result.start();
    return result;
  }

  protected Thread drainStdErr(Process p) {
    final BufferedInputStream err = new BufferedInputStream(p.getErrorStream());
    Thread result = new Drainer(p) {
      void drain() throws IOException {
        drainAndPrint(err, System.err);
      }
    };
    result.start();
    return result;
  }

  /**
   * @author sfink
   * 
   * A thread that runs in a loop, performing the drain() action until a process
   * terminates
   */
  abstract class Drainer extends Thread {

    private final Process p;

    private ByteArrayOutputStream capture;

    abstract void drain() throws IOException;

    Drainer(Process p) {
      this.p = p;
    }

    public void run() {
      try {
        boolean repeat = true;
        while (repeat) {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e1) {
            e1.printStackTrace();
            // just ignore and continue
          }
          drain();
          try {
            p.exitValue();
            // if we get here, the process has terminated
            repeat = false;
            drain();
            System.out.println("process terminated with exit code " + p.exitValue());
          } catch (IllegalThreadStateException e) {
            // this means the process has not yet terminated.
            repeat = true;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public ByteArrayOutputStream getCapture() {
      return capture;
    }

    public void setCapture(ByteArrayOutputStream capture) {
      this.capture = capture;
    }
  }

  private void drainAndPrint(BufferedInputStream s, PrintStream p) throws IOException {
    if (s.available() > 0) {
      byte[] data = new byte[s.available()];
      s.read(data);
      p.print(new String(data));
    }
  }

  private void drainAndCatch(BufferedInputStream s, ByteArrayOutputStream b) throws IOException {
    if (s.available() > 0) {
      byte[] data = new byte[s.available()];
      int nRead = s.read(data);
      b.write(data, 0, nRead);
    }
  }


  public boolean isCaptureOutput() {
    return captureOutput;
  }

  public byte[] getOutput() {
    return output;
  }

  protected void setOutput(byte[] newOutput) {
    output = newOutput;
  }

  public byte[] getInput() {
    return input;
  }

  /**
   * Set input which will be fed to the launched process's stdin
   */
  public void setInput(byte[] input) {
    this.input = input;
  }
}
