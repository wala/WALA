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
package com.ibm.wala.ipa.callgraph.cha;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.BasicCallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.callgraph.impl.FakeWorldClinitMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.collections.ComposedIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

public class CHACallGraph extends BasicCallGraph<CHAContextInterpreter> {
  private final IClassHierarchy cha;
  private final AnalysisOptions options;
  private final IAnalysisCacheView cache;

  /**
   * if set to true, do not include call graph edges in classes outside
   * the application class loader.  This means callbacks from library
   * to application will be ignored.
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
      return obj.getClass()==getClass() && getMethod().equals(((CHANode)obj).getMethod());
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

  public CHACallGraph(IClassHierarchy cha) {
    this(cha, false);
  }

  public CHACallGraph(IClassHierarchy cha, boolean applicationOnly) {
    this.cha = cha;
    this.options = new AnalysisOptions();
    this.cache = new AnalysisCacheImpl();
    this.applicationOnly = applicationOnly;
    setInterpreter(new ContextInsensitiveCHAContextInterpreter());
  }

  @SuppressWarnings("deprecation")
  public void init(Iterable<Entrypoint> entrypoints) throws CancelException {
    super.init();

    CGNode root = getFakeRootNode();
    int programCounter = 0;
    for(Entrypoint e : entrypoints) {
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

  private Iterator<IMethod> getPossibleTargets(CallSiteReference site) {
    if (site.isDispatch()) {
      return cha.getPossibleTargets(site.getDeclaredTarget()).iterator();
    } else {
      IMethod m = cha.resolveMethod(site.getDeclaredTarget());
      if (m != null) {
        return new NonNullSingletonIterator<IMethod>(m);
      } else {
        return EmptyIterator.instance();
      }
    }
  }

  @Override
  public Set<CGNode> getPossibleTargets(CGNode node, CallSiteReference site) {
    return Iterator2Collection.toSet(
      new MapIterator<IMethod,CGNode>(
          new FilterIterator<IMethod>(
              getPossibleTargets(site),
              new Predicate<IMethod>() {
                @Override public boolean test(IMethod o) {
                  return isRelevantMethod(o);
                }
              }
          ),
        new Function<IMethod,CGNode>() {
          @Override
          public CGNode apply(IMethod object) {
            try {
              return findOrCreateNode(object, Everywhere.EVERYWHERE);
            } catch (CancelException e) {
              assert false : e.toString();
              return null;
            }
          }
        }));
  }

  @Override
  public int getNumberOfTargets(CGNode node, CallSiteReference site) {
    return IteratorUtil.count(getPossibleTargets(site));
  }

  @Override
  public Iterator<CallSiteReference> getPossibleSites(final CGNode src, final CGNode target) {
    return
      new FilterIterator<CallSiteReference>(getInterpreter(src).iterateCallSites(src),
        new Predicate<CallSiteReference>() {
          @Override public boolean test(CallSiteReference o) {
            return getPossibleTargets(src, o).contains(target);
          }
        });
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
    return new CHARootNode(new FakeRootMethod(cha, options, cache), Everywhere.EVERYWHERE);
  }

  @Override
  protected CGNode makeFakeWorldClinitNode() throws CancelException {
    return new CHARootNode(new FakeWorldClinitMethod(cha, options, cache), Everywhere.EVERYWHERE);
  }

  private int clinitPC = 0;

  @Override
  @SuppressWarnings("deprecation")
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
        clinits.addTarget(CallSiteReference.make(clinitPC++, clinit.getReference(), IInvokeInstruction.Dispatch.STATIC), cln);
      }
    }
    return n;
  }

  private Stack<CGNode> newNodes = new Stack<CGNode>();

  private void closure() throws CancelException {
    while (! newNodes.isEmpty()) {
      CGNode n = newNodes.pop();
      for(Iterator<CallSiteReference> sites = n.iterateCallSites(); sites.hasNext(); ) {
        Iterator<IMethod> methods = getPossibleTargets(sites.next());
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
          }
        }
      }
    }
  }

  private boolean isRelevantMethod(IMethod target) {
    return !target.isAbstract()
        && (!applicationOnly 
            || cha.getScope().isApplicationLoader(target.getDeclaringClass().getClassLoader()));
  }

  private CGNode makeNewNode(IMethod method, Context C) throws CancelException {
    CGNode n;
    Key k = new Key(method, C);
    n = new CHANode(method, C);
    registerNode(k, n);
    newNodes.push(n);
    return n;
  }

  @Override
  protected NumberedEdgeManager<CGNode> getEdgeManager() {
    return new NumberedEdgeManager<CGNode>() {
      private final Map<CGNode, SoftReference<Set<CGNode>>> predecessors = HashMapFactory.make();

      private Set<CGNode> getPreds(CGNode n) {
        if (predecessors.containsKey(n) && predecessors.get(n).get() != null) {
          return predecessors.get(n).get();
        } else {
          Set<CGNode> preds = HashSetFactory.make();
          for(CGNode node : CHACallGraph.this) {
            if (getPossibleSites(node, n).hasNext()) {
              preds.add(node);
            }
          }
          predecessors.put(n, new SoftReference<Set<CGNode>>(preds));
          return preds;
        }
      }

      @Override
      public Iterator<CGNode> getPredNodes(CGNode n) {
        return getPreds(n).iterator();
      }

      @Override
      public int getPredNodeCount(CGNode n) {
        return getPreds(n).size();
      }

      @Override
      public Iterator<CGNode> getSuccNodes(final CGNode n) {
        return new FilterIterator<CGNode>(new ComposedIterator<CallSiteReference, CGNode>(n.iterateCallSites()) {
          @Override
          public Iterator<? extends CGNode> makeInner(CallSiteReference outer) {
            return getPossibleTargets(n, outer).iterator();
          }
        },
        new Predicate<CGNode>() {
          private final MutableIntSet nodes = IntSetUtil.make();
          @Override public boolean test(CGNode o) {
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
        assert false;
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
        for(Iterator<CGNode> ss = getSuccNodes(node); ss.hasNext(); ) {
          result.add(ss.next().getGraphNodeId());
        }
        return result;
      }

      @Override
      public IntSet getPredNodeNumbers(CGNode node) {
        MutableIntSet result = IntSetUtil.make();
        for(Iterator<CGNode> ss = getPredNodes(node); ss.hasNext(); ) {
          result.add(ss.next().getGraphNodeId());
        }
        return result;
      }

    };
  }

}
