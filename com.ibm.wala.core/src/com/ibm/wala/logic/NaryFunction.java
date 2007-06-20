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
 * @author schandra_sf
 */
public class NaryFunction implements IFunction {

  private final String symbol;

  private final int k;

  private NaryFunction(String symbol, int k) {
    this.symbol = symbol;
    this.k = k;
  }

  public static NaryFunction make(String symbol, int k) {
    return new NaryFunction(symbol, k);
  }

  public int getNumberOfParameters() {
    return k;
  }

  public String getSymbol() {
    return symbol;
  }

  @Override
  public String toString() {
    return getSymbol() + " : int ^ k -> int";
  }
}
