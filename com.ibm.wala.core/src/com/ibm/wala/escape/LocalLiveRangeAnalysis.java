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
package com.ibm.wala.escape;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;

import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * Intraprocedural SSA-based live range analysis. This is horribly inefficient.
 * 
 */
public class LocalLiveRangeAnalysis {

  /**
   * Is the variable with value number v live immediately after a particular instruction index?
   * 
   * Algorithm: returns true if there is a path from pc to some use of v that does not traverse the def of v
   * 
   * @param instructionIndex index of an instruction in the IR
   * @throws IllegalArgumentException if du is null
   */
  public static boolean isLive(int v, int instructionIndex, IR ir, DefUse du) {
    if (du == null) {
      throw new IllegalArgumentException("du is null");
    }
    if (du.getNumberOfUses(v) == 0) {
      return false;
    }
    if (instructionIndex < 0) {
      Assertions.UNREACHABLE();
    }
    ISSABasicBlock queryBlock = findBlock(ir, instructionIndex);
    SSAInstruction def = du.getDef(v);
    final SSACFG.BasicBlock defBlock = def == null ? null : findBlock(ir, def);
    final Collection<BasicBlock> uses = findBlocks(ir, du.getUses(v));

    // a filter which accepts everything but the block which defs v
    Predicate<Object> notDef = o -> (defBlock == null || !defBlock.equals(o));

    if (defBlock != null && defBlock.equals(queryBlock)) {
      // for now, conservatively say it's live. fix this later if necessary.
      return true;
    } else {
      Collection reached = DFS.getReachableNodes(ir.getControlFlowGraph(), Collections.singleton(queryBlock), notDef);
      uses.retainAll(reached);
      if (uses.isEmpty()) {
        return false;
      } else if (uses.size() == 1 && uses.iterator().next().equals(queryBlock)) {
        if (instructionIndex == queryBlock.getLastInstructionIndex()) {
          // the query is for the last instruction. There can be no more uses
          // following the block, so:
          // not quite true for return instructions.
          if (ir.getInstructions()[instructionIndex] instanceof SSAReturnInstruction) {
            return true;
          } else {
            return false;
          }
        } else {
          // todo: be more aggressive.
          // there may be more uses after the query, so conservatively assume it
          // may be live
          return true;
        }
      } else {
        return true;
      }
    }
  }

  /**
   * @param statements Iterator<SSAInstruction>
   */
  private static Collection<BasicBlock> findBlocks(IR ir, Iterator<SSAInstruction> statements) {
    Collection<SSAInstruction> s = Iterator2Collection.toSet(statements);
    Collection<BasicBlock> result = HashSetFactory.make();
    outer: for (ISSABasicBlock issaBasicBlock : ir.getControlFlowGraph()) {
      SSACFG.BasicBlock b = (SSACFG.BasicBlock) issaBasicBlock;
      for (SSAInstruction x : b) {
        if (s.contains(x)) {
          result.add(b);
          continue outer;
        }
      }
    }
    if (result.isEmpty()) {
      Assertions.UNREACHABLE();
    }
    return result;
  }

  /**
   * This is horribly inefficient.
   * 
   * @return the basic block which contains the instruction
   */
  private static SSACFG.BasicBlock findBlock(IR ir, SSAInstruction s) {
    if (s == null) {
      Assertions.UNREACHABLE();
    }
    for (ISSABasicBlock issaBasicBlock : ir.getControlFlowGraph()) {
      SSACFG.BasicBlock b = (SSACFG.BasicBlock) issaBasicBlock;
      for (SSAInstruction x : b) {
        if (s.equals(x)) {
          return b;
        }
      }
    }
    Assertions.UNREACHABLE("no block for " + s + " in IR " + ir);
    return null;
  }

  /**
   * This is horribly inefficient.
   * 
   * @return the basic block which contains the ith instruction
   */
  private static ISSABasicBlock findBlock(IR ir, int i) {
    for (ISSABasicBlock issaBasicBlock : ir.getControlFlowGraph()) {
      SSACFG.BasicBlock b = (SSACFG.BasicBlock) issaBasicBlock;
      if (i >= b.getFirstInstructionIndex() && i <= b.getLastInstructionIndex()) {
        return b;
      }
    }
    Assertions.UNREACHABLE("no block for " + i + " in IR " + ir);
    return null;
  }

}
