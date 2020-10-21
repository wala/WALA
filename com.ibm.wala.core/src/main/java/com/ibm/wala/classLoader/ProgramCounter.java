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
package com.ibm.wala.classLoader;

/** Simple object that represents a program counter value (i.e., an instruction in the bytecode) */
public class ProgramCounter {

  /** A constant indicating no source line number information is available. */
  public static final int NO_SOURCE_LINE_NUMBER = -1;

  /** Index into bytecode describing this instruction */
  private final int programCounter;

  /** @param programCounter Index into bytecode describing this instruction */
  public ProgramCounter(final int programCounter) {
    if (programCounter < 0) {
      throw new IllegalArgumentException("illegal programCounter: " + programCounter);
    }
    this.programCounter = programCounter;
  }

  /**
   * Return the program counter (index into the method's bytecode) for this call site.
   *
   * @return the program counter (index into the method's bytecode) for this call site.
   */
  public int getProgramCounter() {
    return programCounter;
  }

  /**
   * A Program Counter value is enough to uniquely identify a call site reference within a method.
   *
   * <p>Note: must use these objects with extreme care; this only works if you never mix
   * ProgramLocations from different methods in the same collection.
   */
  @Override
  public boolean equals(Object obj) {
    return (obj instanceof ProgramCounter)
        && ((ProgramCounter) obj).programCounter == programCounter;
  }

  @Override
  public int hashCode() {
    return programCounter + 77;
  }

  @Override
  public String toString() {
    return "PC@" + programCounter;
  }
}
