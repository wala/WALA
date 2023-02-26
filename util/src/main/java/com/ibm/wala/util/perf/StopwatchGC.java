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
package com.ibm.wala.util.perf;

/** A {@link Stopwatch} that also queries the free memory from the GC. This is mostly useless. */
public class StopwatchGC extends com.ibm.wala.util.perf.Stopwatch {

  private final String name;

  private long startMemory;

  private long endMemory;

  public StopwatchGC(String name) {
    super();
    this.name = name;
  }

  @Override
  public final void start() {
    if (count == 0) {
      // when the GC stop watch is used for repeating events, we count from the first start to the
      // last end.
      // (a different approach would be to accumulate the delta's)
      System.gc();
      Runtime r = Runtime.getRuntime();
      startMemory = r.totalMemory() - r.freeMemory();
    }
    super.start();
  }

  @Override
  public final void stop() {
    super.stop();
    System.gc();
    Runtime r = Runtime.getRuntime();
    endMemory = r.totalMemory() - r.freeMemory();
  }

  public final String report() {
    String result = "";
    if (getCount() > 0) {
      result += "Stopwatch: " + name + ' ' + getElapsedMillis() + " ms" + '\n';
    }
    if (getCount() == 1) {
      result += "       Footprint at entry: " + (float) startMemory / 1000000 + " MB\n";
      result += "        Footprint at exit: " + (float) endMemory / 1000000 + " MB\n";
      result +=
          "                    Delta: " + (float) (endMemory - startMemory) / 1000000 + " MB\n";
    }
    return result;
  }

  /** @return memory at the end of the phase, in MB */
  public float getEndMemory() {
    return (float) endMemory / 1000000;
  }

  /** @return memory at the end of the phase, in MB */
  public float getStartMemory() {
    return (float) startMemory / 1000000;
  }

  /** @return getEndMemory() - getStartMemory() */
  public float getFootprint() {
    return getEndMemory() - getStartMemory();
  }

  /** Returns the name for this timer. */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    //    if (count == 1){
    //      sb.append (", Footprint at entry: " + (float) startMemory / 1000000 + " MB");
    //      sb.append (", Footprint at exit: " + (float) endMemory / 1000000 + " MB");
    //    }
    return super.toString() + ", Delta: " + (float) (endMemory - startMemory) / 1000000 + " MB";
  }
}
