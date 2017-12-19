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
package com.ibm.wala.ssa.analysis;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.fixedpoint.impl.DefaultFixedPointSolver;
import com.ibm.wala.fixpoint.BooleanVariable;
import com.ibm.wala.fixpoint.UnaryOr;
import com.ibm.wala.ssa.*;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * Eliminate dead assignments (phis) from an SSA IR.
 */
public class DeadAssignmentElimination {

  private static final boolean DEBUG = false;

  /**
   * eliminate dead phis from an ir
   * @throws IllegalArgumentException  if ir is null
   */
  public static void perform(IR ir) {
    if (ir == null) {
      throw new IllegalArgumentException("ir is null");
    }
    DefUse DU = new DefUse(ir);
    DeadValueSystem system = new DeadValueSystem(ir, DU);
    try {
      system.solve(null);
    } catch (CancelException e) {
      throw new CancelRuntimeException(e);
    }
    doTransformation(ir,system);
  }

  /**
   * Perform the transformation
   * @param ir IR to transform
   * @param solution dataflow solution for dead assignment elimination
   */
  private static void doTransformation(IR ir, DeadValueSystem solution) {
    ControlFlowGraph<?, ISSABasicBlock> cfg = ir.getControlFlowGraph();
    for (ISSABasicBlock issaBasicBlock : cfg) {
      BasicBlock b = (BasicBlock) issaBasicBlock;
      if (DEBUG) {
        System.err.println("eliminateDeadPhis: " + b);
      }
      if (b.hasPhi()) {
        HashSet<SSAPhiInstruction> toRemove = HashSetFactory.make(5);
        for (SSAPhiInstruction phi : Iterator2Iterable.make(b.iteratePhis())) {
          if (phi != null) {
            int def = phi.getDef();
            if (solution.isDead(def)) {
              if (DEBUG) {
                System.err.println("Will remove phi: " + phi);
              }
              toRemove.add(phi);
            }
          }
        }
        b.removePhis(toRemove);
      }
    }
  }

  /**
   * A dataflow system which computes whether or not a value is dead
   */
  private static class DeadValueSystem extends DefaultFixedPointSolver<BooleanVariable> {

    /**
     * Map: value number -&gt; BooleanVariable isLive
     */
    final private Map<Integer, BooleanVariable> vars = HashMapFactory.make();

    /**
     * set of value numbers that are trivially dead
     */
    final private HashSet<Integer> trivialDead = HashSetFactory.make();

    /**
     * @param ir the IR to analyze
     * @param DU def-use information for the IR
     */
    DeadValueSystem(IR ir, DefUse DU) {
      // create a variable for each potentially dead phi instruction.
      for (SSAInstruction inst : Iterator2Iterable.make(ir.iteratePhis())) {
        SSAPhiInstruction phi = (SSAPhiInstruction) inst;
        if (phi == null) {
          continue;
        }
        int def = phi.getDef();
        if (DU.getNumberOfUses(def) == 0) {
          // the phi is certainly dead ... record this with a dataflow fact.
          trivialDead.add(new Integer(def));
        } else {
          boolean maybeDead = true;
          for (SSAInstruction u : Iterator2Iterable.make(DU.getUses(def))) {
            if (!(u instanceof SSAPhiInstruction)) {
              // certainly not dead
              maybeDead = false;
              break;
            }
          }
          if (maybeDead) {
            // perhaps the phi is dead .. create a variable
            BooleanVariable B = new BooleanVariable(false);
            vars.put(new Integer(def), B);
          }
        }
      }

      // Now create dataflow equations; v is live iff any phi that uses v is live
      for (Entry<Integer, BooleanVariable> E : vars.entrySet()) {
        Integer def = E.getKey();
        BooleanVariable B = E.getValue();
        for (SSAInstruction use : Iterator2Iterable.make(DU.getUses(def.intValue()))) {
          SSAPhiInstruction u = (SSAPhiInstruction) use;
          Integer ud = new Integer(u.getDef());
          if (trivialDead.contains(ud)) {
            // do nothing ... u will not keep def live
          } else {
            if (!vars.keySet().contains(ud)) {
              // u is not potentially dead ... certainly v is live.
              // record this.
              B.set(true);
            } else {
              // maybe u is dead?
              // add constraint v is live if u is live.
              BooleanVariable U = vars.get(ud);
              newStatement(B, UnaryOr.instance(), U, true, false);
            }
          }
        }
      }
    }

    @Override
    protected void initializeVariables() {
      //do nothing: all variables are initialized to false (TOP), meaning "not live"
    }

    @Override
    protected void initializeWorkList() {
      addAllStatementsToWorkList();
    }

    /**
     * @param value
     * @return true iff there are no uses of the given value number
     */
    private boolean isDead(int value) {
      Integer V = Integer.valueOf(value);
      if (trivialDead.contains(V)) {
        return true;
      } else {
        BooleanVariable B = vars.get(V);
        if (B == null) {
          return false;
        } else {
          return !B.getValue();
        }
      }
    }

    @Override
    protected BooleanVariable[] makeStmtRHS(int size) {
      return new BooleanVariable[size];
    }

  }
}
