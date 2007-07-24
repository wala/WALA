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

import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Vocabulary of a calculus
 * 
 * @param <T> the type of constants in this vocabulary
 * 
 * @author sjfink
 *
 */
public interface IVocabulary<T> {
  
  /**
   * The range [low,high] of integer values which are considered valid assignments for variables in this vocabulary
   */
  public IntPair getDomain();
  
  /**
   * each i \in getDomain() maps to a constant of type T
   */
  OrdinalSetMapping<T> getConstants();

  Collection<? extends IRelation> getRelations();
  
  Collection<? extends IFunction> getFunctions();
}
