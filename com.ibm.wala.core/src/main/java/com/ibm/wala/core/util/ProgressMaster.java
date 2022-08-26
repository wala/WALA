/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.util;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

/**
 * A class to control execution through the {@link IProgressMonitor} interface.
 *
 * <p>This class bounds each work item with a time in milliseconds. If there is no apparent progress
 * within the specified bound, this class cancels itself.
 */
public class ProgressMaster {

  public static IProgressMonitor make(
      IProgressMonitor monitor, int msPerWorkItem, boolean checkMemory) {
    if (monitor == null) {
      throw new IllegalArgumentException("null monitor");
    }
    return new ProgressMasterImpl(monitor, msPerWorkItem, checkMemory);
  }
}
