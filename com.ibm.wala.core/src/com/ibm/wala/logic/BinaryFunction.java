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


public class BinaryFunction implements IFunction {

  private final String symbol;

  private BinaryFunction(String symbol) {
    this.symbol = symbol;
  }

  public static BinaryFunction make(String symbol) {
    return new BinaryFunction(symbol);
  }

  public int getNumberOfParameters() {
    return 2;
  }

  public String getSymbol() {
    return symbol;
  }
  
  @Override
  public String toString() {
    return getSymbol() + " : int x int -> int";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
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
    final BinaryFunction other = (BinaryFunction) obj;
    if (symbol == null) {
      if (other.symbol != null)
        return false;
    } else if (!symbol.equals(other.symbol))
      return false;
    return true;
  }
  
}
