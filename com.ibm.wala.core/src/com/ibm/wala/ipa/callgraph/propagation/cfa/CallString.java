/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
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
import com.ibm.wala.ipa.callgraph.ContextItem;

public class CallString implements ContextItem {
  private final CallSiteReference sites[];

  private final IMethod methods[];

  public CallString(CallSiteReference site, IMethod method) {
    if (site == null) {
      throw new IllegalArgumentException("null site");
    }
    this.sites = new CallSiteReference[] { site };
    this.methods = new IMethod[] { method };
  }

  protected CallString(CallSiteReference site, IMethod method, int length, CallString base) {
    int sitesLength = Math.min(length, base.sites.length + 1);
    int methodsLength = Math.min(length, base.methods.length + 1);
    sites = new CallSiteReference[sitesLength];
    sites[0] = site;
    System.arraycopy(base.sites, 0, sites, 1, Math.min(length - 1, base.sites.length));
    methods = new IMethod[methodsLength];
    methods[0] = method;
    System.arraycopy(base.methods, 0, methods, 1, Math.min(length - 1, base.methods.length));
  }

  @Override
  public String toString() {
    StringBuffer str = new StringBuffer("[");
    for (int i = 0; i < sites.length; i++) {
      str.append(" ").append(methods[i].getSignature()).append("@").append(sites[i].getProgramCounter());
    }
    str.append(" ]");
    return str.toString();
  }

  @Override
  public int hashCode() {
    int code = 1;
    for (int i = 0; i < sites.length; i++) {
      code *= sites[i].hashCode() * methods[i].hashCode();
    }

    return code;
  }

  @Override
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

  public CallSiteReference[] getCallSiteRefs() {
    return this.sites;
  }

  public IMethod[] getMethods() {
    return this.methods;
  }

}
