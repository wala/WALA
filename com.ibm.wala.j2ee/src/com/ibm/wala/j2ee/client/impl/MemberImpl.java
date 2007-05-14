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

import com.ibm.wala.j2ee.client.IClass;
import com.ibm.wala.types.MemberReference;

/**
 *
 * A representation of a field used to communicate analysis results.
 * 
 * @author sfink
 */
public abstract class MemberImpl {

  private final IClass declaringClass;
  private final String name;

  public MemberImpl(MemberReference m) {
    this.declaringClass = new ClassImpl(m.getDeclaringClass().getName().toString(),m.getDeclaringClass().getClassLoader().getName().toString());
    this.name = m.getName().toString();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.atk.Method#getDeclaringClass()
   */
  public IClass getDeclaringClass() {
    return declaringClass;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.atk.Method#getClassLoaderName()
   */
  public String getClassLoaderName() {
    return declaringClass.getClassLoaderName();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.atk.Method#getName()
   */
  public String getName() {
    return name;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return getDeclaringClass().hashCode() * 4001 + getClassLoaderName().hashCode() * 4003 + getName().hashCode();
  }
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public abstract boolean equals(Object arg0);
}
