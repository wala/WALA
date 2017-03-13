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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A generic process launcher
 */
public class BasicLauncher extends Launcher {

  protected String cmd;

  public BasicLauncher(boolean captureOutput, boolean captureErr, Logger logger) {
    super(captureOutput, captureErr, logger);
  }

  public String getCmd() {
    return cmd;
  }

  public void setCmd(String newCmd) {
    cmd = newCmd;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (cmd: ");
    result.append(cmd);
    return result.toString();
  }

  /**
   * Launch the process and wait until it is finished.  Returns the exit value of the process.
   */
  public int launch() throws  IllegalArgumentException, IOException {
    Process p = spawnProcess(getCmd());
    Thread d1 = isCaptureErr() ? captureStdErr(p) : drainStdErr(p);
    Thread d2 = isCaptureOutput() ? captureStdOut(p) : drainStdOut(p);
    if (getInput() != null) {
      try (final BufferedOutputStream input = new BufferedOutputStream(p.getOutputStream())) {
        input.write(getInput(), 0, getInput().length);
        input.flush();
      } catch (IOException e) {
        e.printStackTrace();
        throw new IOException("error priming stdin", e);
      }
    }
    try {
      d1.join();
      d2.join();
    } catch (InterruptedException e) {
      throw new Error("Internal error", e);
    }
    if (isCaptureErr()) {
      Drainer d = (Drainer) d1;
      setStdErr(d.getCapture().toByteArray());
    }
    if (isCaptureOutput()) {
      Drainer d = (Drainer) d2;
      setStdOut(d.getCapture().toByteArray());
    }
    return p.exitValue();
  }
}
