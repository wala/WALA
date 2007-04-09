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

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.util.intset.IntPair;

public class Variable implements ITerm, Comparable<Variable> {

  private final int number;
  /**
   * universe of valid int constants this variable can assume.
   * This is optional; can be null.
   */
  private final IntPair range;

  private Variable(int number, IntPair range) {
    this.number = number;
    this.range = range;
  }
  
  public boolean isAtomic() {
    return true;
  }

  public Kind getKind() {
    return Kind.VARIABLE;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + number;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final Variable other = (Variable) obj;
    if (number != other.number)
      return false;
    return true;
  }

  public static Variable make(int number, IntPair range) {
    return new Variable(number, range);
  }
  
  public int getNumber() {
    return number;
  }
  
  @Override
  public String toString() {
    return "v" + getNumber();
  }

  public IntPair getRange() {
    return range;
  }

  public Collection<Variable> getFreeVariables() {
    return Collections.singleton(this);
  }

  public int compareTo(Variable o) {
    return this.getNumber() - o.getNumber();
  }
}
