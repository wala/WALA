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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

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

  private volatile boolean tooMuchMemory = false;

  /**
   * If -1, work items can run forever.
   */
  private final int msPerWorkItem;

  private final boolean checkMemory;
  
  private Timeout currentNanny;

  private ProgressMaster(IProgressMonitor monitor, int msPerWorkItem, boolean checkMemory) {
    this.delegate = monitor;
    this.msPerWorkItem = msPerWorkItem;
    this.checkMemory = checkMemory;
  }

  public static ProgressMaster make(IProgressMonitor monitor, int msPerWorkItem, boolean checkMemory) {
    if (monitor == null) {
      throw new IllegalArgumentException("null monitor");
    }
    return new ProgressMaster(monitor, msPerWorkItem, checkMemory);
  }

  @Override
  public synchronized void beginTask(String name, int totalWork) {
    delegate.beginTask(name, totalWork);
    startNanny();
  }

  private synchronized void startNanny() {
    killNanny();
    if (msPerWorkItem >= 1 || checkMemory) {
      currentNanny = new Timeout();
      // don't let nanny thread prevent JVM exit
      currentNanny.setDaemon(true);
      currentNanny.start();
    }
  }

  public synchronized void reset() {
    killNanny();
    setCanceled();
    timedOut = false;
    tooMuchMemory = false;
  }

  /**
   * Was the last cancel state due to a timeout?
   */
  public boolean lastItemTimedOut() {
    return timedOut;
  }

  public boolean lastItemTooMuchMemory() {
    return tooMuchMemory;
  }
  
  @Override
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

  @Override
  public boolean isCanceled() {
    return delegate.isCanceled() || timedOut || tooMuchMemory;
  }

  public void setCanceled() {
    killNanny();
  }

  /** BEGIN Custom change: subtasks and canceling */

  @Override
  public void subTask(String subTask) {
    delegate.subTask(subTask);
  }

  @Override
  public void cancel() {
    setCanceled();
  }
/** END Custom change: subtasks and canceling */
  @Override
  public synchronized void worked(int work) {
    killNanny();
    delegate.worked(work);
    startNanny();
  }

  public int getMillisPerWorkItem() {
    return msPerWorkItem;
  }

  public static class TooMuchMemoryUsed extends Exception {

    private static final long serialVersionUID = -7174940833610292692L;

  }

  private class Timeout extends Thread {
    
    @Override
    public void run() {
      try {
        MemoryMXBean gcbean = null;
        NotificationListener listener = null;
        
        if (checkMemory) {
          for(MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType().equals(MemoryType.HEAP)) {
              pool.setCollectionUsageThreshold((long) (pool.getUsage().getMax() * MAX_USED_MEM_BEFORE_BACKING_OUT));
            }
          }
          
          final Thread nannyThread = this;
          gcbean = ManagementFactory.getMemoryMXBean();

          listener = (notification, arg1) -> {
            MemoryNotificationInfo info = MemoryNotificationInfo.from((CompositeData) notification.getUserData());
            long used = info.getUsage().getUsed();
            long max = Runtime.getRuntime().maxMemory();

            if (((double)used/(double)max) > MAX_USED_MEM_BEFORE_BACKING_OUT) {
              System.err.println("used " + used + " of " + max);
              tooMuchMemory = true;
              nannyThread.interrupt();
            }
          };
          try {
            ManagementFactory.getPlatformMBeanServer().addNotificationListener(gcbean.getObjectName(), listener, null, null);
          } catch (InstanceNotFoundException e) {
            throw new Error("cannot find existing bean", e);
          }
        }

        Thread.sleep(msPerWorkItem);

        if (checkMemory) {
          try {
          ManagementFactory.getPlatformMBeanServer().removeNotificationListener(gcbean.getObjectName(), listener);
          } catch (InstanceNotFoundException | ListenerNotFoundException e) {
            throw new Error("cannot find existing bean", e);
          }
        }
        
        if (isInterrupted()) {
          return;
        }
        
        timedOut = true;
      } catch (InterruptedException e) {
        return;
      }
    }
    
    private static final double MAX_USED_MEM_BEFORE_BACKING_OUT = .7;
    
  }

  @Override
  public String getCancelMessage() {
    return tooMuchMemory? "too much memory": timedOut? "timed out" : "unknown";
  }

}
