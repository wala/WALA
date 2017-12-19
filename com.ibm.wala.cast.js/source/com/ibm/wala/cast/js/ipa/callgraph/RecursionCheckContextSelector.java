/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import com.ibm.wala.analysis.reflection.InstanceKeyWithNode;
import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys.ScopeMappingInstanceKey;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.SingleInstanceFilter;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntSet;

/**
 * ensures that no contexts returned by a base context selector are recursive
 * (assertion failure otherwise)
 */
public class RecursionCheckContextSelector implements ContextSelector {

  private final ContextSelector base;
 
    
  /**
   * the highest parameter index that we'll check . this is a HACK. ideally,
   * given a context, we'd have some way to know all the {@link ContextKey}s
   * that it knows about.
   * 
   * @see ContextKey#PARAMETERS
   */
  private static final int MAX_INTERESTING_PARAM = 5;

  public RecursionCheckContextSelector(ContextSelector base) {
    this.base = base;
  }

  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
    Context baseContext = base.getCalleeTarget(caller, site, callee, actualParameters);
    assert !recursiveContext(baseContext, callee);
    return baseContext;
  }

  private static boolean recursiveContext(Context baseContext, IMethod callee) {
    if (!recursionPossible(callee)) {
      return false;
    }
    LinkedList<Pair<Context,Collection<IMethod>>> worklist = new LinkedList<>();
    worklist.push(Pair.make(baseContext, (Collection<IMethod>)Collections.singleton(callee)));
    while (!worklist.isEmpty()) {
      Pair<Context, Collection<IMethod>> p = worklist.removeFirst();
      Context curContext = p.fst;
      Collection<IMethod> curEncountered = p.snd;
      // we just do a case analysis here. we might have to add cases later to
      // account for new types of context / recursion.
      CGNode callerNode = (CGNode) curContext.get(ContextKey.CALLER);
      if (callerNode != null) {
        if (!updateForNode(baseContext, curEncountered, worklist, callerNode)) {
          System.err.println("callee " + callee);
          return true;
        }
      }
      for (int i = 0; i < MAX_INTERESTING_PARAM; i++) {
        FilteredPointerKey.SingleInstanceFilter filter = (SingleInstanceFilter) curContext.get(ContextKey.PARAMETERS[i]);
        if (filter != null) {
          InstanceKey ik = filter.getInstance();
          if (ik instanceof ScopeMappingInstanceKey) {
            ik = ((ScopeMappingInstanceKey) ik).getBase();
          }
          if (ik instanceof InstanceKeyWithNode) {
            CGNode node = ((InstanceKeyWithNode)ik).getNode();
            if (!updateForNode(baseContext, curEncountered, worklist, node)) {
              System.err.println("callee " + callee);
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean updateForNode(Context baseContext, Collection<IMethod> curEncountered, LinkedList<Pair<Context, Collection<IMethod>>> worklist, CGNode callerNode) {
    final IMethod method = callerNode.getMethod();
    if (!recursionPossible(method)) {
      assert !curEncountered.contains(method);
      return true;
    }
    if (curEncountered.contains(method)) {
      System.err.println("recursion in context on method " + method);
      System.err.println("encountered methods: ");
      for (IMethod m : curEncountered) {
        System.err.println("  " + m);
      }
      System.err.println("context " + baseContext);
      return false;
    }    
    Collection<IMethod> newEncountered = new ArrayList<>(curEncountered);
    newEncountered.add(method);
    worklist.add(Pair.make(callerNode.getContext(),newEncountered));
    return true;
  }


  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return base.getRelevantParameters(caller, site);
  }

  /**
   * is it possible for m to be involved in a recursive cycle?
   * @param m
   */
  private static boolean recursionPossible(IMethod m) {
    // object or array constructors cannot be involved
    if (m.getReference().getName().equals(JavaScriptMethods.ctorAtom)) {
      TypeReference declaringClass = m.getReference().getDeclaringClass();
      if (declaringClass.equals(JavaScriptTypes.Object) || declaringClass.equals(JavaScriptTypes.Array)) {
        return false;
      }
    }
    return true;
  }
}
