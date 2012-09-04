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



/**
 * Simple utilities for Eclipse progress monitors
 */
public class MonitorUtil {
  
  /**
   * Use this interface to decouple core utilities from the Eclipse layer
   */
  public interface IProgressMonitor {

    void beginTask(String task, int totalWork);

    boolean isCanceled();

    void done();

    void worked(int units);
  }

  public static void beginTask(IProgressMonitor monitor, String task, int totalWork) throws CancelException {
    if (monitor != null) {
      monitor.beginTask(task, totalWork);
      if (monitor.isCanceled()) {
        throw CancelException.make("cancelled in " + task);
      }
    }
  }

  public static void done(IProgressMonitor monitor) throws CancelException {
    if (monitor != null) {
      monitor.done();
      if (monitor.isCanceled()) {
        throw CancelException.make("cancelled in " + monitor.toString());
      }
    }
  }

  public static void worked(IProgressMonitor monitor, int units) throws CancelException {
    if (monitor != null) {
      monitor.worked(units);
      if (monitor.isCanceled()) {
        throw CancelException.make("cancelled in " + monitor.toString());
      }
    }
  }

  public static void throwExceptionIfCanceled(IProgressMonitor progressMonitor) throws CancelException {
    if (progressMonitor != null) {
      if (progressMonitor.isCanceled()) {
        throw CancelException.make("operation cancelled");
      }
    }
  }

//  public static IProgressMonitor subProgress(ProgressMaster progress, int i) {
//    if (progress == null) {
//      return null;
//    } else {
//      return new SubProgressMonitor(progress, i);
//    }
//  }
}
