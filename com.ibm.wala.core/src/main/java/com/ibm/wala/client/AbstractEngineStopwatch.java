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

/** An object to track performance of an analysis engine */
public abstract class AbstractEngineStopwatch implements EngineStopwatch {

  /** @return the number of distinct categories timed by this object */
  protected abstract int getNumberOfCategories();

  /** @return an array of Strings that represent names of the categories tracked */
  protected abstract String[] getCategoryNames();

  protected final StopwatchGC[] stopwatch;

  protected AbstractEngineStopwatch() {
    stopwatch = new StopwatchGC[getNumberOfCategories()];
    for (int i = 0; i < getNumberOfCategories(); i++) {
      stopwatch[i] = new StopwatchGC(getCategoryNames()[i]);
    }
  }

  @Override
  public final String report() {
    StringBuilder result = new StringBuilder();
    long total = 0;
    for (int i = 0; i < getNumberOfCategories(); i++) {
      total += stopwatch[i].getElapsedMillis();
      result
          .append(getCategoryNames()[i])
          .append(": ")
          .append(stopwatch[i].getElapsedMillis())
          .append('\n');
    }
    result.append("Total       : ").append(total).append('\n');
    return result.toString();
  }

  /** */
  @Override
  public void start(byte category) {
    stopwatch[category].start();
  }

  /** */
  @Override
  public void stop(byte category) {
    stopwatch[category].stop();
  }

  @Override
  public StopwatchGC getTimer(byte category) {
    return stopwatch[category];
  }
}
