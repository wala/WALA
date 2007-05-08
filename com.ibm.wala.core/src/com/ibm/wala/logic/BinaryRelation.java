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
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.IntSet;

public class BinaryRelation implements IRelation {
  
  public final static BinaryRelation EQUALS = new BinaryRelation("=");

  private final String symbol;
  
  protected BinaryRelation(String symbol) {
    this.symbol = symbol;
  }
  
  public int getValence() {
    return 2;
  }

  @Override
  public String toString() {
    return getSymbol() + " : int x int";
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
    final BinaryRelation other = (BinaryRelation) obj;
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

  /**
   * build a constraint saying v \in s
   */
  public static IFormula makeSetConstraint(Variable v, IntSet s) {
    if (s.isEmpty()) {
      // a hack. TODO: support primitives for "true" and "false"
      return RelationFormula.makeEquals(IntConstant.make(0), IntConstant.make(1));
    }
    IntIterator it = s.intIterator();
    int first = it.next();
    IFormula result = RelationFormula.makeEquals(v, first);
    while (it.hasNext()) {
      int i = it.next();
      result = BinaryFormula.or(result, RelationFormula.makeEquals(v, i));
    }
    return result;
  }
  
  /**
   * Build constraints which ensure that the relation r fully defines the
   * relation R over the given range of integers.
   * @throws IllegalArgumentException  if domain is null
   * 
   */
  public static Collection<IFormula> buildConstraints(IBinaryNaturalRelation r, BinaryRelation R, IntPair domain) {
    if (domain == null) {
      throw new IllegalArgumentException("domain is null");
    }
    Set<IFormula> result = HashSetFactory.make();
    for (int i = domain.getX(); i <= domain.getY(); i++) {
      IntSet s = r.getRelated(i);
      if (s != null) {
        Variable v0 = Variable.make(0, domain);
        IFormula inSet = makeSetConstraint(v0, s);
        IFormula f = BinaryFormula.biconditional(inSet, RelationFormula.make(R, i, v0));
        result.add(QuantifiedFormula.forall(v0, f));
      }
    }
    return result;
  }
  
  public static BinaryRelation make(String symbol) {
    return new BinaryRelation(symbol);
  }

}
