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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.wala.util.collections.HashSetFactory;

/**
 * An atomic formula of the form R(Term, ... Term)
 * 
 * @author sjfink
 * 
 */
public class RelationFormula implements IFormula {
  private final IRelation R;

  private final List<ITerm> terms;

  public IRelation getRelation() {
    return R;
  }

  public List<ITerm> getTerms() {
    return terms;
  }

  private RelationFormula(final IRelation R, final List<ITerm> terms) throws IllegalArgumentException {
    super();
    this.R = R;
    this.terms = terms;
    if (R == null) {
      throw new IllegalArgumentException("R cannot be null");
    }
  }

  public static RelationFormula make(BinaryRelation R, ITerm t1, ITerm t2) {
    ArrayList<ITerm> l = new ArrayList<ITerm>();
    l.add(t1);
    l.add(t2);
    return new RelationFormula(R, l);
  }

  public static RelationFormula make(BinaryRelation R, int i, ITerm t2) {
    return make(R, IntConstant.make(i), t2);
  }

  public static RelationFormula make(BinaryRelation R, ITerm t1, int j) {
    return make(R, t1, IntConstant.make(j));
  }

  public static RelationFormula make(BinaryRelation R, int i, int j) {
    return make(R, IntConstant.make(i), IntConstant.make(j));
  }

  public static RelationFormula makeEquals(ITerm t1, ITerm t2) {
    return make(BinaryRelation.EQUALS, t1, t2);
  }

  public static RelationFormula makeEquals(ITerm t1, int i) {
    return make(BinaryRelation.EQUALS, t1, IntConstant.make(i));
  }

  public static RelationFormula make(UnaryRelation R, ITerm t) {
    ArrayList<ITerm> l = new ArrayList<ITerm>();
    l.add(t);
    return new RelationFormula(R, l);
  }
  
  public static RelationFormula make(UnaryRelation R, int i) {
    ArrayList<ITerm> l = new ArrayList<ITerm>();
    l.add(IntConstant.make(i));
    return new RelationFormula(R, l);
  }

  public static IFormula make(IRelation relation, List<ITerm> terms) {
    return new RelationFormula(relation, terms);
  }
  
  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((R == null) ? 0 : R.hashCode());
    result = PRIME * result + ((terms == null) ? 0 : terms.hashCode());
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
    final RelationFormula other = (RelationFormula) obj;
    if (R == null) {
      if (other.R != null)
        return false;
    } else if (!R.equals(other.R))
      return false;
    if (terms == null) {
      if (other.terms != null)
        return false;
    } else if (!terms.equals(other.terms))
      return false;
    return true;
  }

  public Kind getKind() {
    return Kind.RELATION;
  }

  public Collection<Variable> getFreeVariables() {
    Collection<Variable> result = HashSetFactory.make(terms.size());
    for (ITerm t : terms) {
      result.addAll(t.getFreeVariables());
    }
    return result;
  }

  @Override
  public String toString() {
    return prettyPrint(DefaultDecorator.instance());
  }
  
  public String prettyPrint(ILogicDecorator d) {
    if (R.getValence() == 2) {
      return infixNotation(d);
    } else {
      return prefixNotation(d);
    }
  }

  private String prefixNotation(ILogicDecorator d) {
    StringBuffer result = new StringBuffer(R.getSymbol());
    result.append("(");
    for (int i = 0; i < R.getValence() - 1; i++) {
      result.append(terms.get(i).prettyPrint(d));
      result.append(",");
    }
    if (R.getValence() > 0) 
    	result.append(terms.get(R.getValence() - 1).prettyPrint(d));
    result.append(")");
    return result.toString();
  }

  private String infixNotation(ILogicDecorator d) {
    assert R.getValence() == 2;
    StringBuffer result = new StringBuffer();
    result.append(terms.get(0).prettyPrint(d));
    result.append(" ");
    result.append(R.getSymbol());
    result.append(" ");
    result.append(terms.get(1).prettyPrint(d));
    return result.toString();
  }

  public boolean isAtomic() {
    return true;
  }
}
