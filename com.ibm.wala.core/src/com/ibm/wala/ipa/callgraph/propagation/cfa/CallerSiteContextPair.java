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

/**
 * This is a context which is defined by a pair consisting of &lt;caller node, base context&gt;.
 * 
 * The base context is typically some special case; e.g., a JavaTypeContext used for reflection.
 */
public class CallerSiteContextPair extends CallerSiteContext {

  private final Context baseContext;

  /**
   * @param caller the node which defines this context.
   */
  public CallerSiteContextPair(CGNode caller, CallSiteReference callSite, Context baseContext) {
    super(caller, callSite);
    if (caller == null) {
      throw new IllegalArgumentException("null caller");
    }
    this.baseContext = baseContext;
    // avoid recursive contexts for now.
    assert !(baseContext instanceof CallerContextPair);
  }

  @Override
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

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      CallerSiteContextPair other = (CallerSiteContextPair) obj;
      return getCaller().equals(other.getCaller()) && getCallSite().equals(other.getCallSite())
          && baseContext.equals(other.baseContext);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return super.hashCode() + baseContext.hashCode();
  }

  @Override
  public String toString() {
    return super.toString() + ",Base:" + baseContext;
  }

}
