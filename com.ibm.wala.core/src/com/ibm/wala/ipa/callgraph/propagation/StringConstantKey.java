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
public final class StringConstantKey implements InstanceKey {
  private final String string;
  private final IClass stringClass;
  public StringConstantKey(String string, IClass stringClass) {
    this.string = string;
    this.stringClass = stringClass;
  }
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof StringConstantKey) {
      StringConstantKey other = (StringConstantKey) obj;
      return string.equals(other.string);
    } else {
      return false;
    }
  }
  @Override
  public int hashCode() {
    return 1877*string.hashCode();
  }
  @Override
  public String toString() {
    return "[" + string + "]";
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.InstanceKey#getConcreteType()
   */
  public IClass getConcreteType() {
    return stringClass;
  }
  public String getString() {
    return string;
  }
}
