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
import com.ibm.wala.util.debug.Assertions;

/**
 *
 * This is a context which is defined by a pair consisting of 
 * <caller node, base context>.
 * 
 * The base context is typically some special case; e.g.,
 * a JavaTypeContext used for reflection.
 * 
 * @author sfink
 */
public class CallerContextPair extends CallerContext {


  private final Context baseContext;
  /**
   * @param caller the node which defines this context.
   */
  public CallerContextPair(CGNode caller, Context baseContext) {
    super(caller);
    this.baseContext = baseContext;
    if (Assertions.verifyAssertions) {
      // avoid recursive contexts for now.
      Assertions._assert(!(baseContext instanceof CallerContextPair));
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.detox.ipa.callgraph.Context#get(com.ibm.detox.ipa.callgraph.ContextKey)
   */
  @Override
  public ContextItem get(ContextKey name) {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    if (name.equals(ContextKey.CALLER)) {
      return super.get(name);
    } else {
      return baseContext.get(name);
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      CallerContextPair other = (CallerContextPair)obj;
      return getCaller().equals(other.getCaller()) && baseContext.equals(other.baseContext);
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return 8377 * getCaller().hashCode() + baseContext.hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Caller: " + getCaller() + ",Base:" + baseContext;
  }

}
