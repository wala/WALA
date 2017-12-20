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
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.wala.fixedpoint.impl.GeneralStatement;
import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.fixpoint.AbstractStatement;
import com.ibm.wala.fixpoint.IFixedPointStatement;
import com.ibm.wala.fixpoint.IFixedPointSystem;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.fixpoint.UnaryStatement;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.DelegatingNumberedNodeManager;
import com.ibm.wala.util.graph.impl.SparseNumberedEdgeManager;
import com.ibm.wala.util.graph.traverse.Topological;
import com.ibm.wala.util.heapTrace.HeapTracer;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.IntSet;

/**
 * A dataflow graph implementation specialized for propagation-based pointer analysis
 */
public class PropagationGraph implements IFixedPointSystem<PointsToSetVariable> {

  private final static boolean DEBUG = false;

  private final static boolean VERBOSE = false;

  /**
   * Track nodes (PointsToSet Variables and AbstractEquations)
   */
  private final NumberedNodeManager<INodeWithNumber> nodeManager = new DelegatingNumberedNodeManager<>();

  /**
   * Track edges (equations) that are not represented implicitly
   */
  private final NumberedEdgeManager<INodeWithNumber> edgeManager = new SparseNumberedEdgeManager<>(nodeManager, 2,
      BasicNaturalRelation.SIMPLE);

  private final DelegateGraph delegateGraph = new DelegateGraph();

  private final HashSet<AbstractStatement> delegateStatements = HashSetFactory.make();

  /**
   * special representation for implicitly represented unary equations. This is a map from UnaryOperator ->
   * IBinaryNonNegativeIntRelation.
   * 
   * for UnaryOperator op, let R be implicitMap.get(op) then (i,j) \in R implies i op j is an equation in the graph
   * 
   */
  private final SmallMap<UnaryOperator<PointsToSetVariable>, IBinaryNaturalRelation> implicitUnaryMap = new SmallMap<>();

  /**
   * The inverse of relations in the implicit map
   * 
   * for UnaryOperator op, let R be invImplicitMap.get(op) then (i,j) \in R implies j op i is an equation in the graph
   */
  private final SmallMap<UnaryOperator<PointsToSetVariable>, IBinaryNaturalRelation> invImplicitUnaryMap = new SmallMap<>();

  /**
   * Number of implicit unary equations registered
   */
  private int implicitUnaryCount = 0;

  /**
   * @return a relation in map m corresponding to a key
   */
  private static IBinaryNaturalRelation findOrCreateRelation(Map<UnaryOperator<PointsToSetVariable>, IBinaryNaturalRelation> m,
      UnaryOperator<PointsToSetVariable> key) {
    IBinaryNaturalRelation result = m.get(key);
    if (result == null) {
      result = makeRelation(key);
      m.put(key, result);
    }
    return result;
  }

  /**
   * @return a Relation object to track implicit equations using the operator
   */
  private static IBinaryNaturalRelation makeRelation(AbstractOperator op) {
    byte[] implementation = null;
    if (op instanceof AssignOperator) {
      // lots of assignments.
      implementation = new byte[] { BasicNaturalRelation.SIMPLE_SPACE_STINGY, BasicNaturalRelation.SIMPLE_SPACE_STINGY };
    } else {
      // assume sparse assignments with any other operator.
      implementation = new byte[] { BasicNaturalRelation.SIMPLE_SPACE_STINGY };
    }
    return new BasicNaturalRelation(implementation, BasicNaturalRelation.SIMPLE);
  }

  /**
   * @author sfink
   * 
   *         A graph which tracks explicit equations.
   * 
   *         use this with care ...
   */
  private class DelegateGraph extends AbstractNumberedGraph<INodeWithNumber> {

    private int equationCount = 0;

    private int varCount = 0;

    @Override
    public void addNode(INodeWithNumber o) {
      Assertions.UNREACHABLE("Don't call me");
    }

    public void addEquation(AbstractStatement<PointsToSetVariable, ?> eq) {
      assert !containsStatement(eq);
      equationCount++;
      super.addNode(eq);
    }

    public void addVariable(PointsToSetVariable v) {
      if (!containsVariable(v)) {
        varCount++;
        super.addNode(v);
      }
    }

    /*
     * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
     */
    @Override
    protected NumberedNodeManager<INodeWithNumber> getNodeManager() {
      return nodeManager;
    }

    /*
     * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
     */
    @Override
    protected NumberedEdgeManager<INodeWithNumber> getEdgeManager() {
      return edgeManager;
    }

    protected int getEquationCount() {
      return equationCount;
    }

    protected int getVarCount() {
      return varCount;
    }

  }

  /**
   * @throws IllegalArgumentException if eq is null
   */
  public void addStatement(GeneralStatement<PointsToSetVariable> eq) {
    if (eq == null) {
      throw new IllegalArgumentException("eq is null");
    }
    PointsToSetVariable lhs = eq.getLHS();
    delegateGraph.addEquation(eq);
    delegateStatements.add(eq);
    if (lhs != null) {
      delegateGraph.addVariable(lhs);
      delegateGraph.addEdge(eq, lhs);
    }
    for (int i = 0; i < eq.getRHS().length; i++) {
      PointsToSetVariable v = eq.getRHS()[i];
      if (v != null) {
        delegateGraph.addVariable(v);
        delegateGraph.addEdge(v, eq);
      }
    }
  }

  public void addStatement(UnaryStatement<PointsToSetVariable> eq) throws IllegalArgumentException {
    if (eq == null) {
      throw new IllegalArgumentException("eq == null");
    }
    if (useImplicitRepresentation(eq)) {
      addImplicitStatement(eq);
    } else {
      PointsToSetVariable lhs = eq.getLHS();
      PointsToSetVariable rhs = eq.getRightHandSide();
      delegateGraph.addEquation(eq);
      delegateStatements.add(eq);
      if (lhs != null) {
        delegateGraph.addVariable(lhs);
        delegateGraph.addEdge(eq, lhs);
      }
      delegateGraph.addVariable(rhs);
      delegateGraph.addEdge(rhs, eq);
    }
  }

  /**
   * @return true iff this equation should be represented implicitly in this data structure
   */
  private static boolean useImplicitRepresentation(IFixedPointStatement s) {
    AbstractStatement eq = (AbstractStatement) s;
    AbstractOperator op = eq.getOperator();
    return (op instanceof AssignOperator || op instanceof PropagationCallGraphBuilder.FilterOperator);
  }

  public void removeVariable(PointsToSetVariable p) {
    assert getNumberOfStatementsThatDef(p) == 0;
    assert getNumberOfStatementsThatUse(p) == 0;
    delegateGraph.removeNode(p);
  }

  private void addImplicitStatement(UnaryStatement<PointsToSetVariable> eq) {
    if (DEBUG) {
      System.err.println(("addImplicitStatement " + eq));
    }
    delegateGraph.addVariable(eq.getLHS());
    delegateGraph.addVariable(eq.getRightHandSide());
    int lhs = eq.getLHS().getGraphNodeId();
    int rhs = eq.getRightHandSide().getGraphNodeId();
    if (DEBUG) {
      System.err.println(("lhs rhs " + lhs + " " + rhs));
    }
    IBinaryNaturalRelation R = findOrCreateRelation(implicitUnaryMap, eq.getOperator());
    boolean b = R.add(lhs, rhs);
    if (b) {
      implicitUnaryCount++;
      IBinaryNaturalRelation iR = findOrCreateRelation(invImplicitUnaryMap, eq.getOperator());
      iR.add(rhs, lhs);
    }
  }

  private void removeImplicitStatement(UnaryStatement<PointsToSetVariable> eq) {
    if (DEBUG) {
      System.err.println(("removeImplicitStatement " + eq));
    }
    int lhs = eq.getLHS().getGraphNodeId();
    int rhs = eq.getRightHandSide().getGraphNodeId();
    if (DEBUG) {
      System.err.println(("lhs rhs " + lhs + " " + rhs));
    }
    IBinaryNaturalRelation R = findOrCreateRelation(implicitUnaryMap, eq.getOperator());
    R.remove(lhs, rhs);
    IBinaryNaturalRelation iR = findOrCreateRelation(invImplicitUnaryMap, eq.getOperator());
    iR.remove(rhs, lhs);
    implicitUnaryCount--;
  }

  @Override
  public Iterator<AbstractStatement> getStatements() {
    Iterator<AbstractStatement> it = IteratorUtil.filter(delegateGraph.iterator(), AbstractStatement.class);
    return new CompoundIterator<>(it, new GlobalImplicitIterator());
  }

  /**
   * Iterator of implicit equations that use a particular variable.
   */
  private final class ImplicitUseIterator implements Iterator<AbstractStatement> {

    final PointsToSetVariable use;

    final IntIterator defs;

    final UnaryOperator<PointsToSetVariable> op;

    ImplicitUseIterator(UnaryOperator<PointsToSetVariable> op, PointsToSetVariable use, IntSet defs) {
      this.op = op;
      this.use = use;
      this.defs = defs.intIterator();
    }

    @Override
    public boolean hasNext() {
      return defs.hasNext();
    }

    @Override
    public AbstractStatement next() {
      int l = defs.next();
      PointsToSetVariable lhs = (PointsToSetVariable) delegateGraph.getNode(l);
      UnaryStatement temp = op.makeEquation(lhs, use);
      if (DEBUG) {
        System.err.print(("XX Return temp: " + temp));
        System.err.println(("lhs rhs " + l + " " + use.getGraphNodeId()));
      }
      return temp;
    }

    @Override
    public void remove() {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
    }
  }

  /**
   * Iterator of implicit equations that def a particular variable.
   */
  private final class ImplicitDefIterator implements Iterator<AbstractStatement> {

    final PointsToSetVariable def;

    final IntIterator uses;

    final UnaryOperator<PointsToSetVariable> op;

    ImplicitDefIterator(UnaryOperator<PointsToSetVariable> op, IntSet uses, PointsToSetVariable def) {
      this.op = op;
      this.def = def;
      this.uses = uses.intIterator();
    }

    @Override
    public boolean hasNext() {
      return uses.hasNext();
    }

    @Override
    public AbstractStatement next() {
      int r = uses.next();
      PointsToSetVariable rhs = (PointsToSetVariable) delegateGraph.getNode(r);
      UnaryStatement temp = op.makeEquation(def, rhs);
      if (DEBUG) {
        System.err.print(("YY Return temp: " + temp));
      }
      return temp;
    }

    @Override
    public void remove() {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
    }
  }

  /**
   * Iterator of all implicit equations
   */
  private class GlobalImplicitIterator implements Iterator<AbstractStatement> {

    private final Iterator<UnaryOperator<PointsToSetVariable>> outerKeyDelegate = implicitUnaryMap.keySet().iterator();

    private Iterator<IntPair> innerDelegate;

    private UnaryOperator<PointsToSetVariable> currentOperator;

    GlobalImplicitIterator() {
      advanceOuter();
    }

    /**
     * advance to the next operator
     */
    private void advanceOuter() {
      innerDelegate = null;
      while (outerKeyDelegate.hasNext()) {
        currentOperator = outerKeyDelegate.next();
        IBinaryNaturalRelation R = implicitUnaryMap.get(currentOperator);
        Iterator<IntPair> it = R.iterator();
        if (it.hasNext()) {
          innerDelegate = it;
          return;
        }
      }
    }

    @Override
    public boolean hasNext() {
      return innerDelegate != null;
    }

    @Override
    public AbstractStatement next() {
      IntPair p = innerDelegate.next();
      int lhs = p.getX();
      int rhs = p.getY();
      UnaryStatement result = currentOperator.makeEquation((PointsToSetVariable) delegateGraph.getNode(lhs),
          (PointsToSetVariable) delegateGraph.getNode(rhs));
      if (!innerDelegate.hasNext()) {
        advanceOuter();
      }
      return result;
    }

    @Override
    public void remove() {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();

    }
  }

  @Override
  public void removeStatement(IFixedPointStatement<PointsToSetVariable> eq) throws IllegalArgumentException {
    if (eq == null) {
      throw new IllegalArgumentException("eq == null");
    }
    if (useImplicitRepresentation(eq)) {
      removeImplicitStatement((UnaryStatement<PointsToSetVariable>) eq);
    } else {
      delegateStatements.remove(eq);
      delegateGraph.removeNodeAndEdges(eq);
    }
  }

  @Override
  public void reorder() {
    VariableGraphView graph = new VariableGraphView();

    Iterator<PointsToSetVariable> order = Topological.makeTopologicalIter(graph).iterator();

    int number = 0;
    while (order.hasNext()) {
      Object elt = order.next();
      if (elt instanceof IVariable) {
        IVariable v = (IVariable) elt;
        v.setOrderNumber(number++);
      }
    }
  }

  /**
   * A graph of just the variables in the system. v1 -&gt; v2 iff there exists equation e s.t. e uses v1 and e defs v2.
   * 
   * Note that this graph trickily and fragilely reuses the nodeManager from the delegateGraph, above. This will work ok as long as
   * every variable is inserted in the delegateGraph.
   */
  private class VariableGraphView extends AbstractNumberedGraph<PointsToSetVariable> {

    /*
     * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
     */
    @Override
    public void removeNodeAndEdges(PointsToSetVariable N) {
      Assertions.UNREACHABLE();
    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
     */
    @Override
    public Iterator<PointsToSetVariable> iterator() {
      return getVariables();
    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
     */
    @Override
    public int getNumberOfNodes() {
      return delegateGraph.getVarCount();
    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
     */
    @Override
    public void addNode(PointsToSetVariable n) {
      Assertions.UNREACHABLE();

    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
     */
    @Override
    public void removeNode(PointsToSetVariable n) {
      Assertions.UNREACHABLE();

    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
     */
    @Override
    public boolean containsNode(PointsToSetVariable N) {
      return delegateGraph.containsNode(N);
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
     */
    @Override
    public Iterator<PointsToSetVariable> getPredNodes(PointsToSetVariable v) {
      final Iterator<AbstractStatement> eqs = getStatementsThatDef(v);
      return new Iterator<PointsToSetVariable>() {
        Iterator<INodeWithNumber> inner;

        @Override
        public boolean hasNext() {
          return eqs.hasNext() || (inner != null);
        }

        @Override
        public PointsToSetVariable next() {
          if (inner != null) {
            PointsToSetVariable result = (PointsToSetVariable)inner.next();
            if (!inner.hasNext()) {
              inner = null;
            }
            return result;
          } else {
            AbstractStatement eq = eqs.next();
            if (useImplicitRepresentation(eq)) {
              return (PointsToSetVariable) ((UnaryStatement) eq).getRightHandSide();
            } else {
              inner = delegateGraph.getPredNodes(eq);
              return next();
            }
          }
        }

        @Override
        public void remove() {
          // TODO Auto-generated method stub
          Assertions.UNREACHABLE();
        }
      };
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
     */
    @SuppressWarnings("unused")
    public int getPredNodeCount(INodeWithNumber N) {
      PointsToSetVariable v = (PointsToSetVariable) N;
      int result = 0;
      for (AbstractStatement eq : Iterator2Iterable.make(getStatementsThatDef(v))) {
        if (useImplicitRepresentation(eq)) {
          result++;
        } else {
          result += delegateGraph.getPredNodeCount(N);
        }
      }
      return result;
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
     */
    @Override
    public Iterator<PointsToSetVariable> getSuccNodes(PointsToSetVariable v) {
      final Iterator<AbstractStatement> eqs = getStatementsThatUse(v);
      return new Iterator<PointsToSetVariable>() {
        PointsToSetVariable nextResult;
        {
          advance();
        }

        @Override
        public boolean hasNext() {
          return nextResult != null;
        }

        @Override
        public PointsToSetVariable next() {
          PointsToSetVariable result = nextResult;
          advance();
          return result;
        }

        private void advance() {
          nextResult = null;
          while (eqs.hasNext() && nextResult == null) {
            AbstractStatement eq = eqs.next();
            nextResult = (PointsToSetVariable) eq.getLHS();
          }
        }

        @Override
        public void remove() {
          // TODO Auto-generated method stub
          Assertions.UNREACHABLE();
        }
      };
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
     */
    @Override
    public int getSuccNodeCount(PointsToSetVariable v) {
      int result = 0;
      for (AbstractStatement eq : Iterator2Iterable.make(getStatementsThatUse(v))) {
        IVariable lhs = eq.getLHS();
        if (lhs != null) {
          result++;
        }
      }
      return result;
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object, java.lang.Object)
     */
    @Override
    public void addEdge(PointsToSetVariable src, PointsToSetVariable dst) {
      Assertions.UNREACHABLE();
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(java.lang.Object)
     */
    @Override
    public void removeAllIncidentEdges(PointsToSetVariable node) {
      Assertions.UNREACHABLE();
    }

    /*
     * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected NumberedNodeManager getNodeManager() {
      return nodeManager;
    }

    /*
     * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected NumberedEdgeManager getEdgeManager() {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
      return null;
    }

  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<AbstractStatement> getStatementsThatUse(PointsToSetVariable v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    int number = v.getGraphNodeId();
    if (number == -1) {
      return EmptyIterator.instance();
    }
    Iterator<INodeWithNumber> result = delegateGraph.getSuccNodes(v);
    for (int i = 0; i < invImplicitUnaryMap.size(); i++) {
      UnaryOperator op = invImplicitUnaryMap.getKey(i);
      IBinaryNaturalRelation R = (IBinaryNaturalRelation) invImplicitUnaryMap.getValue(i);
      IntSet s = R.getRelated(number);
      if (s != null) {
        result = new CompoundIterator<>(new ImplicitUseIterator(op, v, s), result);
      }
    }
    List<AbstractStatement> list = new ArrayList<>();
    while (result.hasNext()) {
      list.add((AbstractStatement) result.next());
    }
    return list.iterator();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<AbstractStatement> getStatementsThatDef(PointsToSetVariable v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    int number = v.getGraphNodeId();
    if (number == -1) {
      return EmptyIterator.instance();
    }
    Iterator<INodeWithNumber> result = delegateGraph.getPredNodes(v);
    for (int i = 0; i < implicitUnaryMap.size(); i++) {
      UnaryOperator op = implicitUnaryMap.getKey(i);
      IBinaryNaturalRelation R = (IBinaryNaturalRelation) implicitUnaryMap.getValue(i);
      IntSet s = R.getRelated(number);
      if (s != null) {
        result = new CompoundIterator<>(new ImplicitDefIterator(op, s, v), result);
      }
    }

    List<AbstractStatement> list = new ArrayList<>();
    while (result.hasNext()) {
      list.add((AbstractStatement) result.next());
    }
    return list.iterator();
  }

  /**
   * Note that this implementation consults the implicit relation for each and every operator cached. This will be inefficient if
   * there are many implicit operators.
   * 
   * @throws IllegalArgumentException if v is null
   * 
   */
  @Override
  public int getNumberOfStatementsThatUse(PointsToSetVariable v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    int number = v.getGraphNodeId();
    if (number == -1) {
      return 0;
    }
    int result = delegateGraph.getSuccNodeCount(v);
    for (UnaryOperator<PointsToSetVariable> op : invImplicitUnaryMap.keySet()) {
      IBinaryNaturalRelation R = invImplicitUnaryMap.get(op);
      IntSet s = R.getRelated(number);
      if (s != null) {
        result += s.size();
      }
    }
    return result;
  }

  @Override
  public int getNumberOfStatementsThatDef(PointsToSetVariable v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    int number = v.getGraphNodeId();
    if (number == -1) {
      return 0;
    }
    int result = delegateGraph.getPredNodeCount(v);
    for (UnaryOperator<PointsToSetVariable> op : implicitUnaryMap.keySet()) {
      IBinaryNaturalRelation R = implicitUnaryMap.get(op);
      IntSet s = R.getRelated(number);
      if (s != null) {
        result += s.size();
      }
    }
    return result;
  }

  @Override
  public Iterator<PointsToSetVariable> getVariables() {
    return IteratorUtil.filter(delegateGraph.iterator(), PointsToSetVariable.class);
  }

  /*
   * @see com.ibm.wala.util.debug.VerboseAction#performVerboseAction()
   */
  public void performVerboseAction() {
    if (VERBOSE) {
      System.err.println(("stats for " + getClass()));
      System.err.println(("number of variables: " + delegateGraph.getVarCount()));
      System.err.println(("implicit equations: " + (implicitUnaryCount)));
      System.err.println(("explicit equations: " + delegateGraph.getEquationCount()));
      System.err.println("implicit map:");
      int count = 0;
      int totalBytes = 0;
      for (Map.Entry<UnaryOperator<PointsToSetVariable>, IBinaryNaturalRelation> entry : implicitUnaryMap.entrySet()) {
		  count++;
        Map.Entry<?, IBinaryNaturalRelation> e = entry;
        IBinaryNaturalRelation R = e.getValue();
        System.err.println(("entry " + count));
        R.performVerboseAction();
        HeapTracer.Result result = HeapTracer.traceHeap(Collections.singleton(R), false);
        totalBytes += result.getTotalSize();
      }
      System.err.println(("bytes in implicit map: " + totalBytes));
    }
  }

  @Override
  public boolean containsStatement(IFixedPointStatement<PointsToSetVariable> eq) throws IllegalArgumentException {
    if (eq == null) {
      throw new IllegalArgumentException("eq == null");
    }
    if (useImplicitRepresentation(eq)) {
      UnaryStatement<PointsToSetVariable> ueq = (UnaryStatement<PointsToSetVariable>) eq;
      return containsImplicitStatement(ueq);
    } else {
      return delegateStatements.contains(eq);
    }
  }

  /**
   * @return true iff the graph already contains this equation
   */
  private boolean containsImplicitStatement(UnaryStatement<PointsToSetVariable> eq) {
    if (!containsVariable(eq.getLHS())) {
      return false;
    }
    if (!containsVariable(eq.getRightHandSide())) {
      return false;
    }
    int lhs = eq.getLHS().getGraphNodeId();
    int rhs = eq.getRightHandSide().getGraphNodeId();
    UnaryOperator op = eq.getOperator();
    IBinaryNaturalRelation R = implicitUnaryMap.get(op);
    if (R != null) {
      return R.contains(lhs, rhs);
    } else {
      return false;
    }
  }

  @Override
  public boolean containsVariable(PointsToSetVariable v) {
    return delegateGraph.containsNode(v);
  }

  @Override
  public void addStatement(IFixedPointStatement<PointsToSetVariable> statement) throws IllegalArgumentException, UnimplementedError {
    if (statement == null) {
      throw new IllegalArgumentException("statement == null");
    }
    if (statement instanceof UnaryStatement) {
      addStatement((UnaryStatement<PointsToSetVariable>) statement);
    } else if (statement instanceof GeneralStatement) {
      addStatement((GeneralStatement<PointsToSetVariable>) statement);
    } else {
      Assertions.UNREACHABLE("unexpected: " + statement.getClass());
    }
  }

  /**
   * A graph of just the variables in the system. v1 -&gt; v2 iff there exists an assignment equation e s.t. e uses v1 and e defs v2.
   * 
   */
  public NumberedGraph<PointsToSetVariable> getAssignmentGraph() {
    return new FilteredConstraintGraphView() {

      @Override
      boolean isInteresting(AbstractStatement eq) {
        return eq instanceof AssignEquation;
      }
    };
  }

  /**
   * A graph of just the variables in the system. v1 -&gt; v2 iff there exists an Assingnment or Filter equation e s.t. e uses v1 and e
   * defs v2.
   * 
   */
  public Graph<PointsToSetVariable> getFilterAssignmentGraph() {
    return new FilteredConstraintGraphView() {

      @Override
      boolean isInteresting(AbstractStatement eq) {
        return eq instanceof AssignEquation || eq.getOperator() instanceof PropagationCallGraphBuilder.FilterOperator;
      }
    };
  }

  /**
   * NOTE: do not use this method unless you really know what you are doing. Functionality is fragile and may not work in the
   * future.
   */
  public Graph<PointsToSetVariable> getFlowGraphIncludingImplicitConstraints() {
    return new VariableGraphView();
  }

  /**
   * A graph of just the variables in the system. v1 -&gt; v2 that are related by def-use with "interesting" operators
   * 
   */
  private abstract class FilteredConstraintGraphView extends AbstractNumberedGraph<PointsToSetVariable> {

    abstract boolean isInteresting(AbstractStatement eq);

    /*
     * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
     */
    @Override
    public void removeNodeAndEdges(PointsToSetVariable N) {
      Assertions.UNREACHABLE();
    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
     */
    @Override
    public Iterator<PointsToSetVariable> iterator() {
      return getVariables();
    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
     */
    @Override
    public int getNumberOfNodes() {
      return delegateGraph.getVarCount();
    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
     */
    @Override
    public void addNode(PointsToSetVariable n) {
      Assertions.UNREACHABLE();

    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
     */
    @Override
    public void removeNode(PointsToSetVariable n) {
      Assertions.UNREACHABLE();

    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
     */
    @Override
    public boolean containsNode(PointsToSetVariable N) {
      Assertions.UNREACHABLE();
      return false;
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
     */
    @Override
    public Iterator<PointsToSetVariable> getPredNodes(PointsToSetVariable v) {
      final Iterator<AbstractStatement> eqs = getStatementsThatDef(v);
      return new Iterator<PointsToSetVariable>() {
        PointsToSetVariable nextResult;
        {
          advance();
        }

        @Override
        public boolean hasNext() {
          return nextResult != null;
        }

        @Override
        public PointsToSetVariable next() {
          PointsToSetVariable result = nextResult;
          advance();
          return result;
        }

        private void advance() {
          nextResult = null;
          while (eqs.hasNext() && nextResult == null) {
            AbstractStatement eq = eqs.next();
            if (isInteresting(eq)) {
              nextResult = (PointsToSetVariable) ((UnaryStatement) eq).getRightHandSide();
            }
          }
        }

        @Override
        public void remove() {
          Assertions.UNREACHABLE();
        }
      };
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
     */
    @Override
    public int getPredNodeCount(PointsToSetVariable v) {
      int result = 0;
      for (AbstractStatement eq : Iterator2Iterable.make(getStatementsThatDef(v))) {
        if (isInteresting(eq)) {
          result++;
        }
      }
      return result;
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
     */
    @Override
    public Iterator<PointsToSetVariable> getSuccNodes(PointsToSetVariable v) {
      final Iterator<AbstractStatement> eqs = getStatementsThatUse(v);
      return new Iterator<PointsToSetVariable>() {
        PointsToSetVariable nextResult;
        {
          advance();
        }

        @Override
        public boolean hasNext() {
          return nextResult != null;
        }

        @Override
        public PointsToSetVariable next() {
          PointsToSetVariable result = nextResult;
          advance();
          return result;
        }

        private void advance() {
          nextResult = null;
          while (eqs.hasNext() && nextResult == null) {
            AbstractStatement eq = eqs.next();
            if (isInteresting(eq)) {
              nextResult = (PointsToSetVariable) ((UnaryStatement) eq).getLHS();
            }
          }
        }

        @Override
        public void remove() {
          // TODO Auto-generated method stub
          Assertions.UNREACHABLE();
        }
      };
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
     */
    @Override
    public int getSuccNodeCount(PointsToSetVariable v) {
      int result = 0;
      for (AbstractStatement eq : Iterator2Iterable.make(getStatementsThatUse(v))) {
        if (isInteresting(eq)) {
          result++;
        }
      }
      return result;
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object, java.lang.Object)
     */
    @Override
    public void addEdge(PointsToSetVariable src, PointsToSetVariable dst) {
      Assertions.UNREACHABLE();
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(java.lang.Object)
     */
    @Override
    public void removeAllIncidentEdges(PointsToSetVariable node) {
      Assertions.UNREACHABLE();
    }

    /*
     * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected NumberedNodeManager getNodeManager() {
      return nodeManager;
    }

    /*
     * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
     */
    @Override
    protected NumberedEdgeManager<PointsToSetVariable> getEdgeManager() {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public String spaceReport() {
    StringBuffer result = new StringBuffer("PropagationGraph\n");
    result.append("ImplicitEdges:" + countImplicitEdges() + "\n");
    // for (Iterator it = implicitUnaryMap.values().iterator(); it.hasNext(); )
    // {
    // result.append(it.next() + "\n");
    // }
    return result.toString();
  }

  private int countImplicitEdges() {
    return IteratorUtil.count(new GlobalImplicitIterator());
  }

}
