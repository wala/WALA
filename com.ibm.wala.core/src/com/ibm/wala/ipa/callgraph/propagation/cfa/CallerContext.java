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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;

/**
 *
 * This is a context which is defined by the caller node.
 * 
 * @author sfink
 */
public class CallerContext implements Context {

  private final CGNode caller;

  /**
   * @param caller the node which defines this context.
   */
  public CallerContext(CGNode caller) {
    this.caller = caller;
  }

  /* (non-Javadoc)
   * @see com.ibm.detox.ipa.callgraph.Context#get(com.ibm.detox.ipa.callgraph.ContextKey)
   */
  public ContextItem get(ContextKey name) {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    if (name.equals(ContextKey.CALLER)) {
      return caller;
    } else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (getClass().equals(obj.getClass())) {
      CallerContext other = (CallerContext)obj;
      return caller.equals(other.caller);
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return 7841 * caller.hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "Caller: " + caller;
  }

  public CGNode getCaller() {
    return caller;
  }

}
