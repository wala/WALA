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
public abstract class AbstractVariable extends AbstractTerm implements Comparable<AbstractVariable> {

  private final int number;

  protected AbstractVariable(int number) {
    this.number = number;
  }

  public final Kind getKind() {
    return Kind.VARIABLE;
  }

  @Override
  public final int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + number;
    return result;
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final AbstractVariable other = (AbstractVariable) obj;
    if (number != other.number)
      return false;
    return true;
  }

  public final int getNumber() {
    return number;
  }
  
  @Override
  public String toString() {
    return "v" + getNumber();
  }

  public String prettyPrint(ILogicDecorator d) {
    return d.prettyPrint(this);
  }

  public final Collection<AbstractVariable> getFreeVariables() {
    return Collections.singleton(this);
  }
  
  public final Collection<? extends IConstant> getConstants() {
    return Collections.emptySet();
  }

  public final int compareTo(AbstractVariable o) throws NullPointerException {
    return this.getNumber() - o.getNumber();
  }

  public final Collection<? extends ITerm> getAllTerms() {
    return Collections.singleton(this);
  }
}
