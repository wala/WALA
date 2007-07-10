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
import java.util.HashSet;

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * A disjunction of formulae
 * 
 * @author sjfink
 */
public class Disjunction extends AbstractBinaryFormula {

  // invariant: size >= 1
  private final Collection<? extends IFormula> clauses;

  private Disjunction(Collection<? extends IFormula> clauses) {
    this.clauses = clauses;
  }

  public Collection<? extends IConstant> getConstants() {
    Collection<IConstant> result = HashSetFactory.make();
    for (IFormula f : clauses) {
      result.addAll(f.getConstants());
    }
    return result;
  }
  
  public Collection<? extends ITerm> getTerms() {
    Collection<ITerm> result = HashSetFactory.make();
    for (IFormula f : clauses) {
      result.addAll(f.getTerms());
    }
    return result;
  }

  public Collection<Variable> getFreeVariables() {
    Collection<Variable> result = HashSetFactory.make();
    for (IFormula f : clauses) {
      result.addAll(f.getFreeVariables());
    }
    return result;
  }

  public boolean isAtomic() {
    return false;
  }

  public String prettyPrint(ILogicDecorator d) {
    if (clauses.size() == 1) {
      return getF1().prettyPrint(d);
    } else {
      StringBuffer result = new StringBuffer();
      result.append(" ( ");
      result.append(getF1().prettyPrint(d));
      result.append(" ) ");
      result.append(d.prettyPrint(getConnective()));
      result.append(" ( ");
      result.append(getF2().prettyPrint(d));
      result.append(" )");
      return result.toString();
    }
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
    // if clauses.size() == 1, we fake this by saying we are getF1() OR false
    if (clauses.size() == 1) {
      return BooleanConstantFormula.FALSE;
    } else {
      Collection<IFormula> c = new HashSet<IFormula>(clauses);
      c.remove(getF1());
      return make(c);
    }
  }

  public static Disjunction make(Collection<? extends IFormula> clauses) {
    for (IFormula c: clauses) {
      assert !(c instanceof Disjunction);
    }
    return new Disjunction(clauses);
  }

  public Collection<? extends IFormula> getClauses() {
    return Collections.unmodifiableCollection(clauses);
  }

  public static Disjunction make(BooleanConstantFormula f) {
    Collection<? extends IFormula> c = Collections.singleton(f);
    return new Disjunction(c);
  }
  

  @Override
  public String toString() {
    return prettyPrint(DefaultDecorator.instance());
  }

}
