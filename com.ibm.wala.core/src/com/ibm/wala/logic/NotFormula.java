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

/**
 * A formula of the form not(f)
 * 
 * @author sjfink
 */
public class NotFormula implements IFormula {

  private final IFormula f;

  protected NotFormula(final IFormula f) throws IllegalArgumentException {
    super();
    this.f = f;
    if (f == null) {
      throw new IllegalArgumentException("f cannot be null");
    }
  }

  public static IFormula make(IFormula f) {
    switch (f.getKind()) {
    case RELATION:
      RelationFormula r = (RelationFormula) f;
      if (r.getRelation().equals(BinaryRelation.EQUALS)) {
        return RelationFormula.make(BinaryRelation.NE, r.getTerms());
      }
      if (r.getRelation().equals(BinaryRelation.NE)) {
        return RelationFormula.make(BinaryRelation.EQUALS, r.getTerms());
      }
      if (r.getRelation().equals(BinaryRelation.GE)) {
        return RelationFormula.make(BinaryRelation.LT, r.getTerms());
      }
      if (r.getRelation().equals(BinaryRelation.GT)) {
        return RelationFormula.make(BinaryRelation.LE, r.getTerms());
      }
      if (r.getRelation().equals(BinaryRelation.LE)) {
        return RelationFormula.make(BinaryRelation.GT, r.getTerms());
      }
      if (r.getRelation().equals(BinaryRelation.LT)) {
        return RelationFormula.make(BinaryRelation.GE, r.getTerms());
      }
      return new NotFormula(f);
    case CONSTANT:
      if (f.equals(BooleanConstantFormula.TRUE)) {
        return BooleanConstantFormula.FALSE;
      } else {
        assert f.equals(BooleanConstantFormula.FALSE);
        return BooleanConstantFormula.TRUE;
      }
    case BINARY:
    case NEGATION:
    case QUANTIFIED:
      default:
        return new NotFormula(f);
    }
  }

  public Kind getKind() {
    return Kind.NEGATION;
  }

  public IFormula getFormula() {
    return f;
  }

  public Collection<AbstractVariable> getFreeVariables() {
    return f.getFreeVariables();
  }
  
  public Collection<? extends IConstant> getConstants() {
    return f.getConstants();
  }

  public Collection<? extends ITerm> getAllTerms() {
    return f.getAllTerms();
  }

  @Override
  public String toString() {
    return prettyPrint(DefaultDecorator.instance());
  }

  public String prettyPrint(ILogicDecorator d) {
    return d.prettyPrint(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((f == null) ? 0 : f.hashCode());
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
    final NotFormula other = (NotFormula) obj;
    if (f == null) {
      if (other.f != null)
        return false;
    } else if (!f.equals(other.f))
      return false;
    return true;
  }
}
