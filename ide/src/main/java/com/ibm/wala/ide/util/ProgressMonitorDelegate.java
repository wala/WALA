/*
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ide.util;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

/** A Wrapper around an Eclipse IProgressMonitor */
public class ProgressMonitorDelegate implements IProgressMonitor {

  public static ProgressMonitorDelegate createProgressMonitorDelegate(
      org.eclipse.core.runtime.IProgressMonitor d) {
    if (d == null) {
      throw new IllegalArgumentException("d is null");
    }
    return new ProgressMonitorDelegate(d);
  }

  private final org.eclipse.core.runtime.IProgressMonitor delegate;

  private ProgressMonitorDelegate(org.eclipse.core.runtime.IProgressMonitor d) {
    this.delegate = d;
  }

  @Override
  public void beginTask(String task, int totalWork) {
    delegate.beginTask(task, totalWork);
  }

  @Override
  public boolean isCanceled() {
    return delegate.isCanceled();
  }

  @Override
  public void done() {
    delegate.done();
  }

  @Override
  public void worked(int units) {
    delegate.worked(units);
  }

  /* BEGIN Custom change: subtasks and canceling */
  @Override
  public void subTask(String subTask) {
    delegate.subTask(subTask);
  }

  @Override
  public void cancel() {
    delegate.setCanceled(true);
  }
  /* END Custom change: subtasks and canceling */
  @Override
  public String getCancelMessage() {
    return "cancelled by eclipse monitor: " + delegate.toString();
  }
}
