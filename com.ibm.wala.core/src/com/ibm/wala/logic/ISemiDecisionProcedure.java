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

/**
 * @author sjfink
 */
public interface ISemiDecisionProcedure {
  
  /**
   * @return true if we can prove f is a contradiction
   */
  public boolean isContradiction(IFormula f);
  
  /**
   * @return true if we can prove f is a tautology
   */
  public boolean isTautology(IFormula f);
  
  /**
   * @param facts
   *            formulae that can be treated as axioms
   * @return true if we can prove f is a tautology
   * @throws IllegalArgumentException
   *             if facts == null
   */
  public boolean isTautology(IFormula f, Collection<IMaxTerm> facts) throws IllegalArgumentException;

  /**
   * @param facts
   *            formulae that can be treated as axioms
   * @return true if we can prove f is a contradiction
   * @throws IllegalArgumentException
   *             if facts == null
   */
  public boolean isContradiction(IFormula f, Collection<IMaxTerm> facts) throws IllegalArgumentException;

}
