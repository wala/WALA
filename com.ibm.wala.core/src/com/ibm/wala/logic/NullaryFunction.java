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


/**
 * a function that takes no parameters.
 * 
 * @author sjfink
 *
 */
public class NullaryFunction implements IFunction {

  private final String symbol;



  private NullaryFunction(String symbol) {
    this.symbol = symbol;
  }

  public static NullaryFunction make(String symbol) {
    return new NullaryFunction(symbol);
  }

  public int getNumberOfParameters() {
    return 0;
  }

  public String getSymbol() {
    return symbol;
  }

  @Override
  public String toString() {
    return getSymbol() + "()";
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
    final NullaryFunction other = (NullaryFunction) obj;
    if (symbol == null) {
      if (other.symbol != null)
        return false;
    } else if (!symbol.equals(other.symbol))
      return false;
    return true;
  }
}
