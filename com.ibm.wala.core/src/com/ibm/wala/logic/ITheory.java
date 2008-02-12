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
 * A theory is a set of sentences
 * 
 * @author sjfink
 *
 */
public interface ITheory {
  
  public IVocabulary getVocabulary();

  /**
   * get all sentences in theory
   * TODO: remove this, replace with getSentencesRelevantToConstraints(empty)? MS
   * @return the sentences
   */
  public Collection<? extends IFormula> getSentences();
  
  /**
   * 
   * @param constraints
   * @return those sentences in this theory relevant to constraints
   */
  public Collection<? extends IFormula> getSentencesRelevantToConstraints(Collection<? extends IFormula> constraints);
  
}
