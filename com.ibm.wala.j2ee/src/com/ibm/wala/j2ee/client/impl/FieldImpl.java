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

import com.ibm.wala.j2ee.client.IField;
import com.ibm.wala.types.FieldReference;

/**
 *
 * Object to track a field in analysis results
 * 
 * @author sfink
 */
public class FieldImpl extends MemberImpl implements IField {

  /**
   * @param f data structure representing a field
   */
  public FieldImpl(FieldReference f) {
    super(f);
  }
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return getDeclaringClass() + "." + getName();
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.j2ee.client.impl.MemberImpl#equals(java.lang.Object)
   */
  public boolean equals(Object arg0) {
    if (arg0 == null) {
      return false;
    }
    if (getClass().equals(arg0.getClass())) {
      FieldImpl other = (FieldImpl)arg0;
      return getName().equals(other.getName()) && getClassLoaderName().equals(other.getClassLoaderName());
    } else {
      return false;
    }
  }
}
