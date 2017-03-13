/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.examples.analysis.dataflow;

import java.util.ArrayList;
import java.util.Map;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorKillGen;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Compute intraprocedural reaching defs of global variables, i.e., the defs are
 * {@link SSAPutInstruction}s on static state.
 * 
 * @author manu
 * 
 */
public class IntraprocReachingDefs {

  /**
   * the exploded control-flow graph on which to compute the analysis
   */
  private final ExplodedControlFlowGraph ecfg;

  /**
   * maps the index of a putstatic IR instruction to a more compact numbering for use in bitvectors
   */
  private final OrdinalSetMapping<Integer> putInstrNumbering;

  /**
   * used to resolve references to fields in putstatic instructions
   */
  private final IClassHierarchy cha;

  /**
   * maps each static field to the numbers of the statements (in {@link #putInstrNumbering}) that define it; used for kills in flow
   * functions
   */
  private final Map<IField, BitVector> staticField2DefStatements = HashMapFactory.make();

  private static final boolean VERBOSE = true;

  public IntraprocReachingDefs(ExplodedControlFlowGraph ecfg, IClassHierarchy cha) {
    this.ecfg = ecfg;
    this.cha = cha;
    this.putInstrNumbering = numberPutStatics();
  }

  /**
   * generate a numbering of the putstatic instructions
   */
  private OrdinalSetMapping<Integer> numberPutStatics() {
    ArrayList<Integer> putInstrs = new ArrayList<>();
    IR ir = ecfg.getIR();
    SSAInstruction[] instructions = ir.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      SSAInstruction instruction = instructions[i];
      if (instruction instanceof SSAPutInstruction && ((SSAPutInstruction) instruction).isStatic()) {
        SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
        // instrNum is the number that will be assigned to this putstatic
        int instrNum = putInstrs.size();
        putInstrs.add(i);
        // also update the mapping of static fields to def'ing statements
        IField field = cha.resolveField(putInstr.getDeclaredField());
        assert field != null;
        BitVector bv = staticField2DefStatements.get(field);
        if (bv == null) {
          bv = new BitVector();
          staticField2DefStatements.put(field, bv);
        }
        bv.set(instrNum);
      }
    }
    return new ObjectArrayMapping<>(putInstrs.toArray(new Integer[putInstrs.size()]));
  }

  private class TransferFunctions implements ITransferFunctionProvider<IExplodedBasicBlock, BitVectorVariable> {

    @Override
    public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(IExplodedBasicBlock src, IExplodedBasicBlock dst) {
      throw new UnsupportedOperationException();
    }

    /**
     * our meet operator is set union
     */
    @Override
    public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
      return BitVectorUnion.instance();
    }

    @Override
    public UnaryOperator<BitVectorVariable> getNodeTransferFunction(IExplodedBasicBlock node) {
      SSAInstruction instruction = node.getInstruction();
      int instructionIndex = node.getFirstInstructionIndex();
      if (instruction instanceof SSAPutInstruction && ((SSAPutInstruction) instruction).isStatic()) {
        // kill all defs of the same static field, and gen this instruction
        final SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
        final IField field = cha.resolveField(putInstr.getDeclaredField());
        assert field != null;
        BitVector kill = staticField2DefStatements.get(field);
        BitVector gen = new BitVector();
        gen.set(putInstrNumbering.getMappedIndex(instructionIndex));
        return new BitVectorKillGen(kill, gen);
      } else {
        // identity function for non-putstatic instructions
        return BitVectorIdentity.instance();
      }
    }

    @Override
    public boolean hasEdgeTransferFunctions() {
      // we only need transfer functions on nodes
      return false;
    }

    @Override
    public boolean hasNodeTransferFunctions() {
      return true;
    }

  }

  /**
   * run the analysis
   * 
   * @return the solver used for the analysis, which contains the analysis result
   */
  public BitVectorSolver<IExplodedBasicBlock> analyze() {
    // the framework describes the dataflow problem, in particular the underlying graph and the transfer functions
    BitVectorFramework<IExplodedBasicBlock, Integer> framework = new BitVectorFramework<>(ecfg,
        new TransferFunctions(), putInstrNumbering);
    BitVectorSolver<IExplodedBasicBlock> solver = new BitVectorSolver<>(framework);
    try {
      solver.solve(null);
    } catch (CancelException e) {
      // this shouldn't happen
      assert false;
    }
    if (VERBOSE) {
      for (IExplodedBasicBlock ebb : ecfg) {
        System.out.println(ebb);
        System.out.println(ebb.getInstruction());
        System.out.println(solver.getIn(ebb));
        System.out.println(solver.getOut(ebb));
      }
    }
    return solver;
  }
}
