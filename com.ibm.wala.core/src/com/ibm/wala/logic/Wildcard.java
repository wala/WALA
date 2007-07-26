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
package com.ibm.wala.logic;

/**
 * A special constant used for pattern matching during substitution.
 * 
 * @author sjfink
 */
public class Wildcard extends AbstractConstant {

  public static final Wildcard STAR = new Wildcard(0);
  
  private final int number;
  private Wildcard(int number) {
    this.number = number;
  }

  @Override
  public String toString() {
    return number == 0 ?  "*" : "?" + number + "?";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + number;
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
    final Wildcard other = (Wildcard) obj;
    if (number != other.number)
      return false;
    return true;
  }

  public int getNumber() {
    return number;
  }

  public static Wildcard make(int i) {
    return new Wildcard(i);
  }

}
