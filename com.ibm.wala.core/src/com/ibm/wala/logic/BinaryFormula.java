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
import java.util.Iterator;

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;
import com.ibm.wala.util.collections.HashSetFactory;

public class BinaryFormula extends AbstractBinaryFormula {
  private final IFormula f1;

  private final IFormula f2;

  private final BinaryConnective b;

  private BinaryFormula(BinaryConnective b, IFormula f1, IFormula f2) {
    super();
    this.b = b;
    this.f1 = f1;
    this.f2 = f2;
  }

  public static IFormula and(Collection<IFormula> clauses) throws IllegalArgumentException {
    if (clauses == null) {
      throw new IllegalArgumentException("clauses is null");
    }
    if (clauses.isEmpty()) {
      throw new IllegalArgumentException("cannot and empty collection");
    }
    Iterator<IFormula> it = clauses.iterator();
    IFormula result = it.next();
    while (it.hasNext()) {
      result = and(result, it.next());
    }
    return result;
  }

  public static IFormula and(IFormula f1, IFormula f2) throws IllegalArgumentException {
    if (f1 == null) {
      throw new IllegalArgumentException("f1 == null");
    }
    if (f1.equals(BooleanConstantFormula.TRUE)) {
      return f2;
    } else if (f2.equals(BooleanConstantFormula.TRUE)) {
      return f1;
    } else if (f1.equals(BooleanConstantFormula.FALSE) || f2.equals(BooleanConstantFormula.FALSE)) {
      return BooleanConstantFormula.FALSE;
    } else {
      return new BinaryFormula(BinaryConnective.AND, f1, f2);
    }
  }

  public static BinaryFormula biconditional(IFormula f1, IFormula f2) {
    return new BinaryFormula(BinaryConnective.BICONDITIONAL, f1, f2);
  }

  public static IFormula make(BinaryConnective connective, IFormula f1, IFormula f2) {
    return new BinaryFormula(connective, f1, f2);
  }

  public static IFormula or(IFormula f1, IFormula f2) throws IllegalArgumentException {
    if (f1 == null) {
      throw new IllegalArgumentException("f1 == null");
    }
    if (f1.equals(BooleanConstantFormula.FALSE)) {
      return f2;
    } else if (f2.equals(BooleanConstantFormula.FALSE)) {
      return f1;
    } else if (f1.equals(BooleanConstantFormula.TRUE) || f2.equals(BooleanConstantFormula.TRUE)) {
      return BooleanConstantFormula.TRUE;
    } else {
      return new BinaryFormula(BinaryConnective.OR, f1, f2);
    }
  }

  public static BinaryFormula implies(IFormula f1, IFormula f2) {
    return new BinaryFormula(BinaryConnective.IMPLIES, f1, f2);
  }

  @Override
  public BinaryConnective getConnective() {
    return b;
  }

  @Override
  public IFormula getF1() {
    return f1;
  }

  @Override
  public IFormula getF2() {
    return f2;
  }

  public Collection<AbstractVariable> getFreeVariables() {
    Collection<AbstractVariable> result = HashSetFactory.make();
    result.addAll(f1.getFreeVariables());
    result.addAll(f2.getFreeVariables());
    return result;
  }
  
  public Collection<? extends ITerm> getAllTerms() {
    Collection<ITerm> result = HashSetFactory.make();
    result.addAll(f1.getAllTerms());
    result.addAll(f2.getAllTerms());
    return result;
  }

  public Collection<IConstant> getConstants() {
    Collection<IConstant> result = HashSetFactory.make();
    result.addAll(f1.getConstants());
    result.addAll(f2.getConstants());
    return result;
  }
  
  public String prettyPrint(ILogicDecorator d) throws IllegalArgumentException {
    if (d == null) {
      throw new IllegalArgumentException("d == null");
    }
    return d.prettyPrint(this);
  }

  @Override
  public String toString() {
    return prettyPrint(DefaultDecorator.instance());
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((b == null) ? 0 : b.hashCode());
    result = PRIME * result + ((f1 == null) ? 0 : f1.hashCode());
    result = PRIME * result + ((f2 == null) ? 0 : f2.hashCode());
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
    final BinaryFormula other = (BinaryFormula) obj;
    if (b == null) {
      if (other.b != null)
        return false;
    } else if (!b.equals(other.b))
      return false;
    if (f1 == null) {
      if (other.f1 != null)
        return false;
    } else if (!f1.equals(other.f1))
      return false;
    if (f2 == null) {
      if (other.f2 != null)
        return false;
    } else if (!f2.equals(other.f2))
      return false;
    return true;
  }
}
