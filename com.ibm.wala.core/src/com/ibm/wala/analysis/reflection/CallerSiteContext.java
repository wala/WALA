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

package com.ibm.wala.analysis.reflection;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;

/**
 * 
 * A context which is a <CGNode, CallSiteReference> pair
 * 
 * @author sfink
 */
public class CallerSiteContext implements Context {
  private final CGNode caller;
  private final CallSiteReference site;

  /**
   * @param caller
   * @param site
   */
  public CallerSiteContext(CGNode caller, CallSiteReference site) {
    this.caller = caller;
    this.site = site;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.Context#get(com.ibm.wala.ipa.callgraph.ContextKey)
   */
  public ContextItem get(ContextKey name) {
    return null;
  }
    
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "[CallerSiteContext: " + caller + "," + site.getProgramCounter() + "]";
  }

  public CGNode getCaller() {
    return caller;
  }

  public CallSiteReference getSite() {
    return site;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object arg0) {
    if (getClass().equals(arg0.getClass())) {
      CallerSiteContext other = (CallerSiteContext) arg0;
      return caller.equals(other.caller) && (site.equals(other.site));
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return site.hashCode() + 839 * caller.hashCode();
  }

}
