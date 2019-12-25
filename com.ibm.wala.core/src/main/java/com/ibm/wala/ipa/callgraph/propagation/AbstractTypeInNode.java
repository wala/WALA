/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.analysis.reflection.InstanceKeyWithNode;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.debug.Assertions;

/**
 * Abstract base class for {@link InstanceKey} which represents at least some {@link IClass} in some
 * {@link CGNode}.
 */
public abstract class AbstractTypeInNode implements InstanceKeyWithNode {
  private final IClass type;

  private final CGNode node;

  public AbstractTypeInNode(CGNode node, IClass type) {
    if (node == null) {
      throw new IllegalArgumentException("null node");
    }
    if (type != null && type.isInterface()) {
      Assertions.UNREACHABLE("unexpected type: " + type);
    }
    this.node = node;
    this.type = type;
  }

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  @Override
  public abstract String toString();

  /** @return the concrete type allocated */
  @Override
  public IClass getConcreteType() {
    return type;
  }

  /** @return the call graph node which contains this allocation */
  @Override
  public CGNode getNode() {
    return node;
  }
}
