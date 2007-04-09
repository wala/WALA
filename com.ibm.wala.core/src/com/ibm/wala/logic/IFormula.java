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
 * Formula := P(Term, ....) 
 *         |  NOT(Formula)
 *         |  Formula CONNECTIVE Formula
 *         |  QUANTIFIER Formula
 * 
 * @author sjfink
 *
 */
public interface IFormula {
  static enum Kind {
    RELATION, NEGATION, BINARY, QUANTIFIED
  }
  
  public boolean isAtomic();

  public Kind getKind();
  
  public Collection<Variable> getFreeVariables();
}
