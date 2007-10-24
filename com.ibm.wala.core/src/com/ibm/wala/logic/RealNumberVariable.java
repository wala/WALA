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
 * A term that represents an unconstrained real number variable in a formula
 * 
 * @author sjfink
 */
public class RealNumberVariable extends AbstractNumberedVariable {

  protected RealNumberVariable(int number) {
   super(number);
  }

  public static RealNumberVariable make(int number) {
    return new RealNumberVariable(number);
  }
}
