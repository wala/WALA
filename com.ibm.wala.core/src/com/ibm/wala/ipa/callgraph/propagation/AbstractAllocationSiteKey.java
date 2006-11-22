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

import com.ibm.wala.analysis.reflection.InstanceKeyWithNode;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.debug.Assertions;

/**
 * A base class for allocation site instance keys
 */
public abstract class AbstractAllocationSiteKey implements InstanceKeyWithNode {
  private final IClass type;
  private final CGNode node;

  public AbstractAllocationSiteKey(CGNode node, IClass type) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(node != null);
      if (type != null && type.isInterface()) {
        Assertions.UNREACHABLE("unexpected type: " + type);
      }
    }
    this.node = node;
    this.type = type;
  }

  public abstract boolean equals(Object obj);

  public abstract int hashCode();

  public abstract String toString();

  /**
   * @return the concrete type allocated
   */
  public IClass getConcreteType() {
    return type;
  }

  /**
   * @return the call graph node which contains this allocation
   */
  public CGNode getNode() {
    return node;
  }

}

