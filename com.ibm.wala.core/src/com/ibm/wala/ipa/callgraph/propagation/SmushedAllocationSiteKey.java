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
import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * An instance key which represents a unique set for ALL allocation sites of a
 * given type in a CGNode
 */
public class SmushedAllocationSiteKey extends AbstractAllocationSiteKey {
  public SmushedAllocationSiteKey(CGNode node, IClass type) {
    super(node, type);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    // instanceof is OK because this class is final
    if (obj instanceof SmushedAllocationSiteKey) {
      SmushedAllocationSiteKey other = (SmushedAllocationSiteKey) obj;
      return getNode().equals(other.getNode()) && getConcreteType().equals(other.getConcreteType());
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getNode().hashCode() * 8293 + getConcreteType().hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "SMUSHED " + getNode() + " : " + getConcreteType();
  }
}
