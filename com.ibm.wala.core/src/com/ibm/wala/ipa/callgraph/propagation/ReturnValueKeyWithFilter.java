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

import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * a helper class which can modify a PropagationCallGraphBuilder to deal with reflective factory
 * methods.
 */
public class ReturnValueKeyWithFilter extends ReturnValueKey implements FilteredPointerKey {

  private final TypeFilter typeFilter;

  public ReturnValueKeyWithFilter(CGNode node, TypeFilter typeFilter) {
    super(node);
    if (typeFilter == null) {
      throw new IllegalArgumentException("null typeFilter");
    }
    this.typeFilter = typeFilter;
  }

  @Override
  public TypeFilter getTypeFilter() {
    return typeFilter;
  }
}
