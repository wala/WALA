/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.client;

import com.ibm.wala.util.perf.StopwatchGC;

/** An object to track performance of analysis engine */
public interface EngineStopwatch {

  /** @return a String representation of the information in this object */
  public String report();

  /** start timing for some category */
  public void start(byte category);

  /** stop timing for some category */
  public void stop(byte category);

  /** Returns access to class encapsulating time events results, related to the given category. */
  public StopwatchGC getTimer(byte category);
}
