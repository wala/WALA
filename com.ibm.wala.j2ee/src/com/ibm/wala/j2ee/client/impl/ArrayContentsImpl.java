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

import com.ibm.wala.j2ee.client.IArrayContents;
import com.ibm.wala.j2ee.client.IClass;

/**
 *
 * Object to track array contents in analysis results
 * 
 * @author sfink
 */
public class ArrayContentsImpl implements IArrayContents {

  private final IClass klass;
  
  public ArrayContentsImpl(IClass klass) {
    this.klass = klass;
  }
  

  public IClass getDeclaredClass() {
    return klass;
  }

  public String toString() {
    return "ArrayContents:" + klass;
  }

  public boolean equals(Object arg0) {
    if (getClass().equals(arg0.getClass())) {
      ArrayContentsImpl that = (ArrayContentsImpl)arg0;
      return klass.equals(that.klass);
    } else {
      return false;
    }
  }

  public int hashCode() {
    return 653 * klass.hashCode();
  }
}
