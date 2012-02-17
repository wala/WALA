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
package com.ibm.wala.ipa.callgraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.IntSet;

/**
 * A ContextSelector that behaves as the composition of a list of child
 * selectors. Note that we deliberately do not document what order the
 * composition is performed in, with the hope that the {@link ContextKey}s
 * understood by the {@link Context}s returned by distinct children do not
 * overlap.
 * 
 */
public class ComposedContextSelector implements ContextSelector {

  private final List<ContextSelector> childSelectors;

  public ComposedContextSelector(Collection<ContextSelector> childSelectors) {
    this.childSelectors = new ArrayList<ContextSelector>(childSelectors);
  }
  
  public ComposedContextSelector(ContextSelector... childSelectors) {
    this.childSelectors = Arrays.asList(childSelectors);
  }

  /**
   * return a context representing the composition of all the non-null child
   * contexts. Elides the {@link Everywhere#EVERYWHERE} context when some child
   * returns a context that is not {@link Everywhere#EVERYWHERE}.
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
    List<Context> childResults = new ArrayList<Context>(1);
    boolean sawEverywhere = false;
    for (ContextSelector child : childSelectors) {
      Context childResult = child.getCalleeTarget(caller, site, callee, actualParameters);
      if (childResult != null) {
        childResults.add(childResult);
        if (sawEverywhere) {
          assert !childResult.equals(Everywhere.EVERYWHERE);
        } else {
          sawEverywhere = childResult.equals(Everywhere.EVERYWHERE);
        }
      }
    }
    if (childResults.isEmpty()) {
      return null;
    }
    boolean elideEverywhere = sawEverywhere && childResults.size() > 1;
    Context result = null;
    for (Context c : childResults) {
      if (!elideEverywhere || !c.equals(Everywhere.EVERYWHERE)) {
        result = (result == null) ? c : new DelegatingContext(c, result);
      }
    }
    return result;
  }

  /**
   * return the union of the relevant parameters for all children
   */
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    IntSet result = null;
    for (ContextSelector child : childSelectors) {
      IntSet childResult = child.getRelevantParameters(caller, site);
      if (result == null) {
        result = childResult;
      } else {
        result = result.union(childResult);
      }
    }
    return result;
  }

}
