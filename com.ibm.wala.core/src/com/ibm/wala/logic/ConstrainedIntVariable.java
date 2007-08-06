/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.logic;

import com.ibm.wala.util.intset.IntPair;

/**
 * A term that represents a variable in a formula
 * 
 * @author sjfink
 */
public class ConstrainedIntVariable extends IntVariable {

  /**
   * universe of valid integer constants this variable can assume.
   */
  private final IntPair range;

  protected ConstrainedIntVariable(int number, IntPair range) {
    super(number);
    assert range != null;
    this.range = range;
  }

  public static ConstrainedIntVariable make(int number, IntPair range) {
    return new ConstrainedIntVariable(number, range);
  }
  
  public IntPair getRange() {
    return range;
  }
}
