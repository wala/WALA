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

import com.ibm.wala.classLoader.CallSiteReference;
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
 * @author Julian Dolby (dolby@us.ibm.com)
 * @author sjfink
 */
public class CallerSiteContextPair extends CallerSiteContext {


  private final Context baseContext;
  /**
   * @param caller the node which defines this context.
   */
  public CallerSiteContextPair(CGNode caller, CallSiteReference callSite, Context baseContext) {
    super(caller, callSite);
    this.baseContext = baseContext;
    if (Assertions.verifyAssertions) {
      // avoid recursive contexts for now.
      Assertions._assert(!(baseContext instanceof CallerContextPair));
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.detox.ipa.callgraph.Context#get(com.ibm.detox.ipa.callgraph.ContextKey)
   */
  public ContextItem get(ContextKey name) {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    if (name.equals(ContextKey.CALLER) || name.equals(ContextKey.CALLSITE)) {
      return super.get(name);
    } else {
      return baseContext.get(name);
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (getClass().equals(obj.getClass())) {
      CallerSiteContextPair other = (CallerSiteContextPair)obj;
      return getCaller().equals(other.getCaller())
	  && getCallSite().equals(other.getCallSite())
	  && baseContext.equals(other.baseContext);
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return super.hashCode() + baseContext.hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return super.toString() + ",Base:" + baseContext;
  }

}
