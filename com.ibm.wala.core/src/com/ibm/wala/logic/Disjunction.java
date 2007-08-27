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

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * A disjunction of formulae
 * 
 * @author sjfink
 */
public class Disjunction extends AbstractBinaryFormula implements IMaxTerm {

  // invariant: size >= 2
  private final Collection<? extends IFormula> clauses;

  private Disjunction(Collection<? extends IFormula> clauses) {
    assert clauses.size() >= 2;
    this.clauses = clauses;
  }

  public Collection<? extends IConstant> getConstants() {
    Collection<IConstant> result = HashSetFactory.make();
    for (IFormula f : clauses) {
      result.addAll(f.getConstants());
    }
    return result;
  }

  public Collection<? extends ITerm> getAllTerms() {
    Collection<ITerm> result = HashSetFactory.make();
    for (IFormula f : clauses) {
      result.addAll(f.getAllTerms());
    }
    return result;
  }

  public Collection<AbstractVariable> getFreeVariables() {
    Collection<AbstractVariable> result = HashSetFactory.make();
    for (IFormula f : clauses) {
      result.addAll(f.getFreeVariables());
    }
    return result;
  }

  public String prettyPrint(ILogicDecorator d) {
    if (d == null) {
      throw new IllegalArgumentException("d == null");
    }
    return d.prettyPrint(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((clauses == null) ? 0 : clauses.hashCode());
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
    final Disjunction other = (Disjunction) obj;
    if (clauses == null) {
      if (other.clauses != null)
        return false;
    } else if (!clauses.equals(other.clauses))
      return false;
    return true;
  }

  @Override
  public BinaryConnective getConnective() {
    return BinaryConnective.OR;
  }

  @Override
  public IFormula getF1() {
    return clauses.iterator().next();
  }

  @Override
  public IFormula getF2() {
    Collection<? extends IFormula> c = HashSetFactory.make(clauses);
    c.remove(getF1());
    if (c.size() == 1) {
      return c.iterator().next();
    } else {
      return make(c);
    }
  }

  public static IMaxTerm make(Collection<? extends IFormula> clauses) {
    assert clauses.size() >= 2;
    Collection<IFormula> newClauses = HashSetFactory.make();
    for (IFormula c : clauses) {
      if (c instanceof Disjunction) {
        Disjunction d = (Disjunction) c;
        newClauses.addAll(d.clauses);
      } else {
        if (AdHocSemiDecisionProcedure.singleton().isTautology(c)) {
          return BooleanConstantFormula.TRUE;
        } else if (!AdHocSemiDecisionProcedure.singleton().isContradiction(c)) {
          newClauses.add(AdHocSemiDecisionProcedure.normalize(c));
        }
      }
    }
    if (newClauses.isEmpty()) {
      return (BooleanConstantFormula.FALSE);
    } else if (newClauses.size() == 1) {
      IFormula f = newClauses.iterator().next();
      assert f instanceof IMaxTerm;
      return (IMaxTerm) f;
    } else {
      return new Disjunction(newClauses);
    }
  }

  public Collection<? extends IFormula> getClauses() {
    return Collections.unmodifiableCollection(clauses);
  }

  @Override
  public String toString() {
    return prettyPrint(DefaultDecorator.instance());
  }

  public Collection<? extends IMaxTerm> getMaxTerms() {
    return Collections.singleton(this);
  }

}
