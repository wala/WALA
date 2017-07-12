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
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.IntSet;

public abstract class CallStringContextSelector implements ContextSelector {

  public static final ContextKey CALL_STRING = new ContextKey() {
    @Override
    public String toString() {
      return "CALL_STRING_KEY";
    }
  };

  public static class CallStringContextPair implements Context {
    private final CallString cs;

    private final Context base;

    public CallStringContextPair(CallString cs, Context base) {
      this.cs = cs;
      this.base = base;
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof CallStringContextPair) && ((CallStringContextPair) o).cs.equals(cs)
          && ((CallStringContextPair) o).base.equals(base);
    }

    @Override
    public String toString() {
      return "CallStringContextPair: " + cs.toString() + ":" + base.toString();
    }

    @Override
    public int hashCode() {
      return cs.hashCode() * base.hashCode();
    }

    @Override
    public ContextItem get(ContextKey name) {
      if (CALL_STRING.equals(name)) {
        return cs;
      } else {
        return base.get(name);
      }
    }

    public Context getBaseContext() {
      return base;
    }

    public CallString getCallString() {
      return cs;
    }
  }

  protected final ContextSelector base;

  public CallStringContextSelector(ContextSelector base) {
    this.base = base;
  }

  protected abstract int getLength(CGNode caller, CallSiteReference site, IMethod target);

  protected CallString getCallString(CGNode caller, CallSiteReference site, IMethod target) {
    int length = getLength(caller, site, target);
    if (length > 0) {
      if (caller.getContext().get(CALL_STRING) != null) {
        return new CallString(site, caller.getMethod(), length, (CallString) caller.getContext().get(CALL_STRING));
      } else {
        return new CallString(site, caller.getMethod());
      }
    } else {
      return null;
    }
  }

  /* 
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    Context baseContext = base.getCalleeTarget(caller, site, callee, receiver);
    CallString cs = getCallString(caller, site, callee);
    if (cs == null) {
      return baseContext;
    } else if (baseContext == Everywhere.EVERYWHERE) {
      return new CallStringContext(cs);
    } else {
      return new CallStringContextPair(cs, baseContext);
    }
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return base.getRelevantParameters(caller, site);
  }
  
}
