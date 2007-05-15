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
import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.IntSet;

public class UnaryFunction implements IFunction {

  private final String symbol;

  private UnaryFunction(String symbol) {
    this.symbol = symbol;
  }

  public static UnaryFunction make(String symbol) {
    return new UnaryFunction(symbol);
  }

  public int getNumberOfParameters() {
    return 1;
  }

  public String getSymbol() {
    return symbol;
  }

  /**
   * Build constraints which ensure that the function f defines the relation R.
   * @param domain 
   * @throws IllegalArgumentException  if domain is null
   */
  public static Collection<IFormula> buildConstraints(IBinaryNaturalRelation R, UnaryFunction f, IntPair domain) {
    if (domain == null) {
      throw new IllegalArgumentException("domain is null");
    }
    Set<IFormula> result = HashSetFactory.make();
    for (int i = domain.getX(); i <= domain.getY(); i++) {
      IntSet s = R.getRelated(i);
      if (s != null) {
        assert (s.size() == 1);
        result.add(RelationFormula.makeEquals(FunctionTerm.make(f, i), s.intIterator().next()));
      } else {
        result.add(RelationFormula.makeEquals(FunctionTerm.make(f, i), -1));
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return getSymbol() + " : int -> int";
  }

}
