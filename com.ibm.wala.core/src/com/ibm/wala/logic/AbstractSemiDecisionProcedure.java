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

/**
 * Abstract base class for decision procedures.
 * 
 * @author sjfink
 */
public abstract class AbstractSemiDecisionProcedure implements ISemiDecisionProcedure {
 
  public  boolean isTautology(IFormula f) {
    Collection<IMaxTerm> emptyTheory = Collections.emptySet();
    return isTautology(f, emptyTheory);
  }

  public  boolean isContradiction(IFormula f) {
    Collection<IMaxTerm> emptyTheory = Collections.emptySet();
    return isContradiction(f, emptyTheory);
  }

}
