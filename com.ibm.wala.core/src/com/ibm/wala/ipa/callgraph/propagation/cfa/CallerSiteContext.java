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
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;

/**
 * @author Julain Dolby (?)
 *
 */
class CallerSiteContext extends CallerContext {

  private final CallSiteReference callSite;

  CallerSiteContext(CGNode caller, CallSiteReference callSite) {
    super(caller);
    this.callSite = callSite;
  }

  public ContextItem get(ContextKey name) {
    if (name.equals(ContextKey.CALLSITE)) {
      return callSite;
    } else {
      return super.get(name);
    }
  }

  public boolean equals(Object obj) {
    if (getClass().equals(obj.getClass())) {
      CallerSiteContext other = (CallerSiteContext)obj;
      return getCaller().equals(other.getCaller()) &&
	     callSite.equals(other.callSite);
    } else {
      return false;
    }
  }

  public int hashCode() {
    return callSite.hashCode() * super.hashCode();
  }

  public String toString() {
    return super.toString() + "@" + callSite.getProgramCounter();
  }

  public CallSiteReference getCallSite() {
    return callSite;
  }

}
