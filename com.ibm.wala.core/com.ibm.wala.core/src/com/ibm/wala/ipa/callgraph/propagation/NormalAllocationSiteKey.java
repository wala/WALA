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
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * An instance key which represents the set of objects allocated at a 
 * "normal" (not multinewarray) bytecode
 */
public final class NormalAllocationSiteKey extends AllocationSiteKey {
  public NormalAllocationSiteKey(CGNode node, NewSiteReference allocation, IClass type) {
    super(node, allocation, type);
  }
  public boolean equals(Object obj) {
    // instanceof is OK because this class is final
    if (obj instanceof NormalAllocationSiteKey) {
      AllocationSiteKey other = (AllocationSiteKey) obj;
      return getNode().equals(other.getNode()) && getSite().equals(other.getSite());
    } else {
      return false;
    }
  }
  public int hashCode() {
    return getNode().hashCode() * 8647 + getSite().hashCode();
  }

}
