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
import com.ibm.wala.types.MethodReference;

/**
 * 
 * A representation of a method used to communicate analysis results.
 * 
 * @author sfink
 */
public class MethodImpl extends MemberImpl implements IMethod {

  private final String descriptor;

  public MethodImpl(MethodReference M) {
    super(M);
    descriptor = M.getDescriptor().toString();
  }


  public String getDescriptor() {
    return descriptor;
  }

  /**
   * A signature is a string like:
   * com.foo.bar.createLargeOrder(IILjava.lang.String;SLjava.sql.Date;)Ljava.lang.Integer;
   * 
   * @return String representation of the signature
   */
  public String getSignature() {
    String s = getDeclaringClass().toString().substring(1).replace('/', '.') + "." + getName() + getDescriptor();
    return s;
  }


  public String toString() {
    return getSignature();
  }


  public boolean equals(Object arg0) {
    if (arg0 == null) {
      return false;
    }
    if (getClass().equals(arg0.getClass())) {
      MethodImpl other = (MethodImpl) arg0;
      return getDeclaringClass().equals(other.getDeclaringClass()) && getClassLoaderName().equals(other.getClassLoaderName())
          && getName().equals(other.getName()) && descriptor.equals(other.descriptor);
    } else {
      return false;
    }
  }


  public int hashCode() {
    return getDeclaringClass().hashCode() * 4001 + getClassLoaderName().hashCode() * 4003 + getName().hashCode()
        + descriptor.hashCode();
  }


  public String getSelector() {
    return getName() + descriptor;
  }

}
