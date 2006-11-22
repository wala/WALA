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
package com.ibm.wala.j2ee.client.impl;

import com.ibm.wala.j2ee.client.IMethod;
import com.ibm.wala.j2ee.client.IProgramLocation;

/**
 * @author sfink
 */
public class ProgramLocation implements IProgramLocation {

  private final IMethod method;

  private final int bytecodeIndex;

  private final int lineNumber;

  public ProgramLocation(IMethod method, int bytecodeIndex, int lineNumber) {
    this.method = method;
    this.bytecodeIndex = bytecodeIndex;
    this.lineNumber = lineNumber;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.client.IProgramLocation#getMethod()
   */
  public IMethod getMethod() {
    return method;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.client.IProgramLocation#getBytecodeIndex()
   */
  public int getBytecodeIndex() {
    return bytecodeIndex;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.client.IProgramLocation#getLineNumber()
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return method + "@" + bytecodeIndex + "(line " + lineNumber + ")";
  }

}