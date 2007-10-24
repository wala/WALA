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
 * An n-ary function
 * 
 * @author sjfink
 *
 */
public interface IFunction {
  /**
   * @return the number of parameters to this function
   */
  int getNumberOfParameters();

  /**
   * @return a string which uniquely identifies this relation
   */
  String getSymbol();
  

}
