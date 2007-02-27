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

import com.ibm.wala.util.warnings.WalaException;

/**
 * A generic process launcher
 */
public class BasicLauncher extends Launcher {

  protected String cmd;

  public BasicLauncher(boolean captureOutput) {
    super(captureOutput);
  }

  public String getCmd() {
    return cmd;
  }

  public void setCmd(String newCmd) {
    cmd = newCmd;
  }

  public String toString() {
    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (cmd: ");
    result.append(cmd);
    result.append(", captureOutput: ");
    result.append(isCaptureOutput());
    result.append(", Output: ");
    result.append(output);
    result.append(')');
    return result.toString();
  }

  /**
   * Launch the process and wait until it is finished.
   * @throws WalaException
   * @throws IllegalArgumentException
   */
  public void launch() throws WalaException, IllegalArgumentException {
    Process p = spawnProcess(getCmd());
    Thread d1 = drainStdErr(p);
    Thread d2 = isCaptureOutput() ? captureStdOut(p) : drainStdOut(p);
    try {
      d1.join();
      d2.join();
    } catch (InterruptedException e) {
      throw new WalaException("Internal error", e);
    }
    if (isCaptureOutput()) {
      Drainer d = (Drainer) d2;
      setOutput(d.getCapture().toByteArray());
    }
  }
}
