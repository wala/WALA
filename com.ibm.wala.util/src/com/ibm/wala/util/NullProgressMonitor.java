/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

/** Dummy {@link IProgressMonitor} */
public class NullProgressMonitor implements IProgressMonitor {

  @Override
  public void beginTask(String task, int totalWork) {
    // do nothing
  }

  @Override
  public boolean isCanceled() {
    // do nothing
    return false;
  }

  @Override
  public void done() {
    // do nothing
  }

  @Override
  public void worked(int units) {
    // do nothing
  }

  /* BEGIN Custom change: subtasks and canceling */
  @Override
  public void subTask(String subTask) {
    // do nothing
  }

  @Override
  public void cancel() {
    // do nothing
  }

  /* END Custom change: subtasks and canceling */
  @Override
  public String getCancelMessage() {
    assert false;
    return "never cqncels";
  }
}
