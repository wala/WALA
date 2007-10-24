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
 * Vocabulary of a calculus
 * 
 * @param <T> the type of constants in this vocabulary
 * 
 * @author sjfink
 *
 */
public interface IVocabulary<T extends IConstant> {
  
  Collection<AbstractVariable> getVariables();
  
  Collection<T> getConstants();

  Collection<? extends IRelation> getRelations();
  
  Collection<? extends IFunction> getFunctions();
}
