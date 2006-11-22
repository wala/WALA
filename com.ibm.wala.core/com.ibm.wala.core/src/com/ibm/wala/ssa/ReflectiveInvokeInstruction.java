/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import com.ibm.wala.classLoader.CallSiteReference;

/**
 * @author Julian Dolby (dolby@us.ibm.com)
 *
 */
public abstract class ReflectiveInvokeInstruction extends SSAAbstractInvokeInstruction {

  /**
   * The value of the function to be called.
   */
  protected final int function;

  protected ReflectiveInvokeInstruction(int function, int result, int exception, CallSiteReference site) {
    super(result, exception, site);
    this.function = function;
  }

  public int getFunction() {
    return function;
  }
}


