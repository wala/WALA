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
package com.ibm.wala.fixedpoint.impl;

import java.util.Iterator;
import java.util.LinkedList;

import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.fixpoint.AbstractStatement;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.IFixedPointSolver;
import com.ibm.wala.fixpoint.IFixedPointStatement;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.fixpoint.UnaryStatement;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.VerboseAction;
import com.ibm.wala.util.graph.INodeWithNumber;

/**
 * Represents a set of {@link IFixedPointStatement}s to be solved by a {@link IFixedPointSolver}
 * 
 * <p>
 * Implementation Note:
 * 
 * The set of steps and variables is internally represented as a graph. Each step and each variable is a node in the graph. If a
 * step produces a variable that is used by another step, the graph has a directed edge from the producer to the consumer.
 * Fixed-point iteration proceeds in a topological order according to these edges.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractFixedPointSolver<T extends IVariable<T>> implements IFixedPointSolver<T>, FixedPointConstants,
    VerboseAction {

  static final boolean DEBUG = false;

  static public final boolean verbose = "true".equals(System.getProperty("com.ibm.wala.fixedpoint.impl.verbose"));

  static public final int DEFAULT_VERBOSE_INTERVAL = 100000;

  static final boolean MORE_VERBOSE = true;

  static public final int DEFAULT_PERIODIC_MAINTENANCE_INTERVAL = 100000;

  /**
   * A tuning parameter; how may new IStatementDefinitionss must be added before doing a new topological sort? TODO: Tune this
   * empirically.
   */
  private int minSizeForTopSort = 0;

  /**
   * A tuning parameter; by what percentage must the number of equations grow before we perform a topological sort?
   */
  private double topologicalGrowthFactor = 0.1;

  /**
   * A tuning parameter: how many evaluations are allowed to take place between topological re-orderings. The idea is that many
   * evaluations may be a sign of a bad ordering, even when few new equations are being added.
   * 
   * A number less than zero mean infinite.
   */
  private int maxEvalBetweenTopo = 500000;

  private int evaluationsAtLastOrdering = 0;

  /**
   * How many equations have been added since the last topological sort?
   */
  int topologicalCounter = 0;

  /**
   * The next order number to assign to a new equation
   */
  int nextOrderNumber = 1;

  /**
   * During verbose evaluation, holds the number of dataflow equations evaluated
   */
  private int nEvaluated = 0;

  /**
   * During verbose evaluation, holds the number of dataflow equations created
   */
  private int nCreated = 0;

  /**
   * worklist for the iterative solver
   */
  protected Worklist workList = new Worklist();

  /**
   * A boolean which is initially true, but set to false after the first call to solve();
   */
  private boolean firstSolve = true;

  protected abstract T[] makeStmtRHS(int size);
  
  /**
   * Some setup which occurs only before the first solve
   */
  public void initForFirstSolve() {
    orderStatements();
    initializeVariables();
    initializeWorkList();
    firstSolve = false;
  }

  /**
   * @return true iff work list is empty
   */
  public boolean emptyWorkList() {
    return workList.isEmpty();
  }

  /**
   * Solve the set of dataflow graph.
   * <p>
   * PRECONDITION: graph is set up
   * 
   * @return true iff the evaluation of some equation caused a change in the value of some variable.
   */
  @Override
  @SuppressWarnings("unchecked")
  public boolean solve(IProgressMonitor monitor) throws CancelException {

    boolean globalChange = false;

    if (firstSolve) {
      initForFirstSolve();
    }

    while (!workList.isEmpty()) {
      MonitorUtil.throwExceptionIfCanceled(monitor);
      orderStatements();

      // duplicate insertion detection
      AbstractStatement s = workList.takeStatement();

      if (DEBUG) {
        System.err.println(("Before evaluation " + s));
      }
      byte code = s.evaluate();
      nEvaluated++;
      if (verbose) {
        if (nEvaluated % getVerboseInterval() == 0) {
          performVerboseAction();
        }
        if (nEvaluated % getPeriodicMaintainInterval() == 0) {
          periodicMaintenance();
        }

      }
      if (DEBUG) {
        System.err.println(("After evaluation  " + s + " " + isChanged(code)));
      }
      if (isChanged(code)) {
        globalChange = true;
        updateWorkList(s);
      }
      if (isFixed(code)) {
        removeStatement(s);
      }
    }
    return globalChange;
  }

  @Override
  public void performVerboseAction() {
    System.err.println("Evaluated " + nEvaluated);
    System.err.println("Created   " + nCreated);
    System.err.println("Worklist  " + workList.size());
    if (MORE_VERBOSE) {
      if (!workList.isEmpty()) {
        AbstractStatement s = workList.takeStatement();
        System.err.println("Peek      " + lineBreak(s.toString(), 132));
        if (s instanceof VerboseAction) {
          ((VerboseAction) s).performVerboseAction();
        }
        workList.insertStatement(s);
      }
    }
  }

  public static String lineBreak(String string, int wrap) {
    if (string == null) {
      throw new IllegalArgumentException("string is null");
    }
    if (string.length() > wrap) {
      StringBuffer result = new StringBuffer();
      int start = 0;
      while (start < string.length()) {
        int end = Math.min(start + wrap, string.length());
        result.append(string.substring(start, end));
        result.append("\n  ");
        start = end;
      }
      return result.toString();
    } else {
      return string;
    }
  }

  public void removeStatement(AbstractStatement<T, ?> s) {
    getFixedPointSystem().removeStatement(s);
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("Fixed Point System:\n");
    for (INodeWithNumber nwn : Iterator2Iterable.make(getStatements())) {
      result.append(nwn).append("\n");
    }
    return result.toString();
  }

  public Iterator<? extends INodeWithNumber> getStatements() {
    return getFixedPointSystem().getStatements();
  }

  /**
   * Add a step to the work list.
   * 
   * @param s the step to add
   */
  public void addToWorkList(AbstractStatement s) {
    workList.insertStatement(s);
  }

  /**
   * Add all to the work list.
   */
  public void addAllStatementsToWorkList() {
    for (INodeWithNumber nwn : Iterator2Iterable.make(getStatements())) {
      AbstractStatement eq = (AbstractStatement) nwn;
      addToWorkList(eq);
    }
  }

  /**
   * Call this method when the contents of a variable changes. This routine adds all graph using this variable to the set of new
   * graph.
   * 
   * @param v the variable that has changed
   */
  public void changedVariable(T v) {
    for (INodeWithNumber nwn : Iterator2Iterable.make(getFixedPointSystem().getStatementsThatUse(v))) {
      AbstractStatement s = (AbstractStatement) nwn;
      addToWorkList(s);
    }
  }

  /**
   * Add a step with zero operands on the right-hand side.
   * 
   * TODO: this is a little odd, in that this equation will never fire unless explicitly added to a work list. I think in most cases
   * we shouldn't be creating this nullary form.
   * 
   * @param lhs the variable set by this equation
   * @param operator the step operator
   * @throws IllegalArgumentException if lhs is null
   */
  public boolean newStatement(final T lhs, final NullaryOperator<T> operator, final boolean toWorkList, final boolean eager) {
    if (lhs == null) {
      throw new IllegalArgumentException("lhs is null");
    }
    // add to the list of graph
    lhs.setOrderNumber(nextOrderNumber++);
    final NullaryStatement<T> s = new BasicNullaryStatement<>(lhs, operator);
    if (getFixedPointSystem().containsStatement(s)) {
      return false;
    }
    nCreated++;
    getFixedPointSystem().addStatement(s);
    incorporateNewStatement(toWorkList, eager, s);
    topologicalCounter++;
    return true;
  }

  @SuppressWarnings("unchecked")
  private void incorporateNewStatement(boolean toWorkList, boolean eager, AbstractStatement s) {
    if (eager) {
      byte code = s.evaluate();
      if (verbose) {
        nEvaluated++;
        if (nEvaluated % getVerboseInterval() == 0) {
          performVerboseAction();
        }
        if (nEvaluated % getPeriodicMaintainInterval() == 0) {
          periodicMaintenance();
        }
      }
      if (isChanged(code)) {
        updateWorkList(s);
      }
      if (isFixed(code)) {
        removeStatement(s);
      }
    } else if (toWorkList) {
      addToWorkList(s);
    }
  }

  /**
   * Add a step with one operand on the right-hand side.
   * 
   * @param lhs the lattice variable set by this equation
   * @param operator the step's operator
   * @param rhs first operand on the rhs
   * @return true iff the system changes
   * @throws IllegalArgumentException if operator is null
   */
  public boolean newStatement(T lhs, UnaryOperator<T> operator, T rhs, boolean toWorkList, boolean eager) {
    if (operator == null) {
      throw new IllegalArgumentException("operator is null");
    }
    // add to the list of graph
    UnaryStatement<T> s = operator.makeEquation(lhs, rhs);
    if (getFixedPointSystem().containsStatement(s)) {
      return false;
    }
    if (lhs != null) {
      lhs.setOrderNumber(nextOrderNumber++);
    }
    nCreated++;
    getFixedPointSystem().addStatement(s);
    incorporateNewStatement(toWorkList, eager, s);
    topologicalCounter++;
    return true;
  }

  protected class Statement extends GeneralStatement<T> {

    public Statement(T lhs, AbstractOperator<T> operator, T op1, T op2, T op3) {
      super(lhs, operator, op1, op2, op3);
    }

    public Statement(T lhs, AbstractOperator<T> operator, T op1, T op2) {
      super(lhs, operator, op1, op2);
    }

    public Statement(T lhs, AbstractOperator<T> operator, T[] rhs) {
      super(lhs, operator, rhs);
    }

    public Statement(T lhs, AbstractOperator<T> operator) {
      super(lhs, operator);
    }

    @Override
    protected T[] makeRHS(int size) {
      return makeStmtRHS(size);
    }
    
  }
  
  /**
   * Add an equation with two operands on the right-hand side.
   * 
   * @param lhs the lattice variable set by this equation
   * @param operator the equation operator
   * @param op1 first operand on the rhs
   * @param op2 second operand on the rhs
   */
  public boolean newStatement(T lhs, AbstractOperator<T> operator, T op1, T op2, boolean toWorkList, boolean eager) {
    // add to the list of graph

    GeneralStatement<T> s = new Statement(lhs, operator, op1, op2);
    if (getFixedPointSystem().containsStatement(s)) {
      return false;
    }
    if (lhs != null) {
      lhs.setOrderNumber(nextOrderNumber++);
    }
    nCreated++;
    getFixedPointSystem().addStatement(s);
    incorporateNewStatement(toWorkList, eager, s);
    topologicalCounter++;
    return true;
  }

  /**
   * Add a step with three operands on the right-hand side.
   * 
   * @param lhs the lattice variable set by this equation
   * @param operator the equation operator
   * @param op1 first operand on the rhs
   * @param op2 second operand on the rhs
   * @param op3 third operand on the rhs
   * @throws IllegalArgumentException if lhs is null
   */
  public boolean newStatement(T lhs, AbstractOperator<T> operator, T op1, T op2, T op3, boolean toWorkList, boolean eager) {
    if (lhs == null) {
      throw new IllegalArgumentException("lhs is null");
    }
    // add to the list of graph
    lhs.setOrderNumber(nextOrderNumber++);
    GeneralStatement<T> s = new Statement(lhs, operator, op1, op2, op3);
    if (getFixedPointSystem().containsStatement(s)) {
      nextOrderNumber--;
      return false;
    }
    nCreated++;
    getFixedPointSystem().addStatement(s);

    incorporateNewStatement(toWorkList, eager, s);
    topologicalCounter++;
    return true;
  }

  /**
   * Add a step to the system with an arbitrary number of operands on the right-hand side.
   * 
   * @param lhs lattice variable set by this equation
   * @param operator the operator
   * @param rhs the operands on the rhs
   */
  public boolean newStatement(T lhs, AbstractOperator<T> operator, T[] rhs, boolean toWorkList, boolean eager) {
    // add to the list of graph
    if (lhs != null)
      lhs.setOrderNumber(nextOrderNumber++);
    GeneralStatement<T> s = new Statement(lhs, operator, rhs);
    if (getFixedPointSystem().containsStatement(s)) {
      nextOrderNumber--;
      return false;
    }
    nCreated++;
    getFixedPointSystem().addStatement(s);
    incorporateNewStatement(toWorkList, eager, s);
    topologicalCounter++;
    return true;
  }

  /**
   * Initialize all lattice vars in the system.
   */
  abstract protected void initializeVariables();

  /**
   * Initialize the work list for iteration.j
   */
  abstract protected void initializeWorkList();

  /**
   * Update the worklist, assuming that a particular equation has been re-evaluated
   * 
   * @param s the equation that has been re-evaluated.
   */
  private void updateWorkList(AbstractStatement<T, ?> s) {
    // find each equation which uses this lattice cell, and
    // add it to the work list
    T v = s.getLHS();
    if (v == null) {
      return;
    }
    changedVariable(v);
  }

  /**
   * Number the graph in topological order.
   */
  private void orderStatementsInternal() {
    if (verbose) {
      if (nEvaluated > 0) {
        System.err.println("Reorder " + nEvaluated + " " + nCreated);
      }
    }
    reorder();
    if (verbose) {
      if (nEvaluated > 0) {
        System.err.println("Reorder finished " + nEvaluated + " " + nCreated);
      }
    }
    topologicalCounter = 0;
    evaluationsAtLastOrdering = nEvaluated;
  }

  /**
   * 
   */
  public void orderStatements() {

    if (nextOrderNumber > minSizeForTopSort) {
      if (((double) topologicalCounter / (double) nextOrderNumber) > topologicalGrowthFactor) {
        orderStatementsInternal();
        return;
      }
    }

    if ((nEvaluated - evaluationsAtLastOrdering) > maxEvalBetweenTopo) {
      orderStatementsInternal();
      return;
    }
  }

  /**
   * Re-order the step definitions.
   */
  private void reorder() {
    // drain the worklist
    LinkedList<AbstractStatement> temp = new LinkedList<>();
    while (!workList.isEmpty()) {
      AbstractStatement eq = workList.takeStatement();
      temp.add(eq);
    }
    workList = new Worklist();

    // compute new ordering
    getFixedPointSystem().reorder();

    // re-populate worklist
    for (AbstractStatement s : temp) {
      workList.insertStatement(s);
    }
  }

  public static boolean isChanged(byte code) {
    return (code & CHANGED_MASK) != 0;
  }

  public static boolean isSideEffect(byte code) {
    return (code & SIDE_EFFECT_MASK) != 0;
  }

  public static boolean isFixed(byte code) {
    return (code & FIXED_MASK) != 0;
  }

  public int getMinSizeForTopSort() {
    return minSizeForTopSort;
  }

  /**
   * @param i
   */
  public void setMinEquationsForTopSort(int i) {
    minSizeForTopSort = i;
  }

  public int getMaxEvalBetweenTopo() {
    return maxEvalBetweenTopo;
  }

  public double getTopologicalGrowthFactor() {
    return topologicalGrowthFactor;
  }

  /**
   * @param i
   */
  public void setMaxEvalBetweenTopo(int i) {
    maxEvalBetweenTopo = i;
  }

  /**
   * @param d
   */
  public void setTopologicalGrowthFactor(double d) {
    topologicalGrowthFactor = d;
  }

  public int getNumberOfEvaluations() {
    return nEvaluated;
  }

  public void incNumberOfEvaluations() {
    nEvaluated++;
  }

  /**
   * a method that will be called every N evaluations. subclasses should override as desired.
   */
  protected void periodicMaintenance() {
  }

  /**
   * subclasses should override as desired.
   */
  protected int getVerboseInterval() {
    return DEFAULT_VERBOSE_INTERVAL;
  }

  /**
   * subclasses should override as desired.
   */
  protected int getPeriodicMaintainInterval() {
    return DEFAULT_PERIODIC_MAINTENANCE_INTERVAL;
  }
}
