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

import com.ibm.wala.logic.ILogicConstants.Quantifier;

public class QuantifiedFormula implements IMaxTerm {

  private final IFormula f;

  private final Quantifier q;

  private final AbstractNumberedVariable boundV;

  private QuantifiedFormula(final Quantifier q, final AbstractNumberedVariable boundV, final IFormula f) {
    super();
    this.f = f;
    this.q = q;
    this.boundV = boundV;
  }

  public static QuantifiedFormula forall(AbstractNumberedVariable v, IFormula formula) {
    return new QuantifiedFormula(Quantifier.FORALL, v, formula);
  }

  public static QuantifiedFormula forall(AbstractNumberedVariable v1, AbstractNumberedVariable v2, IFormula formula) {
    return new QuantifiedFormula(Quantifier.FORALL, v1, forall(v2, formula));
  }

  public static QuantifiedFormula forall(AbstractNumberedVariable v1, AbstractNumberedVariable v2, AbstractNumberedVariable v3, IFormula formula) {
    return new QuantifiedFormula(Quantifier.FORALL, v1, forall(v2, v3, formula));
  }

  public static IFormula make(Quantifier q, AbstractNumberedVariable v, IFormula f) {
    return new QuantifiedFormula(q, v, f);
  }

  public Kind getKind() {
    return Kind.QUANTIFIED;
  }

  public AbstractNumberedVariable getBoundVar() {
    return boundV;
  }

  public IFormula getFormula() {
    return f;
  }

  public Quantifier getQuantifier() {
    return q;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((boundV == null) ? 0 : boundV.hashCode());
    result = PRIME * result + ((f == null) ? 0 : f.hashCode());
    result = PRIME * result + ((q == null) ? 0 : q.hashCode());
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
    final QuantifiedFormula other = (QuantifiedFormula) obj;
    if (boundV == null) {
      if (other.boundV != null)
        return false;
    } else if (!boundV.equals(other.boundV))
      return false;
    if (f == null) {
      if (other.f != null)
        return false;
    } else if (!f.equals(other.f))
      return false;
    if (q == null) {
      if (other.q != null)
        return false;
    } else if (!q.equals(other.q))
      return false;
    return true;
  }

  public Collection<AbstractNumberedVariable> getFreeVariables() {
    Collection<AbstractNumberedVariable> result = f.getFreeVariables();
    result.remove(boundV);
    return result;
  }

  public Collection<? extends IConstant> getConstants() {
    return f.getConstants();
  }

  public Collection<? extends ITerm> getAllTerms() {
    return f.getAllTerms();
  }

  @Override
  public String toString() {
////    if (getBoundVar().getRange() == null) {
////      return getQuantifier() + " " + getBoundVar()  + "." + getFormula();
////    } else {
//      return getQuantifier() + " " + getBoundVar() + getBoundVar().getRange() + "." + getFormula();
//    }
    return getQuantifier() + " " + getBoundVar()  + "." + getFormula();
  }

  public String prettyPrint(ILogicDecorator d) throws IllegalArgumentException {
    if (d == null) {
      throw new IllegalArgumentException("d == null");
    }
    return d.prettyPrint(getQuantifier()) + " " + d.prettyPrint(getBoundVar()) + "." + getFormula().prettyPrint(d);
  }
  
  public Collection<? extends IMaxTerm> getMaxTerms() {
    return Collections.singleton(this);
  }
}
