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
import com.ibm.wala.util.warnings.WarningSet;

public abstract class CallStringContextSelector implements ContextSelector {



  private static final ContextKey CALL_STRING = new ContextKey() {
    public String toString() {
      return "CALL_STRING_KEY";
    }
  };

  private class CallString implements ContextItem {
    private final CallSiteReference sites[];

    private final IMethod methods[];

    private CallString(CallSiteReference site, IMethod method) {
      this.sites = new CallSiteReference[] { site };
      this.methods = new IMethod[] { method };
    }

    private CallString(CallSiteReference site, IMethod method, int length, CallString base) {
      sites = new CallSiteReference[length];
      sites[0] = site;
      System.arraycopy(base.sites, 0, sites, 1, Math.min(length - 1, base.sites.length));
      methods = new IMethod[length];
      methods[0] = method;
      System.arraycopy(base.methods, 0, methods, 1, Math.min(length - 1, base.methods.length));
    }

    public String toString() {
      StringBuffer str = new StringBuffer("[");
      for (int i = 0; i < sites.length; i++)
        str.append(" ").append(methods[i].getName()).append("@").append(sites[i].getProgramCounter());
      str.append(" ]");
      return str.toString();
    }

    public int hashCode() {
      int code = 1;
      for (int i = 0; i < sites.length; i++) {
        code *= sites[i].hashCode() * methods[i].hashCode();
      }

      return code;
    }

    public boolean equals(Object o) {
      if (o instanceof CallString) {
        CallString oc = (CallString) o;
        if (oc.sites.length == sites.length) {
          for (int i = 0; i < sites.length; i++) {
            if (!(sites[i].equals(oc.sites[i]) && methods[i].equals(oc.methods[i]))) {
              return false;
            }
          }

          return true;
        }
      }

      return false;
    }
  }

  private class CallStringContext implements Context {
    private final CallString cs;

    private CallStringContext(CallString cs) {
      this.cs = cs;
    }

    public boolean equals(Object o) {
      return (o instanceof CallStringContext) && ((CallStringContext) o).cs.equals(cs);
    }

    public int hashCode() {
      return cs.hashCode();
    }

    public String toString() {
      return "CallStringContext: " + cs.toString();
    }

    public ContextItem get(ContextKey name) {
      if (CALL_STRING.equals(name)) {
        return cs;
      } else {
        return null;
      }
    }
  };

  private class CallStringContextPair implements Context {
    private final CallString cs;

    private final Context base;

    private CallStringContextPair(CallString cs, Context base) {
      this.cs = cs;
      this.base = base;
    }

    public boolean equals(Object o) {
      return (o instanceof CallStringContextPair) && ((CallStringContextPair) o).cs.equals(cs)
          && ((CallStringContextPair) o).base.equals(base);
    }

    public String toString() {
      return "CallStringContextPair: " + cs.toString() + ":" + base.toString();
    }

    public int hashCode() {
      return cs.hashCode() * base.hashCode();
    }

    public ContextItem get(ContextKey name) {
      if (CALL_STRING.equals(name)) {
        return cs;
      } else {
        return base.get(name);
      }
    }
  };

  private final ContextSelector base;

  public CallStringContextSelector(ContextSelector base) {
    this.base = base;
  }

  protected abstract int getLength(CGNode caller, CallSiteReference site, IMethod target);

  private CallString getCallString(CGNode caller, CallSiteReference site, IMethod target) {
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

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
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
  
  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    return base.mayUnderstand(caller, site, targetMethod, instance);
  }

  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference site, IMethod targetMethod) {
    return base.getBoundOnNumberOfTargets(caller, site, targetMethod);
  }

  public void setWarnings(WarningSet newWarnings) {
    base.setWarnings(newWarnings);
  }

  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    return false;
  }

  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return false;
  }
}
