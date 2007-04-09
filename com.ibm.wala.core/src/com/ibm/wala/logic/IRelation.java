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

/**
 * an n-ary relational predicate symbol
 * 
 * @author sjfink
 *
 */
public interface IRelation {
  
  /**
   * @return the arity, or valence of this relation
   */
  int getValence();

  /**
   * @return a string which uniquely identifies this relation
   */
  String getSymbol();

}
