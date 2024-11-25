/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph.cha;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.BasicCallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.ExplicitPredecessorsEdgeManager;
import com.ibm.wala.ipa.callgraph.impl.FakeWorldClinitMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.LambdaSummaryClass;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInvokeDynamicInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.ComposedIterator;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Call graph in which call targets are determined entirely based on an {@link IClassHierarchy}. */
public class CHACallGraph extends BasicCallGraph<CHAContextInterpreter> {
  private final IClassHierarchy cha;
  private final AnalysisOptions options;
  private final IAnalysisCacheView cache;

  /**
   * if set to true, do not include call graph edges in classes outside the application class
   * loader. This means callbacks from library to application will be ignored.
   */
  private final boolean applicationOnly;

  private boolean isInitialized = false;

  private class CHANode extends NodeImpl {

    protected CHANode(IMethod method, Context C) {
      super(method, C);
    }

    @Override
    public IR getIR() {
      return cache.getIR(method);
    }

    @Override
    public DefUse getDU() {
      return cache.getDefUse(cache.getIR(method));
    }

    @Override
    public Iterator<NewSiteReference> iterateNewSites() {
      return getInterpreter(this).iterateNewSites(this);
    }

    @Override
    public Iterator<CallSiteReference> iterateCallSites() {
      return getInterpreter(this).iterateCallSites(this);
    }

    @Override
    public boolean equals(Object obj) {
      return obj.getClass() == getClass() && getMethod().equals(((CHANode) obj).getMethod());
    }

    @Override
    public int hashCode() {
      return getMethod().hashCode();
    }

    @Override
    public boolean addTarget(CallSiteReference reference, CGNode target) {
      return false;
    }
  }

  /**
   * NOTE: after calling this contructor, {@link #init(Iterable)} must be invoked to complete
   * initialization
   */
  public CHACallGraph(IClassHierarchy cha) {
    this(cha, false);
  }

  /**
   * NOTE: after calling this contructor, {@link #init(Iterable)} must be invoked to complete
   * initialization
   */
  public CHACallGraph(IClassHierarchy cha, boolean applicationOnly) {
    this.cha = cha;
    this.options = new AnalysisOptions();
    this.cache = new AnalysisCacheImpl();
    this.applicationOnly = applicationOnly;
    setInterpreter(new ContextInsensitiveCHAContextInterpreter());
  }

  /**
   * Builds the call graph data structures. The call graph will only include methods reachable from
   * the provided entrypoints.
   */
  public void init(Iterable<Entrypoint> entrypoints) throws CancelException {
    super.init();

    CGNode root = getFakeRootNode();
    int programCounter = 0;
    for (Entrypoint e : entrypoints) {
      root.addTarget(e.makeSite(programCounter++), null);
    }
    newNodes.push(root);
    closure();
    isInitialized = true;
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return cha;
  }

  private final Map<CallSiteReference, Set<IMethod>> targetCache = HashMapFactory.make();

  private Iterator<IMethod> getPossibleTargets(CallSiteReference site) {
    Set<IMethod> result = targetCache.get(site);
    if (result == null) {
      if (site.isDispatch()) {
        result = cha.getPossibleTargets(site.getDeclaredTarget());
      } else {
        IMethod m = cha.resolveMethod(site.getDeclaredTarget());
        if (m != null) {
          result = Collections.singleton(m);
        } else {
          result = Collections.emptySet();
        }
      }
      targetCache.put(site, result);
    }
    return result.iterator();
  }

  @Override
  public Set<CGNode> getPossibleTargets(CGNode node, CallSiteReference site) {
    return Iterator2Collection.toSet(
        new MapIterator<>(
            new FilterIterator<>(getPossibleTargets(site), this::isRelevantMethod),
            object -> {
              try {
                return findOrCreateNode(object, Everywhere.EVERYWHERE);
              } catch (CancelException e) {
                assert false : e.toString();
                return null;
              }
            }));
  }

  @Override
  public int getNumberOfTargets(CGNode node, CallSiteReference site) {
    return IteratorUtil.count(getPossibleTargets(site));
  }

  @Override
  public Iterator<CallSiteReference> getPossibleSites(final CGNode src, final CGNode target) {
    return new FilterIterator<>(
        getInterpreter(src).iterateCallSites(src),
        o -> getPossibleTargets(src, o).contains(target));
  }

  private class CHARootNode extends CHANode {
    private final Set<CallSiteReference> calls = HashSetFactory.make();

    protected CHARootNode(IMethod method, Context C) {
      super(method, C);
    }

    @Override
    public Iterator<CallSiteReference> iterateCallSites() {
      return calls.iterator();
    }

    @Override
    public boolean addTarget(CallSiteReference reference, CGNode target) {
      return calls.add(reference);
    }
  }

  @Override
  protected CGNode makeFakeRootNode() throws CancelException {
    return new CHARootNode(
        Language.JAVA.getFakeRootMethod(cha, options, cache), Everywhere.EVERYWHERE);
  }

  @Override
  protected CGNode makeFakeWorldClinitNode() throws CancelException {
    return new CHARootNode(
        new FakeWorldClinitMethod(
            Language.JAVA.getFakeRootMethod(cha, options, cache).getDeclaringClass(),
            options,
            cache),
        Everywhere.EVERYWHERE);
  }

  private int clinitPC = 0;

  @Override
  public CGNode findOrCreateNode(IMethod method, Context C) throws CancelException {
    assert C.equals(Everywhere.EVERYWHERE);
    assert !method.isAbstract();

    CGNode n = getNode(method, C);
    if (n == null) {
      assert !isInitialized;
      n = makeNewNode(method, C);

      IMethod clinit = method.getDeclaringClass().getClassInitializer();
      if (clinit != null && getNode(clinit, Everywhere.EVERYWHERE) == null) {
        CGNode cln = makeNewNode(clinit, Everywhere.EVERYWHERE);
        CGNode clinits = getFakeWorldClinitNode();
        clinits.addTarget(
            CallSiteReference.make(
                clinitPC++, clinit.getReference(), IInvokeInstruction.Dispatch.STATIC),
            cln);
        edgeManager.addEdge(clinits, cln);
      }
    }
    return n;
  }

  private final ArrayDeque<CGNode> newNodes = new ArrayDeque<>();

  @SuppressWarnings("UnusedVariable")
  private final Set<MethodReference> functionalInterfaceMethodsForLambdas = HashSetFactory.make();

  private void closure() throws CancelException {
    while (!newNodes.isEmpty()) {
      CGNode n = newNodes.pop();
      for (CallSiteReference site : Iterator2Iterable.make(n.iterateCallSites())) {
        // It's a lambda creation site if the target is LambdaMetaFactory
        // site.getDeclaredTarget().getDeclaringClass().getName().equals(TypeReference.LambdaMetaFactory.getName())
        // In that case, we need to build an IR and create a LambdaSummaryClass using the
        // invokedynamic IR instruction,
        // add that new class to the class hierarchy, and keep track of its inferface in
        // functionalInterfaceMethodsForLambdas
        // also need its synthetic method marked as a target here
        if (isLambdaCreationSite(site)) {
          // Build an IR for the CGNode
          IR ir = n.getIR();
          SSAInvokeDynamicInstruction inst = (SSAInvokeDynamicInstruction) ir.getCalls(site)[0];
          // create the LambdaSummaryClass
          LambdaSummaryClass lambdaSummaryClass = LambdaSummaryClass.create(n, inst);
          IClass functionalInterface = lambdaSummaryClass.getDirectInterfaces().iterator().next();
          // should have a single method
          functionalInterfaceMethodsForLambdas.add(
              functionalInterface.getDeclaredMethods().iterator().next().getReference());
          IMethod target = lambdaSummaryClass.getDeclaredMethods().iterator().next();
          CGNode callee = getNode(target, Everywhere.EVERYWHERE);
          if (callee == null) {
            callee = findOrCreateNode(target, Everywhere.EVERYWHERE);
            if (n == getFakeRootNode()) {
              registerEntrypoint(callee);
            }
          }
          edgeManager.addEdge(n, callee);
        } else {
          Iterator<IMethod> methods = getPossibleTargets(site);
          while (methods.hasNext()) {
            IMethod target = methods.next();
            if (isRelevantMethod(target)) {
              CGNode callee = getNode(target, Everywhere.EVERYWHERE);
              if (callee == null) {
                callee = findOrCreateNode(target, Everywhere.EVERYWHERE);
                if (n == getFakeRootNode()) {
                  registerEntrypoint(callee);
                }
              }
              edgeManager.addEdge(n, callee);
            }
          }
        }
      }
    }
  }

  private boolean isLambdaCreationSite(CallSiteReference site) {
    return site.getDeclaredTarget()
        .getDeclaringClass()
        .getName()
        .equals(TypeReference.LambdaMetaFactory.getName());
  }

  private boolean isRelevantMethod(IMethod target) {
    return !target.isAbstract()
        && (!applicationOnly
            || cha.getScope().isApplicationLoader(target.getDeclaringClass().getClassLoader()));
  }

  private CGNode makeNewNode(IMethod method, Context C) {
    CGNode n;
    Key k = new Key(method, C);
    n = new CHANode(method, C);
    registerNode(k, n);
    newNodes.push(n);
    return n;
  }

  private class CHACallGraphEdgeManager extends ExplicitPredecessorsEdgeManager {

    protected CHACallGraphEdgeManager() {
      super(CHACallGraph.this);
    }

    @Override
    public Iterator<CGNode> getSuccNodes(final CGNode n) {
      return new FilterIterator<>(
          new ComposedIterator<>(n.iterateCallSites()) {
            @Override
            public Iterator<? extends CGNode> makeInner(CallSiteReference outer) {
              return getPossibleTargets(n, outer).iterator();
            }
          },
          new Predicate<>() {
            private final MutableIntSet nodes = IntSetUtil.make();

            @Override
            public boolean test(CGNode o) {
              if (nodes.contains(o.getGraphNodeId())) {
                return false;
              } else {
                nodes.add(o.getGraphNodeId());
                return true;
              }
            }
          });
    }

    @Override
    public int getSuccNodeCount(CGNode N) {
      return IteratorUtil.count(getSuccNodes(N));
    }

    @Override
    public void addEdge(CGNode src, CGNode dst) {
      int x = getNumber(src);
      int y = getNumber(dst);
      predecessors.add(y, x);
    }

    @Override
    public void removeEdge(CGNode src, CGNode dst) throws UnsupportedOperationException {
      assert false;
    }

    @Override
    public void removeAllIncidentEdges(CGNode node) throws UnsupportedOperationException {
      assert false;
    }

    @Override
    public void removeIncomingEdges(CGNode node) throws UnsupportedOperationException {
      assert false;
    }

    @Override
    public void removeOutgoingEdges(CGNode node) throws UnsupportedOperationException {
      assert false;
    }

    @Override
    public boolean hasEdge(CGNode src, CGNode dst) {
      return getPossibleSites(src, dst).hasNext();
    }

    @Override
    public IntSet getSuccNodeNumbers(CGNode node) {
      MutableIntSet result = IntSetUtil.make();
      for (CGNode s : Iterator2Iterable.make(getSuccNodes(node))) {
        result.add(s.getGraphNodeId());
      }
      return result;
    }
  }

  private final CHACallGraphEdgeManager edgeManager = new CHACallGraphEdgeManager();

  @Override
  protected NumberedEdgeManager<CGNode> getEdgeManager() {
    return edgeManager;
  }
}
