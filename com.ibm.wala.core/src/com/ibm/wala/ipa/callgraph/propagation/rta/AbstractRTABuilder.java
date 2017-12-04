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

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.analysis.reflection.ReflectionContextInterpreter;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.callgraph.impl.FakeWorldClinitMethod;
import com.ibm.wala.ipa.callgraph.propagation.ClassBasedInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.IPointsToSolver;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.StandardSolver;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultPointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * Abstract superclass of various RTA flavors
 */
public abstract class AbstractRTABuilder extends PropagationCallGraphBuilder {

  protected final static int DEBUG_LEVEL = 0;

  protected final static boolean DEBUG = (DEBUG_LEVEL > 0);

  private final static int VERBOSE_INTERVAL = 10000;

  private final static int PERIODIC_MAINTAIN_INTERVAL = 10000;

  /**
   * Should we change calls to clone() to assignments?
   */
  protected final boolean clone2Assign = true;

  /**
   * set of classes whose clinit are processed
   */
  protected final Set<IClass> clinitProcessed = HashSetFactory.make();

  /**
   * set of classes (IClass) discovered to be allocated
   */
  protected final HashSet<IClass> allocatedClasses = HashSetFactory.make();

  /**
   * set of class names that are implicitly pre-allocated Note: for performance reasons make sure java.lang.Object comes first
   */
  private final static TypeReference[] PRE_ALLOC = {
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Object"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ArithmeticException"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ArrayStoreException"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ClassCastException"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ClassNotFoundException"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/IndexOutOfBoundsException"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/NegativeArraySizeException"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ExceptionInInitializerError"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/NullPointerException") };

  protected AbstractRTABuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
      ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter) {
    super(cha, options, cache, new DefaultPointerKeyFactory());
    setInstanceKeys(new ClassBasedInstanceKeys(options, cha));
    setContextSelector(makeContextSelector(appContextSelector));
    setContextInterpreter(makeContextInterpreter(appContextInterpreter));
  }

  protected RTAContextInterpreter getRTAContextInterpreter() {
    return getContextInterpreter();
  }

  /**
   * Visit all instructions in a node, and add dataflow constraints induced by each statement relevat to RTA
   */
  @Override
  protected boolean addConstraintsFromNode(CGNode node, IProgressMonitor monitor) {

    if (haveAlreadyVisited(node)) {
      return false;
    } else {
      markAlreadyVisited(node);
    }
    if (DEBUG) {
      System.err.println(("\n\nAdd constraints from node " + node));
    }

    // add all relevant constraints
    addNewConstraints(node);
    addCallConstraints(node);
    addFieldConstraints(node);
    // conservatively assume something changed.
    return true;
  }

  /**
   * Add a constraint for each allocate
   */
  private void addNewConstraints(CGNode node) {
    for (NewSiteReference n : Iterator2Iterable.make(getRTAContextInterpreter().iterateNewSites(node))) {
      visitNew(node, n);
    }
  }

  /**
   * Add a constraint for each invoke
   */
  private void addCallConstraints(CGNode node) {
    for (CallSiteReference c : Iterator2Iterable.make(getRTAContextInterpreter().iterateCallSites(node))) {
      visitInvoke(node, c);
    }
  }

  /**
   * Handle accesses to static fields
   */
  private void addFieldConstraints(CGNode node) {
    for (FieldReference f : Iterator2Iterable.make(getRTAContextInterpreter().iterateFieldsRead(node))) {
      processFieldAccess(f);
    }
    for (FieldReference f : Iterator2Iterable.make(getRTAContextInterpreter().iterateFieldsWritten(node))) {
      processFieldAccess(f);
    }
  }

  /**
   * Is s is a getstatic or putstatic, then potentially add the relevant <clinit>to the newMethod set.
   */
  private void processFieldAccess(FieldReference f) {
    if (DEBUG) {
      System.err.println(("processFieldAccess: " + f));
    }
    TypeReference t = f.getDeclaringClass();
    IClass klass = getClassHierarchy().lookupClass(t);
    if (klass == null) {
    } else {
      processClassInitializer(klass);
    }
  }

  protected void processClassInitializer(IClass klass) {

    if (clinitProcessed.contains(klass)) {
      return;
    }
    clinitProcessed.add(klass);

    if (klass.getClassInitializer() != null) {
      if (DEBUG) {
        System.err.println(("process class initializer for " + klass));
      }

      // add an invocation from the fake root method to the <clinit>
      FakeWorldClinitMethod fakeWorldClinitMethod = (FakeWorldClinitMethod) callGraph.getFakeWorldClinitNode().getMethod();
      MethodReference m = klass.getClassInitializer().getReference();
      CallSiteReference site = CallSiteReference.make(1, m, IInvokeInstruction.Dispatch.STATIC);
      IMethod targetMethod = options.getMethodTargetSelector().getCalleeTarget(callGraph.getFakeRootNode(), site, null);
      if (targetMethod != null) {
        CGNode target = callGraph.getNode(targetMethod, Everywhere.EVERYWHERE);
        if (target == null) {
          SSAInvokeInstruction s = fakeWorldClinitMethod.addInvocation(null, site);
          try {
            target = callGraph.findOrCreateNode(targetMethod, Everywhere.EVERYWHERE);
            processResolvedCall(callGraph.getFakeWorldClinitNode(), s.getCallSite(), target);
          } catch (CancelException e) {
            if (DEBUG) {
              System.err.println("Could not add node for class initializer: " + targetMethod.getSignature()
                  + " due to constraints on the maximum number of nodes in the call graph.");
              return;
            }
          }
        }
      }
    }

    klass = klass.getSuperclass();
    if (klass != null && !clinitProcessed.contains(klass))
      processClassInitializer(klass);
  }

  /**
   * Add a constraint for a call instruction
   * 
   * @throws IllegalArgumentException if site is null
   */
  public void visitInvoke(CGNode node, CallSiteReference site) {

    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    if (DEBUG) {
      System.err.println(("visitInvoke: " + site));
    }

    // if non-virtual, add callgraph edges directly
    IInvokeInstruction.IDispatch code = site.getInvocationCode();

    if (code == IInvokeInstruction.Dispatch.STATIC) {
      CGNode n = getTargetForCall(node, site, null, null);
      if (n != null) {
        processResolvedCall(node, site, n);

        // side effect of invoke: may call class initializer
        processClassInitializer(cha.lookupClass(site.getDeclaredTarget().getDeclaringClass()));
      }
    } else {

      // Add a side effect that will fire when we determine a value
      // for the receiver. This side effect will create a new node
      // and new constraints based on the new callee context.

      // TODO: special case logic for dispatch to "final" - type stuff, where
      // receiver and context can be determined a priori ... like we currently
      // do for invokestatic above.
      PointerKey selector = getKeyForSite(site);
      if (selector == null) {
        return;
      }

      if (DEBUG) {
        System.err.println(("Add side effect, dispatch to " + site));
      }
      UnaryOperator<PointsToSetVariable> dispatchOperator = makeDispatchOperator(site, node);
      system.newSideEffect(dispatchOperator, selector);
    }
  }

  protected abstract UnaryOperator<PointsToSetVariable> makeDispatchOperator(CallSiteReference site, CGNode node);

  protected abstract PointerKey getKeyForSite(CallSiteReference site);

  /**
   * Add constraints for a call site after we have computed a reachable target for the dispatch
   * 
   * Side effect: add edge to the call graph.
   */
  @SuppressWarnings("deprecation")
  void processResolvedCall(CGNode caller, CallSiteReference site, CGNode target) {

    if (DEBUG) {
      System.err.println(("processResolvedCall: " + caller + " ," + site + " , " + target));
    }
    caller.addTarget(site, target);

    if (FakeRootMethod.isFakeRootMethod(caller.getMethod().getReference())) {
      if (entrypointCallSites.contains(site)) {
        callGraph.registerEntrypoint(target);
      }
    }

    if (!haveAlreadyVisited(target)) {
      markDiscovered(target);
    }
  }

  /**
   * Add a constraint for an allocate
   * 
   * @throws IllegalArgumentException if newSite is null
   */
  public void visitNew(CGNode node, NewSiteReference newSite) {

    if (newSite == null) {
      throw new IllegalArgumentException("newSite is null");
    }
    if (DEBUG) {
      System.err.println(("visitNew: " + newSite));
    }
    InstanceKey iKey = getInstanceKeyForAllocation(node, newSite);
    if (iKey == null) {
      // something went wrong. I hope someone raised a warning.
      return;
    }
    IClass klass = iKey.getConcreteType();

    if (DEBUG) {
      System.err.println(("iKey: " + iKey + " " + system.findOrCreateIndexForInstanceKey(iKey)));
    }

    if (klass == null) {
      return;
    }
    if (allocatedClasses.contains(klass)) {
      return;
    }
    allocatedClasses.add(klass);
    updateSetsForNewClass(klass, iKey, node, newSite);

    // side effect of new: may call class initializer
    processClassInitializer(klass);
  }

  /**
   * Perform needed bookkeeping when a new class is discovered.
   * 
   * @param klass
   */
  protected abstract void updateSetsForNewClass(IClass klass, InstanceKey iKey, CGNode node, NewSiteReference ns);

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#customInit()
   */
  @Override
  protected void customInit() {
    super.customInit();

    FakeRootMethod m = (FakeRootMethod) getCallGraph().getFakeRootNode().getMethod();

    for (TypeReference element : PRE_ALLOC) {
      SSANewInstruction n = m.addAllocation(element);
      // visit now to ensure java.lang.Object is visited first
      visitNew(getCallGraph().getFakeRootNode(), n.getNewSite());
    }
  }

  /**
   * @return set of IClasses determined to be allocated
   */
  @SuppressWarnings("unchecked")
  public Set<IClass> getAllocatedTypes() {
    return (Set<IClass>) allocatedClasses.clone();
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#makeSolver()
   */
  @Override
  protected IPointsToSolver makeSolver() {
    return new StandardSolver(system, this);
  }

  protected ContextSelector makeContextSelector(ContextSelector appContextSelector) {
    ContextSelector def = new DefaultContextSelector(options, cha);
    ContextSelector contextSelector = appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);
    return contextSelector;
  }

  protected SSAContextInterpreter makeContextInterpreter(SSAContextInterpreter appContextInterpreter) {

    SSAContextInterpreter defI = new DefaultSSAInterpreter(getOptions(), getAnalysisCache());
    defI = new DelegatingSSAContextInterpreter(ReflectionContextInterpreter.createReflectionContextInterpreter(cha, getOptions(),
        getAnalysisCache()), defI);
    SSAContextInterpreter contextInterpreter = appContextInterpreter == null ? defI : new DelegatingSSAContextInterpreter(
        appContextInterpreter, defI);
    return contextInterpreter;
  }

  @Override
  protected boolean unconditionallyAddConstraintsFromNode(CGNode node, IProgressMonitor monitor) {
    // add all relevant constraints
    addNewConstraints(node);
    addCallConstraints(node);
    addFieldConstraints(node);
    markAlreadyVisited(node);
    return true;
  }

  @Override
  protected ExplicitCallGraph createEmptyCallGraph(IClassHierarchy cha, AnalysisOptions options) {
    return new DelegatingExplicitCallGraph(cha, options, getAnalysisCache());
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#makeSystem(com.ibm.wala.ipa.callgraph.AnalysisOptions)
   */
  @Override
  protected PropagationSystem makeSystem(AnalysisOptions options) {
    PropagationSystem result = super.makeSystem(options);
    result.setVerboseInterval(VERBOSE_INTERVAL);
    result.setPeriodicMaintainInterval(PERIODIC_MAINTAIN_INTERVAL);
    return result;
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.CallGraphBuilder#getPointerAnalysis()
   */
  @Override
  public PointerAnalysis<InstanceKey> getPointerAnalysis() {
    return TypeBasedPointerAnalysis.make(getOptions(), allocatedClasses, getCallGraph());
  }
}
