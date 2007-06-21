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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.fixedpoint.impl.AbstractFixedPointSolver;
import com.ibm.wala.fixedpoint.impl.AbstractStatement;
import com.ibm.wala.fixedpoint.impl.UnaryStatement;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.intset.BimodalMutableIntSet;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.perf.EngineTimings;

/**
 * 
 * pre-transitive solver incorporating concepts from Heintze and Tardieu, PLDI
 * 2001
 * 
 * In this implementation, there are two types of points-to-sets, "transitive
 * roots", and "not-roots".
 * 
 * The points-to-sets for transitive roots are "primordial", they do not result
 * from assignments and transitive closure. For example, if x = new A, the
 * points-to-set for x will be a transitive root holding the instance key for
 * the allocation site.
 * 
 * Under construction.
 * 
 * TODO: fix points-to solution to use demand-driven
 * 
 * @author sfink
 */
public class PreTransitiveSolver extends AbstractPointsToSolver {

  public PreTransitiveSolver(PropagationSystem system, PropagationCallGraphBuilder builder) {
    super(system, builder);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.IPointsToSolver#solve()
   */
  @Override
  public void solve() {

    EngineTimings.startVirtual("PreTransitiveSolver.solve()");

    getBuilder().addConstraintsFromNewNodes();

    boolean changed = false;
    do {
      changed = false;
      BitVectorIntSet visited = new BitVectorIntSet();

      // clear all cached closure (reachability) info
      getSystem().revertToPreTransitive();

      Collection<IVariable> complexUses = findComplexUses();
      NumberedGraph<IVariable> ag = getSystem().getAssignmentGraph();

      for (Iterator<IVariable> it = complexUses.iterator(); it.hasNext();) {
        PointsToSetVariable p = (PointsToSetVariable) it.next();
        getLvals(ag, p.getPointerKey(), new Path(), visited);
        for (Iterator it2 = getSystem().getStatementsThatUse(p); it2.hasNext();) {
          AbstractStatement s = (AbstractStatement) it2.next();
          if (isComplexStatement(s)) {
            byte code = s.evaluate();
            changed |= AbstractFixedPointSolver.isChanged(code);
            changed |= AbstractFixedPointSolver.isSideEffect(code);
          }
        }
      }
      // Add constraints until from new nodes and reflection
      changed |= getBuilder().addConstraintsFromNewNodes();
      if (!changed) {
        // avoid this until last minute.  it's expensive.
        if (getReflectionHandler() != null) {
          changed |= getReflectionHandler().updateForReflection();
        }
      }
    } while (changed);
    
    EngineTimings.finishVirtual("PreTransitiveSolver.solve()");
  }

  private boolean isComplexStatement(AbstractStatement s) {
    IPointerOperator op = (IPointerOperator) s.getOperator();
    return (op.isComplex() || op instanceof PropagationCallGraphBuilder.FilterOperator);
  }

  /**
   * perform graph reachability to find all pointer keys that may flow into p.
   * Perform cycle elimination as a side effect.
   * 
   * This is named getLvals matching the Heintze Tardieu PLDI 01 paper, but I
   * don't find the name intuitive.
   * 
   * TODO: recode so it's not recursive?
   * 
   * @param ag
   *          graph view of pointer assignments
   * @param p
   * @param path
   *          numbers of points-to-sets on the current path.
   * @param visited
   *          numbers of points-to-sets we have already visited in this
   *          iteration, and thus have already cached the reachability.
   * @return the set of instance key numbers which flow to p through assignments
   */
  private IntSet getLvals(Graph<IVariable> ag, PointerKey p, Path path, MutableIntSet visited) {

    if (path.contains(getSystem().getNumber(p))) {
      if (path.size() > 1) {
        unifyCycle(path, getSystem().getNumber(p));
      }
      PointsToSetVariable v = getSystem().findOrCreatePointsToSet(p);
      return v.getValue();
    } else {
      path.add(getSystem().getNumber(p));
      PointsToSetVariable v = getSystem().findOrCreatePointsToSet(p);
      if (visited.contains(getSystem().getNumber(p))) {
        // we've already visited v and cached
        // the reachability.
        path.remove(getSystem().getNumber(p));
        return v.getValue();
      } else {
        visited.add(getSystem().getNumber(p));
        MutableIntSet result = IntSetUtil.getDefaultIntSetFactory().make();
        if (v.getValue() != null) {
          result.addAll(v.getValue());
        }

        // cache the predecessors before unification might screw things up.
        Iterator2Collection<? extends IVariable> origPred = Iterator2Collection.toCollection(ag.getPredNodes(v));
        for (Iterator it = origPred.iterator(); it.hasNext();) {
          PointsToSetVariable n = (PointsToSetVariable) it.next();

          PointerKey pk = n.getPointerKey();
          if (getSystem().isUnified(n.getPointerKey())) {
            n = getSystem().findOrCreatePointsToSet(pk);
            pk = n.getPointerKey();
          }
          IntSet lvals = getLvals(ag, pk, path, visited);
          if (lvals != null) {
            result.addAll(lvals);
          }
        }
        path.remove(getSystem().getNumber(p));
        v.addAll(result);
        return result;
      }
    }
  }

  private void unifyCycle(Path path, int number) {
    MutableSparseIntSet cycle = new MutableSparseIntSet();
    cycle.add(number);
    int index = path.size() - 1;
    while (true) {
      Integer i = path.get(index--);
      if (i.intValue() == number) {
        break;
      }
      cycle.add(i.intValue());
    }
    if (cycle.size() > 1) {
      getSystem().unify(cycle);
    }
  }

  /**
   * TODO: This is horribly slow. Optimize it by pushing the functionality into
   * PropagationSystem and PropagationGraph.
   * 
   * @return set of PointsToSetVariable that are used by complex constraints
   */
  public Collection<IVariable> findComplexUses() {
    HashSet<IVariable> result = new HashSet<IVariable>();
    for (Iterator it = getSystem().getStatements(); it.hasNext();) {
      AbstractStatement s = (AbstractStatement) it.next();
      if (isComplexStatement(s)) {
        if (s instanceof UnaryStatement) {
          UnaryStatement u = (UnaryStatement) s;
          result.add(u.getRightHandSide());
        } else {
          IVariable[] rhs = s.getRHS();
          for (int i = 0; i < rhs.length; i++) {
            result.add(rhs[i]);
          }
        }
      }
    }
    return result;
  }

  private static class Path {
    final MutableIntSet contents = new BimodalMutableIntSet();

    final ArrayList<Integer> sequence = new ArrayList<Integer>();

    public boolean contains(int number) {
      return contents.contains(number);
    }

    public Integer get(int i) {
      return sequence.get(i);
    }

    public void remove(int number) {
      if (Assertions.verifyAssertions) {
        Assertions._assert(sequence.get(sequence.size() - 1).intValue() == number);
      }
      sequence.remove(sequence.size() - 1);
      contents.remove(number);
    }

    public void add(int number) {
      sequence.add(new Integer(number));
      contents.add(number);
    }

    public int size() {
      return contents.size();
    }
  }
}
