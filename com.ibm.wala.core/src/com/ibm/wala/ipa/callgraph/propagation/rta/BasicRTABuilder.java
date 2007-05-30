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
package com.ibm.wala.ipa.callgraph.propagation.rta;

import java.util.Iterator;

import com.ibm.wala.analysis.reflection.CloneInterpreter;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.fixpoint.IntSetVariable;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.warnings.ResolutionFailure;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * @author sfink
 * 
 * TODO: refactor to eliminate more redundancy with SSACallGraphBuilder
 */
public class BasicRTABuilder extends AbstractRTABuilder {

  public BasicRTABuilder(ClassHierarchy cha, WarningSet warnings, AnalysisOptions options,
      ContextSelector contextSelector, SSAContextInterpreter contextInterpreter) {
    super(cha, warnings, options, contextSelector, contextInterpreter, null);
  }

  /**
   * Perform needed bookkeeping when a new class is discovered.
   * 
   * @param klass
   */
  @Override
  protected void updateSetsForNewClass(IClass klass, InstanceKey iKey, CGNode node, NewSiteReference n) {

    // set up the selector map to record each method that klass implements
    registerImplementedMethods(klass, iKey);

    try {
      for (Iterator ifaces = klass.getAllImplementedInterfaces().iterator(); ifaces.hasNext();) {
        IClass c = (IClass) ifaces.next();
        registerImplementedMethods(c, iKey);
      }
      klass = klass.getSuperclass();
      while (klass != null) {
        registerImplementedMethods(klass, iKey);
        klass = klass.getSuperclass();
      }
    } catch (ClassHierarchyException e) {
      Assertions.UNREACHABLE();
    }
  }

  /**
   * Record state for each method implemented by iKey.
   */
  private void registerImplementedMethods(IClass declarer, InstanceKey iKey) {
    if (DEBUG) {
      Trace.println("registerImplementedMethods: " + declarer + " " + iKey);
    }
    for (Iterator it = declarer.getDeclaredMethods().iterator(); it.hasNext();) {
      IMethod M = (IMethod) it.next();
      Selector selector = M.getReference().getSelector();
      PointerKey sKey = getKeyForSelector(selector);
      if (DEBUG) {
        Trace.println("Add constraint: " + selector + " U= " + iKey.getConcreteType());
      }
      system.newConstraint(sKey, iKey);
    }
  }

  @Override
  protected PointerKey getKeyForSite(CallSiteReference site) {
    return new RTASelectorKey(site.getDeclaredTarget().getSelector());
  }

  protected RTASelectorKey getKeyForSelector(Selector selector) {
    return new RTASelectorKey(selector);
  }
  /**
   * An operator to fire when we discover a potential new callee for a virtual
   * or interface call site.
   * 
   * This operator will create a new callee context and constraints if
   * necessary.
   * 
   * N.B: This implementation assumes that the calling context depends solely on
   * the dataflow information computed for the receiver. TODO: generalize this
   * to have other forms of context selection, such as CPA-style algorithms.
   */
  private final class DispatchOperator extends UnaryOperator {
    private final CallSiteReference site;
    private final ExplicitCallGraph.ExplicitNode caller;

    DispatchOperator(CallSiteReference site, ExplicitNode caller) {
      this.site = site;
      this.caller = caller;
    }

    /**
     * The set of classes that have already been processed.
     */
    private MutableIntSet previousReceivers = IntSetUtil.getDefaultIntSetFactory().make();

    @Override
    public byte evaluate(IVariable lhs, IVariable rhs) {
      IntSetVariable receivers = (IntSetVariable) rhs;

      // compute the set of pointers that were not previously handled
      IntSet value = receivers.getValue();
      if (value == null) {
        // this constraint was put on the work list, probably by initialization,
        // even though the right-hand-side is empty.
        // TODO: be more careful about what goes on the worklist to
        // avoid this.
        if (DEBUG) {
          Trace.println("EVAL dispatch with value null");
        }
        return NOT_CHANGED;
      }
      if (DEBUG) {
        String S = "EVAL dispatch to " + caller + ":" + site;
        Trace.println(S);
        if (DEBUG_LEVEL >= 2) {
          Trace.println("receivers: " + value);
        }
      }
      // TODO: cache this!!!
      IClass recvClass = getClassHierarchy().lookupClass(site.getDeclaredTarget().getDeclaringClass());
      if (recvClass == null) {
        getWarnings().add(ResolutionFailure.create(caller,site.getDeclaredTarget().getDeclaringClass()));
        return NOT_CHANGED;
      }
      value = filterForClass(value, recvClass);
      if (DEBUG_LEVEL >= 2) {
        Trace.println("filtered value: " + value);
      }
      IntSetAction action = new IntSetAction() {
        public void act(int ptr) {
          if (DEBUG) {
            Trace.println("    dispatch to ptr " + ptr);
          }
          InstanceKey iKey = system.getInstanceKey(ptr);

          CGNode target = getTargetForCall(caller, site, iKey);
          if (target == null) {
            // This indicates an error; I sure hope getTargetForCall
            // raised a warning about this!
            if (DEBUG) {
              Trace.println("Warning: null target for call " + site + " " + iKey);
            }
            return;
          }
          if (clone2Assign) {
            if (target.getMethod().getReference().equals(CloneInterpreter.CLONE)) {
              // (mostly) ignore a call to clone: it won't affect the
              // solution, but we should probably at least have a call
              // edge
              caller.addTarget(site, target);
              return;
            }
          }

          IntSet targets = caller.getPossibleTargetNumbers(site);
          if (targets != null && targets.contains(target.getGraphNodeId())) {
            // do nothing; we've previously discovered and handled this
            // receiver for this call site.
            return;
          }

          // process the newly discovered target for this call
          processResolvedCall(caller, site, target);

          if (!haveAlreadyVisited(target)) {
            markDiscovered(target);
          }
        }
      };

      value.foreachExcluding(previousReceivers, action);

      // update the set of receivers previously considered
      previousReceivers.copySet(value);

      return NOT_CHANGED;
    }

    @Override
    public String toString() {
      return "Dispatch";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.dataflow.Operator#hashCode()
     */
    @Override
    public int hashCode() {
      return caller.hashCode() + 8707 * site.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.dataflow.Operator#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
      if (o instanceof DispatchOperator) {
        DispatchOperator other = (DispatchOperator)o;
        return caller.equals(other.caller) && site.equals(other.site);
      } else {
        return false;
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.rta.AbstractRTABuilder#makeDispatchOperator(com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  protected UnaryOperator makeDispatchOperator(CallSiteReference site, CGNode node) {
    return new DispatchOperator(site, (ExplicitNode)node);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#getDefaultDispatchBoundHeuristic()
   */
  @Override
  protected byte getDefaultDispatchBoundHeuristic() {
    return AnalysisOptions.NO_DISPATCH_BOUND;
  }
}