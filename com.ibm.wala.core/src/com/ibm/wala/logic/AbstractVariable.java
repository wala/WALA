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

/**
 * A term that represents a variable in a formula
 * 
 * @author sjfink
 */
public abstract class AbstractVariable extends AbstractTerm {


  protected AbstractVariable() {
  }

  public final Kind getKind() {
    return Kind.VARIABLE;
  }

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  
  @Override
  public abstract String toString();

  public String prettyPrint(ILogicDecorator d) throws IllegalArgumentException {
    if (d == null) {
      throw new IllegalArgumentException("d == null");
    }
    return d.prettyPrint(this);
  }

  public final Collection<AbstractVariable> getFreeVariables() {
    return Collections.singleton(this);
  }
  
  public final Collection<? extends IConstant> getConstants() {
    return Collections.emptySet();
  }

  public final Collection<? extends ITerm> getAllTerms() {
    return Collections.singleton(this);
  }
}
