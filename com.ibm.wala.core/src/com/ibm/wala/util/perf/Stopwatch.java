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
package com.ibm.wala.util.perf;

/**
 * Basic class to time events
 * 
 * @author sfink
 */
public class Stopwatch {

  private int count;

  /**
   * elapsed time in nanoseconds
   */
  private long elapsedTime;

  private long startTime;

  public Stopwatch() {
  }

  public void start() {
    startTime = System.nanoTime();
  }

  public void stop() {
    long endTime = System.nanoTime();
    count++;
    elapsedTime += (endTime - startTime);
  }

  /**
   * @return elapsed time in ms
   */
  public long getElapsedMillis() {
    return elapsedTime / 1000000;
  }
  
  /**
   * @return number of times this stopwatch was stopped
   */
  public int getCount() {
    return count;
  }
}