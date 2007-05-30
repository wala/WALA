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

import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * A key which represents the return value for a node
 */
public class ReturnValueKey extends NodeKey {
  /**
   * @param node
   */
  public ReturnValueKey(CGNode node) {
    super(node);
  }

  @Override
  public String toString() {
    return "[Ret-V:" + getNode() + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return (obj instanceof ReturnValueKey) && super.internalEquals(obj);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return 1283 * super.internalHashCode();
  }

}