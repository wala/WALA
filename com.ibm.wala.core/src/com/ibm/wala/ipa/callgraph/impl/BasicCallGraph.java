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
package com.ibm.wala.ipa.callgraph.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.reflection.JavaTypeContext;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.ReceiverInstanceContext;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.DelegatingNumberedNodeManager;
import com.ibm.wala.util.graph.impl.NodeWithNumber;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * Basic data structure support for a call graph.
 */
public abstract class BasicCallGraph<T> extends AbstractNumberedGraph<CGNode> implements CallGraph {

  private static final boolean DEBUG = false;

  private final DelegatingNumberedNodeManager<CGNode> nodeManager = new DelegatingNumberedNodeManager<>();

  /**
   * A fake root node for the graph
   */
  private CGNode fakeRoot;

  /**
   * A node which handles all calls to class initializers
   */
  private CGNode fakeWorldClinit;

  /**
   * An object that handles context interpreter functions
   */
  private T interpreter;

  /**
   * Set of nodes that are entrypoints for this analysis
   */
  private final Set<CGNode> entrypointNodes = HashSetFactory.make();

  /**
   * A mapping from Key to NodeImpls in the graph. Note that each node is created on demand. This Map does not include the root
   * node.
   */
  final private Map<Key, CGNode> nodes = HashMapFactory.make();

  /**
   * A mapping from MethodReference to Set of nodes that represent this methodReference.
   * 
   * TODO: rhs of mapping doesn't have to be a set if it's a singleton; could be a node instead.
   * 
   * TODO: this is a bit redundant with the nodes Map. Restructure these data structures for space efficiency.
   */
  protected final Map<MethodReference, Set<CGNode>> mr2Nodes = HashMapFactory.make();

  public BasicCallGraph() {
    super();
  }

  @SuppressWarnings("deprecation")
  public void init() throws CancelException {
    fakeRoot = makeFakeRootNode();
    Key k = new Key(fakeRoot.getMethod(), fakeRoot.getContext());
    registerNode(k, fakeRoot);
    fakeWorldClinit = makeFakeWorldClinitNode();
    if (fakeWorldClinit != null) {
      k = new Key(fakeWorldClinit.getMethod(), fakeWorldClinit.getContext());
      registerNode(k, fakeWorldClinit);

      // add a call from fakeRoot to fakeWorldClinit
      CallSiteReference site = CallSiteReference.make(1, fakeWorldClinit.getMethod().getReference(),
          IInvokeInstruction.Dispatch.STATIC);
      // note that the result of addInvocation is a different site, with a different program counter!
      site = ((AbstractRootMethod) fakeRoot.getMethod()).addInvocation(null, site).getCallSite();
      fakeRoot.addTarget(site, fakeWorldClinit);
    }
  }

  protected abstract CGNode makeFakeRootNode() throws CancelException;

  protected abstract CGNode makeFakeWorldClinitNode() throws CancelException;

  /**
   * Use with extreme care.
   * 
   * @throws CancelException TODO
   */
  public abstract CGNode findOrCreateNode(IMethod method, Context C) throws CancelException;

  protected void registerNode(Key K, CGNode N) {
    nodes.put(K, N);
    addNode(N);
    Set<CGNode> s = findOrCreateMr2Nodes(K.m);
    s.add(N);
    if (DEBUG) {
      System.err.println(("registered Node: " + N + " for key " + K));
      System.err.println(("now size = " + getNumberOfNodes()));
    }
  }

  private Set<CGNode> findOrCreateMr2Nodes(IMethod method) {
    Set<CGNode> result = mr2Nodes.get(method.getReference());
    if (result == null) {
      result = HashSetFactory.make(3);
      mr2Nodes.put(method.getReference(), result);
    }
    return result;
  }

  protected CGNode getNode(Key K) {
    return nodes.get(K);
  }

  @Override
  public CGNode getFakeRootNode() {
    return fakeRoot;
  }

  @Override
  public CGNode getFakeWorldClinitNode() {
    return fakeWorldClinit;
  }

  /**
   * record that a node is an entrypoint
   */
  public void registerEntrypoint(CGNode node) {
    entrypointNodes.add(node);
  }

  /**
   * Note: not all successors of the root node are entrypoints
   */
  @Override
  public Collection<CGNode> getEntrypointNodes() {
    return entrypointNodes;
  }

  /**
   * A class that represents the a normal node in a call graph.
   */
  public abstract class NodeImpl extends NodeWithNumber implements CGNode {

    /**
     * The method this node represents.
     */
    protected final IMethod method;

    /**
     * The context this node represents.
     */
    private final Context context;

    protected NodeImpl(IMethod method, Context C) {
      this.method = method;
      this.context = C;
      if (method != null && !method.isSynthetic() && method.isAbstract()) {
        assert !method.isAbstract() : "Abstract method " + method;
      }
      assert C != null;
    }

    @Override
    public IMethod getMethod() {
      return method;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public abstract int hashCode();

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return "Node: " + method.toString() + " Context: " + context.toString();
    }

    @Override
    public Context getContext() {
      return context;
    }

    @Override
    public abstract boolean addTarget(CallSiteReference reference, CGNode target);

    @Override
    public IClassHierarchy getClassHierarchy() {
      return method.getClassHierarchy();
    }
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("");
    for (CGNode n : Iterator2Iterable.make(DFS.iterateDiscoverTime(this, new NonNullSingletonIterator<>(getFakeRootNode())))) {
      result.append(nodeToString(this, n) + "\n");
    }
    return result.toString();
  }

  public static String nodeToString(CallGraph CG, CGNode n) {
    StringBuffer result = new StringBuffer(n.toString() +  "\n");
     if (n.getMethod() != null) {
      for (CallSiteReference site : Iterator2Iterable.make(n.iterateCallSites())) {
        Iterator<CGNode> targets = CG.getPossibleTargets(n, site).iterator();
        if (targets.hasNext()) {
          result.append(" - " + site + "\n");
        }
        for (CGNode target : Iterator2Iterable.make(targets)) {
          result.append("     -> " + target + "\n");
        }
      }
    }
    return result.toString();
  }

  @Override
  public void removeNodeAndEdges(CGNode N) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  /**
   * @return NodeImpl, or null if none found
   */
  @Override
  public CGNode getNode(IMethod method, Context C) {
    Key key = new Key(method, C);
    return getNode(key);
  }

  protected final static class Key {
    private final IMethod m;

    private final Context C;

    public Key(IMethod m, Context C) {
      assert m != null : "null method";
      assert C != null : "null context";
      this.m = m;
      this.C = C;
    }

    @Override
    public int hashCode() {
      return 17 * m.hashCode() + C.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      assert o instanceof Key;
      Key other = (Key) o;
      return (m.equals(other.m) && C.equals(other.C));
    }

    @Override
    public String toString() {
      return "{" + m + "," + C + "}";
    }

  }

  @Override
  public Set<CGNode> getNodes(MethodReference m) {
    IMethod im = getClassHierarchy().resolveMethod(m);
    if (im == null) {
      return Collections.emptySet();
    }
    Set<CGNode> result = mr2Nodes.get(im.getReference());
    Set<CGNode> empty = Collections.emptySet();
    return (result == null) ? empty : result;
  }

  /**
   * @param node a call graph node we want information about
   * @return an object that knows how to interpret information about the node
   */
  protected T getInterpreter(CGNode node) {
    if (interpreter == null) {
      throw new IllegalStateException("must register an interpreter for this call graph");
    }
    return interpreter;
  }

  /**
   * We override this since this class supports remove() on nodes, but the superclass doesn't.
   * 
   * @see com.ibm.wala.util.graph.Graph#getNumberOfNodes()
   */
  @Override
  public int getNumberOfNodes() {
    return nodes.size();
  }

  /**
   * We override this since this class supports remove() on nodes, but the superclass doesn't.
   * 
   * @see com.ibm.wala.util.graph.Graph#iterator()
   */
  @Override
  public Iterator<CGNode> iterator() {
    return nodes.values().iterator();
  }

  /**
   * This implementation is necessary because the underlying SparseNumberedGraph may not support node membership tests.
   * 
   * @throws IllegalArgumentException if N is null
   * 
   */
  @Override
  public boolean containsNode(CGNode N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    return getNode(N.getMethod(), N.getContext()) != null;
  }

  public void setInterpreter(T interpreter) {
    this.interpreter = interpreter;
  }

  @Override
  protected NumberedNodeManager<CGNode> getNodeManager() {
    return nodeManager;
  }
  
  public void summarizeByPackage() {
   Map<String, Integer> packages = HashMapFactory.make();
   for(CGNode n : this) { 
     String nm = n.getMethod().getDeclaringClass().getName().toString() + "/" + n.getMethod().getName() + "/" + n.getContext().getClass().toString();
  
     if (n.getContext() instanceof ReceiverInstanceContext) {
       nm = nm + "/" + ((ReceiverInstanceContext)n.getContext()).getReceiver().getConcreteType().getName();
     } else if (n.getContext() instanceof JavaTypeContext) {
       nm = nm + "/" + ((JavaTypeContext)n.getContext()).getType().getTypeReference().getName();
     }
     
     do {
       if (packages.containsKey(nm)) {
         packages.put(nm, 1 + packages.get(nm));
       } else {
         packages.put(nm, 1);
       }
       
       if (nm.indexOf('/') < 0) {
         break;
       }
       
       nm = nm.substring(0, nm.lastIndexOf('/'));
     } while (true);
   }
   
   System.err.println("dump of CG");
   for(Map.Entry<String, Integer> e : packages.entrySet()) {
     System.err.println(e.getValue().intValue() + " " + e.getKey());
   }
  }

}
