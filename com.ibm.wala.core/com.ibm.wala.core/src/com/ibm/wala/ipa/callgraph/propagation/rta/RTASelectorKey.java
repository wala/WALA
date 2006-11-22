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

package com.ibm.wala.ipa.callgraph.propagation.rta;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * This RTA implementation tracks a single set of Classes for each Selector
 * 
 * @author sfink
 */
public class RTASelectorKey implements PointerKey {

  private final Selector selector;
  RTASelectorKey(Selector selector) {
    this.selector = selector;
  }

  public int hashCode() {
    return 131 * selector.hashCode();
  }
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object arg0) {
    if (arg0.getClass().equals(getClass())) {
      RTASelectorKey other = (RTASelectorKey)arg0;
      return selector.equals(other.selector);
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "RTAKey:" + selector.toString();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PointerKey#getTypeFilter()
   */
  public IClass getTypeFilter() {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }
  
}
