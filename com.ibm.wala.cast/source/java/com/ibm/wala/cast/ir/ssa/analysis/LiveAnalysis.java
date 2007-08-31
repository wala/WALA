/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ir.ssa.analysis;

import java.util.Iterator;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;

/**
 * @author Julian Dolby
 * 
 * live-value analysis TODO: document me!
 */
public class LiveAnalysis {

  public interface Result {
    boolean isLiveEntry(SSACFG.BasicBlock bb, int valueNumber);

    boolean isLiveExit(SSACFG.BasicBlock bb, int valueNumber);

    BitVector getLiveBefore(int instr);
  }

  public static LiveAnalysis.Result perform(final ControlFlowGraph cfg, final SymbolTable symtab) {
    return perform(cfg, symtab, new BitVector());
  }

  public static LiveAnalysis.Result perform(final ControlFlowGraph cfg, final SymbolTable symtab, final BitVector considerLiveAtExit) {
    final BitVectorIntSet liveAtExit = new BitVectorIntSet(considerLiveAtExit);
    final SSAInstruction[] instructions = (SSAInstruction[]) cfg.getInstructions();

    final class ExitBlockGenKillOperator extends UnaryOperator {
      public String toString() {
        return "ExitGenKill";
      }

      public boolean equals(Object o) {
        return o == this;
      }

      public int hashCode() {
        return 37721;
      }

      public byte evaluate(IVariable lhs, IVariable rhs) {
        BitVectorVariable L = (BitVectorVariable) lhs;
        boolean changed = L.getValue() == null ? !considerLiveAtExit.isZero() : !L.getValue().sameValue(liveAtExit);

        L.addAll(considerLiveAtExit);

        return changed ? CHANGED : NOT_CHANGED;
      }
    }

    final class BlockValueGenKillOperator extends UnaryOperator {
      private final SSACFG.BasicBlock block;

      BlockValueGenKillOperator(SSACFG.BasicBlock block) {
        this.block = block;
      }

      public String toString() {
        return "GenKill:" + block;
      }

      public boolean equals(Object o) {
        return (o instanceof BlockValueGenKillOperator) && ((BlockValueGenKillOperator) o).block.equals(block);
      }

      public int hashCode() {
        return block.hashCode() * 17;
      }

      private void processDefs(SSAInstruction inst, BitVector bits) {
        for (int j = 0; j < inst.getNumberOfDefs(); j++) {
          bits.clear(inst.getDef(j));
        }
      }

      private void processUses(SSAInstruction inst, BitVector bits) {
        for (int j = 0; j < inst.getNumberOfUses(); j++) {
          Assertions._assert(inst.getUse(j) != -1, inst.toString());
          if (!symtab.isConstant(inst.getUse(j))) {
            bits.set(inst.getUse(j));
          }
        }
      }

      public byte evaluate(IVariable lhs, IVariable rhs) {
        BitVectorVariable L = (BitVectorVariable) lhs;
        IntSet s = ((BitVectorVariable) rhs).getValue();
        BitVectorIntSet bits = new BitVectorIntSet();
        if (s != null) {
          bits.addAll(s);
        }

        for (Iterator sBBs = cfg.getSuccNodes(block); sBBs.hasNext();) {
          SSACFG.BasicBlock sBB = (SSACFG.BasicBlock) sBBs.next();
          int rval = com.ibm.wala.cast.ir.cfg.Util.whichPred(cfg, sBB, block);
          for (Iterator sphis = sBB.iteratePhis(); sphis.hasNext();) {
            SSAPhiInstruction sphi = (SSAPhiInstruction) sphis.next();
            bits.add(sphi.getUse(rval));
          }
        }
        for (int i = block.getLastInstructionIndex(); i >= block.getFirstInstructionIndex(); i--) {
          SSAInstruction inst = instructions[i];
          if (inst != null) {
            processDefs(inst, bits.getBitVector());
            processUses(inst, bits.getBitVector());
          }
        }
        for (Iterator SS = block.iteratePhis(); SS.hasNext();) {
          processDefs((SSAInstruction) SS.next(), bits.getBitVector());
        }

        BitVectorVariable U = new BitVectorVariable();
        U.addAll(bits.getBitVector());

        if (!L.sameValue(U)) {
          L.copyState(U);
          return CHANGED;
        } else {
          return NOT_CHANGED;
        }
      }
    }

    final BitVectorSolver<IBasicBlock> S = new BitVectorSolver<IBasicBlock>(new IKilldallFramework<IBasicBlock, BitVectorVariable>() {
      private final Graph<IBasicBlock> G = GraphInverter.invert(cfg);

      public Graph<IBasicBlock> getFlowGraph() {
        return G;
      }

      public ITransferFunctionProvider<IBasicBlock, BitVectorVariable> getTransferFunctionProvider() {
        return new ITransferFunctionProvider<IBasicBlock, BitVectorVariable>() {

          public boolean hasNodeTransferFunctions() {
            return true;
          }

          public boolean hasEdgeTransferFunctions() {
            return false;
          }

          public UnaryOperator getNodeTransferFunction(IBasicBlock node) {
            if (((SSACFG.BasicBlock) node).isExitBlock()) {
              return new ExitBlockGenKillOperator();
            } else {
              return new BlockValueGenKillOperator((SSACFG.BasicBlock) node);
            }
          }

          public UnaryOperator getEdgeTransferFunction(IBasicBlock s, IBasicBlock d) {
            Assertions.UNREACHABLE();
            return null;
          }

          public AbstractMeetOperator getMeetOperator() {
            return BitVectorUnion.instance();
          }
        };
      }
    });

    S.solve();

    return new Result() {

      public String toString() {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < cfg.getNumberOfNodes(); i++) {
          SSACFG.BasicBlock bb = (SSACFG.BasicBlock) cfg.getNode(i);
          s.append("live entering " + bb + ":" + S.getOut(bb) + "\n");
          s.append("live exiting " + bb + ":" + S.getIn(bb) + "\n");
        }

        return s.toString();
      }

      public boolean isLiveEntry(SSACFG.BasicBlock bb, int valueNumber) {
        return ((BitVectorVariable) S.getOut(bb)).get(valueNumber);
      }

      public boolean isLiveExit(SSACFG.BasicBlock bb, int valueNumber) {
        return ((BitVectorVariable) S.getIn(bb)).get(valueNumber);
      }

      public BitVector getLiveBefore(int instr) {
        SSACFG.BasicBlock bb = (SSACFG.BasicBlock) cfg.getBlockForInstruction(instr);
        IntSet s = ((BitVectorVariable) S.getIn(bb)).getValue();
        BitVectorIntSet bits = new BitVectorIntSet();
        if (s != null) {
          bits.addAll(s);
        }

        for (int i = bb.getLastInstructionIndex(); i >= instr; i--) {
          SSAInstruction inst = instructions[i];
          if (inst != null) {
            for (int j = 0; j < inst.getNumberOfDefs(); j++) {
              bits.remove(inst.getDef(j));
            }
            for (int j = 0; j < inst.getNumberOfUses(); j++) {
              if (!symtab.isConstant(inst.getUse(j))) {
                bits.add(inst.getUse(j));
              }
            }
          }
        }

        return bits.getBitVector();
      }
    };
  }

  public static LiveAnalysis.Result perform(IR ir) {
    return perform(ir.getControlFlowGraph(), ir.getSymbolTable());
  }

}
