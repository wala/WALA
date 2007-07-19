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
package com.ibm.wala.ssa;

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * 
 * An object which represent Def-Use information for an SSA IR
 * 
 * @author sfink
 */
public class DefUse {
  static final boolean DEBUG = false;

  /**
   * A mapping from integer (value number) -> Instruction that defines the value
   */
  final private SSAInstruction[] defs;

  /**
   * A mapping from integer (value number) -> BitVector holding integers
   * representing instructions that use the value number
   */
  final private MutableIntSet[] uses;

  /**
   * A Mapping from integer -> Instruction
   */
  final protected ArrayList<SSAInstruction> allInstructions = new ArrayList<SSAInstruction>();

  /**
   * prevent the IR from being collected while this is live.
   */
  private final IR ir;
  
  /**
   * keep this package private: all calls should be through SSACache
   * @param ir
   *          an IR in SSA form.
   * @throws IllegalArgumentException  if ir is null
   */
  public DefUse(final IR ir) {
    if (ir == null) {
      throw new IllegalArgumentException("ir is null");
    }
    this.ir = ir;

    // set up mapping from integer -> instruction
    getAllInstructions();
    defs = new SSAInstruction[getMaxValueNumber() + 1];
    uses = new MutableIntSet[getMaxValueNumber() + 1];
    if (DEBUG) {
      Trace.println("DefUse: defs.length " + defs.length);
    }
    Iterator it = allInstructions.iterator();
    for (int i = 0; i < allInstructions.size(); i++) {
      SSAInstruction s = (SSAInstruction) it.next();
      if (s == null) {
        continue;
      }
      for (int j = 0; j < getNumberOfDefs(s); j++) {
        defs[getDef(s,j)] = s;
      }
      for (int j = 0; j < getNumberOfUses(s); j++) {
        int use = getUse(s,j);
        if (use != -1) {
          if (uses[use] == null) {
            uses[use] = IntSetUtil.make();
          }
          uses[use].add(i);
        }
      }
    }
  }

  protected int getMaxValueNumber() {
    return ir.getSymbolTable().getMaxValueNumber();
  }

  protected void getAllInstructions() {
    for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); 
	 it.hasNext();) 
    {
      allInstructions.add(it.next());
    }
  }

  protected int getDef(SSAInstruction s, int i) {
    return s.getDef(i);
  }

  protected int getUse(SSAInstruction s, int i) {
    return s.getUse(i);
  }

  protected int getNumberOfDefs(SSAInstruction s) {
    return s.getNumberOfDefs();
  }

  protected int getNumberOfUses(SSAInstruction s) {
    return s.getNumberOfUses();
  }

  /**
   * @return the Instruction that defines the variable with value number v.
   */
  public SSAInstruction getDef(int v) {    
      return (v < defs.length)? defs[v]: null;
  }

  /**
   * Return all uses of the variable with the given value number
   * 
   * @param v
   *          value number
   */
  public Iterator<SSAInstruction> getUses(int v) {
    if (uses[v] == null) {
      return EmptyIterator.instance();
    } else {
      return new UseIterator(uses[v]);
    }
  }

  /**
   * @author sfink
   *
   * return an iterator of all instructions that use any of a set of variables
   */
  private class UseIterator implements Iterator<SSAInstruction> {
    final IntIterator it;

    /**
     * @param uses the set of value numbers whose uses this object iterates over
     */
    UseIterator(IntSet uses) {
      it = uses.intIterator();
    }

    public boolean hasNext() {
      return it.hasNext();
    }

    public SSAInstruction next() {
      return allInstructions.get(it.next());
    }

    public void remove() {
      Assertions.UNREACHABLE();
    }
  }

  /**
   * @param v a value number
   * @return the number of uses of the variable with the given value number
   */
  public int getNumberOfUses(int v) {
    return uses[v] == null ? 0 : uses[v].size();
  }
  
  /**
   * Return the actual, honest-to-goodness IR object used to compute the
   * DefUse information.   By doing this, you ensure that for any
   * SSAInstruction returned by getUses() or getDefs(), the instruction
   * actually == (equals()) an SSAInstruction in the IR.
   * 
   * This is pretty horrible.   TODO: Should think about redesigning instruction
   * identity to avoid this.
   */
  public IR getIR() {
    return ir;
  }
}
