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

/** a local pointer key that carries a type filter */
public class LocalPointerKeyWithFilter extends LocalPointerKey implements FilteredPointerKey {

  private final TypeFilter typeFilter;

  public LocalPointerKeyWithFilter(CGNode node, int valueNumber, TypeFilter typeFilter) {
    super(node, valueNumber);
    assert typeFilter != null;
    this.typeFilter = typeFilter;
  }

  @Override
  public TypeFilter getTypeFilter() {
    return typeFilter;
  }
}
