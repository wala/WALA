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
 * @author sjfink
 *
 */
public class NotFormula implements IFormula {

  private final IFormula f;

  private NotFormula(final IFormula f) {
    super();
    this.f = f;
  }
  
  public static NotFormula make(IFormula f) {
    return new NotFormula(f);
  }

  public Kind getKind() {
    return Kind.NEGATION;
  }

  public IFormula getFormula() {
    return f;
  }

  public Collection<Variable> getFreeVariables() {
    return f.getFreeVariables();
  }
  
  @Override
  public String toString() {
    return "not(" + f + ")";
  }
  public boolean isAtomic() {
    return false;
  }
}
