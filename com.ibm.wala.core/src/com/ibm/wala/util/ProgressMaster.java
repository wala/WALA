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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.sun.management.GarbageCollectionNotificationInfo;



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
    setCanceled(false);
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

  public void setCanceled(boolean value) {
    killNanny();
  }

  /** BEGIN Custom change: subtasks and canceling */

  @Override
  public void subTask(String subTask) {
    delegate.subTask(subTask);
  }

  @Override
  public void cancel() {
    setCanceled(true);
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

  }

  private class Timeout extends Thread {
    
    @Override
    public void run() {
      try {
        Map<NotificationEmitter,NotificationListener> gcListeners = Collections.emptyMap();;

        if (checkMemory) {
          gcListeners = HashMapFactory.make();
          final Thread nannyThread = this;
          List<GarbageCollectorMXBean> gcbeans = ManagementFactory.getGarbageCollectorMXBeans();
          for (GarbageCollectorMXBean gcbean : gcbeans) {
            final NotificationEmitter emitter = (NotificationEmitter) gcbean;
            NotificationFilter filter = new NotificationFilter() {
              @Override
              public boolean isNotificationEnabled(Notification notification) {
                return notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION);
              }
            };
            NotificationListener listener = new NotificationListener() {
              @Override
              public void handleNotification(Notification notification, Object arg1) {
                GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());

                long used = 0;
                for(MemoryUsage usage : info.getGcInfo().getMemoryUsageAfterGc().values()) {
                  used += usage.getUsed();
                }

                long max = Runtime.getRuntime().maxMemory();

                if (((double)used/(double)max) > MAX_USED_MEM_BEFORE_BACKING_OUT) {
                  tooMuchMemory = true;
                  nannyThread.interrupt();
                }
              }
            };
            emitter.addNotificationListener(listener, filter, null);
            gcListeners.put(emitter,listener);
          }
        }

        Thread.sleep(msPerWorkItem);

        if (checkMemory) {
          try {
            for(Map.Entry<NotificationEmitter,NotificationListener> gc : gcListeners.entrySet()) {
              gc.getKey().removeNotificationListener(gc.getValue());
            }
          } catch (ListenerNotFoundException e) {
            assert false : "cannot remove listener that was added";
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
    
    private static final double MAX_USED_MEM_BEFORE_BACKING_OUT = .8;
    
  }

  @Override
  public String getCancelMessage() {
    return tooMuchMemory? "too much memory": timedOut? "timed out" : "unknown";
  }

}
