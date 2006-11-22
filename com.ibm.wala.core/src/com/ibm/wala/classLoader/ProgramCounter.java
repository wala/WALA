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
package com.ibm.wala.classLoader;

import com.ibm.wala.util.debug.Assertions;

/**
 *
 * Simple object that represents a program counter value
 * (ie., an instruction in the bytecode)
 * 
 * @author sfink
 */
public class ProgramCounter implements IProgramCounter {

  /**
   * Index into bytecode describing this instruction
   */
  private final int programCounter;

  /**
   * @param programCounter Index into bytecode describing this instruction
   */
  public ProgramCounter(final int programCounter) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(programCounter >= 0);
    }
    this.programCounter = programCounter;

  }

  /**
   * Return the program counter (index into the method's bytecode) 
   * for this call site.
   * @return the program counter (index into the method's bytecode) 
   * for this call site.
   *
   */
  public int getProgramCounter() {
    return programCounter;
  }


  /**
   * A Program Counter value is enough to uniquely identify a call site reference
   * within a method.
   * 
   * Note: must use these objects with extreme care; this only works if you never
   * mix ProgramLocations from different methods in the same collection.
   * 
   * @see java.lang.Object#equals(Object)
   */
  public boolean equals(Object obj) {
      return
	  (obj instanceof ProgramCounter) 
	               &&
	  ((ProgramCounter) obj).programCounter == programCounter;
  }
    
  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return programCounter;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "PC@" + programCounter;
  }
}
