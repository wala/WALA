/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cfg.exc.inter;

import java.util.HashMap;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.exc.ExceptionPruningAnalysis;
import com.ibm.wala.cfg.exc.intra.NullPointerState;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

/**
 * Saves interprocedural state of a single method.
 *
 * This class has been developed as part of a student project "Studienarbeit" by Markus Herhoffer.
 * It has been adapted and integrated into the WALA project by Juergen Graf.
 * 
 * @author Markus Herhoffer &lt;markus.herhoffer@student.kit.edu&gt;
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 * 
 */
final class IntraprocAnalysisState implements ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> {

  private final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg;
  private final HashMap<IExplodedBasicBlock, NullPointerState> statesOfSsaVars =
      new HashMap<>();
  private final HashMap<IExplodedBasicBlock, Object[]> valuesOfSsaVars = new HashMap<>();
  private final HashMap<IExplodedBasicBlock, int[]> numbersOfSsaVarsThatAreParemerters =
      new HashMap<>();
  private final boolean noAnalysisPossible;
  private final int deletedEdges;
  private boolean throwsException = true;

  /**
   * Constructor for the state of a method that has not been analyzed. These are methods with an empty IR or methods
   * left out explicitly.
   * 
   * Use it if you have nothing to tell about the node.
   */
  IntraprocAnalysisState() {
    this.cfg = null;
    this.noAnalysisPossible = true;
    this.deletedEdges = 0;
  }

  /**
   * Constructor if you have informations on the node.
   * 
   * All values are saved at construction time. So if the analysis changes
   * anything after this OptimizationInfo was created, it won't affect its final
   * attributes.
   * 
   * @param intra
   *          The <code>node</code>'s intraprocedural analysis
   * @param node
   *          the node itself
   * @throws UnsoundGraphException
   * @throws CancelException
   */
  IntraprocAnalysisState(final ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intra, final CGNode node,
      final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg, final int deletedEdges)
      throws UnsoundGraphException, CancelException {
    this.cfg = cfg;
    this.noAnalysisPossible = false;
    this.deletedEdges = deletedEdges;
    final SymbolTable sym = node.getIR().getSymbolTable();
    
    for (final IExplodedBasicBlock block : cfg) {
      // set states
      final NullPointerState state = intra.getState(block);
      this.statesOfSsaVars.put(block, state);

      // set values
      if (block.getInstruction() != null) {
        final int numberOfSSAVars = block.getInstruction().getNumberOfUses();
        final Object[] values = new Object[numberOfSSAVars];

        for (int j = 0; j < numberOfSSAVars; j++) {
          final boolean isContant = sym.isConstant(j);
          values[j] = (isContant ? sym.getConstantValue(j) : null);
        }

        this.valuesOfSsaVars.put(block, values);
      } else {
        this.valuesOfSsaVars.put(block, null);
      }

      // set nr. of parameters
      if (block.getInstruction() instanceof SSAAbstractInvokeInstruction) {
        final SSAAbstractInvokeInstruction instr = (SSAAbstractInvokeInstruction) block.getInstruction();
        final int[] numbersOfParams = AnalysisUtil.getParameterNumbers(instr);
        this.numbersOfSsaVarsThatAreParemerters.put(block, numbersOfParams);
      } else {
        // default to null
        this.numbersOfSsaVarsThatAreParemerters.put(block, null);
      }
    }
  }

  @Override
  public int compute(IProgressMonitor progress) throws UnsoundGraphException, CancelException {
    return deletedEdges;
  }
  
  @Override
  public NullPointerState getState(final IExplodedBasicBlock block) {
    if (noAnalysisPossible) {
      throw new IllegalStateException();
    }

    return statesOfSsaVars.get(block);
  }

  public Object[] getValues(final IExplodedBasicBlock block) {
    if (noAnalysisPossible) {
      throw new IllegalStateException();
    }

    return valuesOfSsaVars.get(block);
  }

  public int[] getInjectedParameters(final IExplodedBasicBlock block) {
    if (noAnalysisPossible) {
      throw new IllegalStateException();
    } else if (!((block.getInstruction() instanceof SSAAbstractInvokeInstruction))) {
      throw new IllegalArgumentException();
    }

    assert (block.getInstruction() instanceof SSAAbstractInvokeInstruction);

    return numbersOfSsaVarsThatAreParemerters.get(block);
  }

  /**
   * Returns the CFG.
   * 
   * @return the CFG or null if there is no CFG for the CGNode.
   */
  @Override
  public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> getCFG() {
    return (noAnalysisPossible ? null : this.cfg);
  }

  public boolean canBeAnalyzed() {
    return !noAnalysisPossible;
  }

  public void setHasExceptions(final boolean throwsException) {
    this.throwsException = throwsException;
  }

  @Override
  public boolean hasExceptions() {
    return throwsException;
  }

  @Override
  public String toString() {
    if (noAnalysisPossible) {
      return "";
    }

    final String ls = System.getProperty("line.separator");
    final StringBuffer output = new StringBuffer();
    output.append(statesOfSsaVars.toString() + ls);
    output.append(valuesOfSsaVars.toString() + ls);
    output.append(numbersOfSsaVarsThatAreParemerters.toString());

    return output.toString();
  }

}
