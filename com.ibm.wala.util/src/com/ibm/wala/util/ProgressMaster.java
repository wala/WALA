/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;



/**
 * A class to control execution through the {@link IProgressMonitor} interface.
 * 
 * This class bounds each work item with a time in milliseconds. If there is no
 * apparent progress within the specified bound, this class cancels itself.
 */
public class ProgressMaster implements IProgressMonitor {

  private final IProgressMonitor delegate;

  private volatile boolean timedOut = false;

  /**
   * If -1, work items can run forever.
   */
  private int msPerWorkItem = -1;

  private Timeout currentNanny;

  private ProgressMaster(IProgressMonitor monitor) {
    this.delegate = monitor;
  }

  public static ProgressMaster make(IProgressMonitor monitor) {
    if (monitor == null) {
      throw new IllegalArgumentException("null monitor");
    }
    return new ProgressMaster(monitor);
  }

  public synchronized void beginTask(String name, int totalWork) {
    delegate.beginTask(name, totalWork);
    startNanny();
  }

  private synchronized void startNanny() {
    killNanny();
    if (msPerWorkItem >= 1) {
      currentNanny = new Timeout(msPerWorkItem);
      currentNanny.start();
    }
  }

  public synchronized void reset() {
    killNanny();
    setCanceled(false);
    timedOut = false;
  }

  /**
   * Was the last cancel state due to a timeout?
   */
  public boolean lastItemTimedOut() {
    return timedOut;
  }

  public synchronized void done() {
    killNanny();
    delegate.done();
  }

  private synchronized void killNanny() {
    if (currentNanny != null) {
      currentNanny.interrupt();
      try {
        currentNanny.join();
      } catch (InterruptedException e) {
      }
      currentNanny = null;
    }
  }

  public boolean isCanceled() {
    return delegate.isCanceled() || timedOut;
  }

  public void setCanceled(boolean value) {
    killNanny();
  }

  public synchronized void worked(int work) {
    killNanny();
    delegate.worked(work);
    startNanny();
  }

  public int getMillisPerWorkItem() {
    return msPerWorkItem;
  }

  public void setMillisPerWorkItem(int msPerWorkItem) {
    this.msPerWorkItem = msPerWorkItem;
  }

  private class Timeout extends Thread {
    @Override
    public void run() {
      try {
        Thread.sleep(sleepMillis);
        if (isInterrupted()) {
          return;
        }
        timedOut = true;
      } catch (InterruptedException e) {
        return;
      }
    }

    private final int sleepMillis;

    Timeout(int sleepMillis) {
      assert sleepMillis >= 1;
      this.sleepMillis = sleepMillis;
    }
  }

}
