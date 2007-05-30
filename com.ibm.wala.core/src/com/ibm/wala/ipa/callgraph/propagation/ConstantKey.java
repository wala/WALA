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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.IClass;


/**
 * An instance key which represents a unique set for each String constant
 */
public final class ConstantKey implements InstanceKey {
  private final Object value;
  private final IClass valueClass;
  public ConstantKey(Object value, IClass valueClass) {
    this.value = value;
    this.valueClass = valueClass;
  }
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ConstantKey) {
      ConstantKey other = (ConstantKey) obj;
      return value==null? other.value==null: value.equals(other.value);
    } else {
      return false;
    }
  }
  @Override
  public int hashCode() {
    return value==null? 65535: 1877*value.hashCode();
  }
  @Override
  public String toString() {
    if (value == null)
      return "[ConstantKey:null]";
    else
      return "[ConstantKey:" + value + ":" + value.getClass() + "]";
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.InstanceKey#getConcreteType()
   */
  public IClass getConcreteType() {
    return valueClass;
  }
  public Object getValue() {
    return value;
  }
}
