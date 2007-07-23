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
 * Term := Constant
 *      |  Variable
 *      |  f(Term,...)
 * 
 * @author sjfink
 *
 */
public interface ITerm {
  static enum Kind {
    CONSTANT, VARIABLE, FUNCTION
  }
  
  public Kind getKind();

  public Collection<Variable> getFreeVariables();

  public String prettyPrint(ILogicDecorator d);

  public Collection<? extends IConstant> getConstants();

  /**
   * Collect all terms that appear in this term, including subterms if this is a function term
   */
  public Collection<? extends ITerm> getAllTerms();
}
