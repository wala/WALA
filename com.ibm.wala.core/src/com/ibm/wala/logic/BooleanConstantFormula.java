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

import java.util.Collection;
import java.util.Collections;

public class BooleanConstantFormula implements IMaxTerm {

  public static final BooleanConstantFormula TRUE = new BooleanConstantFormula(BooleanConstant.TRUE);

  public static final BooleanConstantFormula FALSE = new BooleanConstantFormula(BooleanConstant.FALSE);

  private final BooleanConstant c;

  private BooleanConstantFormula(BooleanConstant c) {
    this.c = c;
  }

  public Collection<Variable> getFreeVariables() {
    return Collections.emptySet();
  }

  public Collection<? extends IConstant> getConstants() {
    Collection<? extends IConstant> result = Collections.singleton(c);
    return result;
  }

  public Collection<? extends ITerm> getTerms() {
    return getConstants();
  }

  public Kind getKind() {
    return Kind.CONSTANT;
  }

  @Override
  public String toString() {
    return prettyPrint(DefaultDecorator.instance());
  }

  public String prettyPrint(ILogicDecorator d) {
    return d.prettyPrint(c);
  }

  @Override
  public boolean equals(Object obj) {
    // note that these are canonical
    return this == obj;
  }

  @Override
  public int hashCode() {
    return 163 * c.hashCode();
  }

  public Collection<? extends ITerm> getAllTerms() {
    return Collections.singleton(c);
  }

  public Collection<? extends IMaxTerm> getMaxTerms() {
    return Collections.singleton(this);
  }

}
