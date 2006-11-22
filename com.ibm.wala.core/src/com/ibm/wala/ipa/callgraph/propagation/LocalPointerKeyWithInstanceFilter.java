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
 *
 * a local pointer key that carries a type filter
 * 
 * @author sfink
 */
public final class LocalPointerKeyWithInstanceFilter extends LocalPointerKeyWithFilter implements InstanceFilteredPointerKey {

  private final InstanceKey filter;

  /**
   * 
   */
  public LocalPointerKeyWithInstanceFilter(CGNode node, int valueNumber, InstanceKey filter) {
    super(node,valueNumber,filter.getConcreteType());
    this.filter = filter;
  }


  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PointerKey#getTypeFilter()
   */
  public InstanceKey getInstanceFilter() {
    return filter;
  }

}
