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
}
