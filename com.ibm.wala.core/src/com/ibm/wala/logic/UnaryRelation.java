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


public class UnaryRelation implements IRelation {
 
  private final String symbol;
  
  protected UnaryRelation(String symbol) {
    this.symbol = symbol;
  }
  
  public int getValence() {
    return 1;
  }

  @Override
  public String toString() {
    return getSymbol() + " : int ";
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((symbol == null) ? 0 : symbol.hashCode());
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
    final UnaryRelation other = (UnaryRelation) obj;
    if (symbol == null) {
      if (other.symbol != null)
        return false;
    } else if (!symbol.equals(other.symbol))
      return false;
    return true;
  }

  public String getSymbol() {
    return symbol;
  }

  
  public static UnaryRelation make(String symbol) {
    return new UnaryRelation(symbol);
  }

}
