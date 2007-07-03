/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.logic;

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;
import com.ibm.wala.logic.ILogicConstants.Quantifier;

public class DefaultDecorator implements ILogicDecorator {

  private final static DefaultDecorator INSTANCE = new DefaultDecorator();

  protected DefaultDecorator() {
  }

  public static DefaultDecorator instance() {
    return INSTANCE;
  }

  public String prettyPrint(BinaryConnective b) {
    return b.toString();
  }

  public String prettyPrint(BooleanConstant c) {
    return c.toString();
  }

  public String prettyPrint(Variable v) {
    return v.toString();
  }

  public String prettyPrint(Quantifier q) {
    return q.toString();
  }

  public String prettyPrint(IConstant constant) {
    return constant.toString();
  }

  public String prettyPrint(FunctionTerm term) {
    StringBuffer result = new StringBuffer(term.getFunction().getSymbol());
    result.append("(");
    for (int i = 0; i < term.getFunction().getNumberOfParameters() - 1; i++) {
      result.append(term.getParameters().get(i).prettyPrint(this));
      result.append(",");
    }
    if (term.getFunction().getNumberOfParameters() > 0) {
      result.append(term.getParameters().get(term.getFunction().getNumberOfParameters() - 1).prettyPrint(this));
    }
    result.append(")");
    return result.toString();
  }
  
  public String prettyPrint(RelationFormula r) {
    if (r.getRelation().getValence() == 2) {
      return infixNotation(r);
    } else {
      return prefixNotation(r);
    }
  }

  public String prefixNotation(RelationFormula r) {
    StringBuffer result = new StringBuffer(prettyPrint(r.getRelation()));
    result.append("(");
    for (int i = 0; i < r.getRelation().getValence() - 1; i++) {
      result.append(r.getTerms().get(i).prettyPrint(this));
      result.append(",");
    }
    if (r.getRelation().getValence() > 0) {
      result.append(r.getTerms().get(r.getRelation().getValence() - 1).prettyPrint(this));
    }
    result.append(")");
    return result.toString();
  }

  public String infixNotation(RelationFormula r) {
    assert r.getRelation().getValence() == 2;
    StringBuffer result = new StringBuffer();
    result.append(r.getTerms().get(0).prettyPrint(this));
    result.append(" ");
    result.append(prettyPrint(r.getRelation()));
    result.append(" ");
    result.append(r.getTerms().get(1).prettyPrint(this));
    return result.toString();
  }

  public String prettyPrint(IRelation r) {
    return r.getSymbol();
  }

}
