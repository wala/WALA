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
package com.ibm.wala.demandpa.flowgraph;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.graph.labeled.LabeledGraph;

public interface IFlowGraph extends LabeledGraph<Object, IFlowLabel> {

  /**
   * Apply a visitor to the successors of some node.
   */
  public abstract void visitSuccs(Object node, IFlowLabelVisitor v);

  /**
   * Apply a visitor to the predecessors of some node.
   */
  public abstract void visitPreds(Object node, IFlowLabelVisitor v);

  /**
   * add representation of flow for a node, if not already present
   * 
   * @throws IllegalArgumentException if node == null
   */
  public abstract void addSubgraphForNode(CGNode node) throws IllegalArgumentException;

  public abstract boolean hasSubgraphForNode(CGNode node);

  /**
   * @param pk
   * @return <code>true</code> iff <code>pk</code> is a formal parameter
   */
  public abstract boolean isParam(LocalPointerKey pk);

  /**
   * @param pk
   * @return the {@link SSAInvokeInstruction}s passing some pointer as a parameter
   */
  public abstract Iterator<SSAAbstractInvokeInstruction> getInstrsPassingParam(LocalPointerKey pk);

  /**
   * get the {@link SSAInvokeInstruction} whose return value is assigned to a pointer key.
   * 
   * @return the instruction, or <code>null</code> if no return value is assigned to pk
   */
  public abstract SSAAbstractInvokeInstruction getInstrReturningTo(LocalPointerKey pk);

  /**
   * @param sfk the static field
   * @return all the variables whose values are written to sfk
   * @throws IllegalArgumentException if sfk == null
   */
  public abstract Iterator<? extends Object> getWritesToStaticField(StaticFieldKey sfk) throws IllegalArgumentException;

  /**
   * @param sfk the static field
   * @return all the variables that get the value of sfk
   * @throws IllegalArgumentException if sfk == null
   */
  public abstract Iterator<? extends Object> getReadsOfStaticField(StaticFieldKey sfk) throws IllegalArgumentException;

  public abstract Iterator<PointerKey> getWritesToInstanceField(PointerKey pk, IField f);

  public abstract Iterator<PointerKey> getReadsOfInstanceField(PointerKey pk, IField f);

  /**
   * 
   * @param formalPk a {@link PointerKey} representing either a formal parameter or return value
   * @return the {@link CallerSiteContext}s representing pointer callers of <code>formalPk</code>'s method
   */
  public abstract Set<CallerSiteContext> getPotentialCallers(PointerKey formalPk);

  /**
   * get the callees that should be considered at a particular call site
   * 
   * @param caller the caller
   * @param site the call site
   * @param actualPk a {@link LocalPointerKey} corresponding to the actual parameter or return value of interest. This may be used
   *          to filter out certain callees.
   * @return the callees of interest
   */
  public abstract Set<CGNode> getPossibleTargets(CGNode caller, CallSiteReference site, LocalPointerKey actualPk);

}
