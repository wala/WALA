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
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * An object which represent Def-Use information for an SSA {@link IR}
 */
public class DefUse {
  static final boolean DEBUG = false;

  /**
   * A mapping from integer (value number) -&gt; {@link SSAInstruction} that defines the value
   */
  final private SSAInstruction[] defs;

  /**
   * A mapping from integer (value number) -&gt; bit vector holding integers representing instructions that use the value number
   */
  final private MutableIntSet[] uses;

  /**
   * A Mapping from integer -&gt; Instruction
   */
  final protected ArrayList<SSAInstruction> allInstructions = new ArrayList<>();

  /**
   * prevent the IR from being collected while this is live.
   */
  private final IR ir;

  /**
   * @param ir an IR in SSA form.
   * @throws IllegalArgumentException if ir is null
   */
  public DefUse(final IR ir) {
    if (ir == null) {
      throw new IllegalArgumentException("ir is null");
    }
    this.ir = ir;

    // set up mapping from integer -> instruction
    initAllInstructions();
    defs = new SSAInstruction[getMaxValueNumber() + 1];
    uses = new MutableIntSet[getMaxValueNumber() + 1];
    if (DEBUG) {
      System.err.println(("DefUse: defs.length " + defs.length));
    }
    Iterator<SSAInstruction> it = allInstructions.iterator();
    for (int i = 0; i < allInstructions.size(); i++) {
      SSAInstruction s = it.next();
      if (s == null) {
        continue;
      }
      for (int j = 0; j < getNumberOfDefs(s); j++) {
        defs[getDef(s, j)] = s;
      }
      for (int j = 0; j < getNumberOfUses(s); j++) {
        int use = getUse(s, j);
        try {
          if (use != -1) {
            if (uses[use] == null) {
              uses[use] = IntSetUtil.make();
            }
            uses[use].add(i);
          }
        } catch (ArrayIndexOutOfBoundsException e) {
          assert false : "unexpected value number " + use;
        }
      }
    }
  }

  /**
   * @return the maximum value number in a particular IR
   */
  protected int getMaxValueNumber() {
    return ir.getSymbolTable().getMaxValueNumber();
  }

  /**
   * Initialize the allInstructions field with every {@link SSAInstruction} in the ir.
   */
  protected void initAllInstructions() {
    for (SSAInstruction inst : Iterator2Iterable.make(ir.iterateAllInstructions())) {
      allInstructions.add(inst);
    }
  }

  /**
   * What is the ith value number defined by instruction s?
   */
  protected int getDef(SSAInstruction s, int i) {
    return s.getDef(i);
  }

  /**
   * What is the ith value number used by instruction s?
   */
  protected int getUse(SSAInstruction s, int i) {
    return s.getUse(i);
  }

  /**
   * How many value numbers does instruction s def?
   */
  protected int getNumberOfDefs(SSAInstruction s) {
    return s.getNumberOfDefs();
  }

  /**
   * How many value numbers does instruction s use?
   */
  protected int getNumberOfUses(SSAInstruction s) {
    return s.getNumberOfUses();
  }

  /**
   * @return the {@link SSAInstruction} that defines the variable with value number v.
   */
  public SSAInstruction getDef(int v) {
    return (v < defs.length) ? defs[v] : null;
  }

  /**
   * Return all uses of the variable with the given value number
   */
  public Iterator<SSAInstruction> getUses(int v) {
    if (uses[v] == null) {
      return EmptyIterator.instance();
    } else {
      return new UseIterator(uses[v]);
    }
  }

  /**
   * return an {@link Iterator} of all instructions that use any of a set of variables
   */
  private class UseIterator implements Iterator<SSAInstruction> {
    final IntIterator it;

    /**
     * @param uses the set of value numbers whose uses this object iterates over
     */
    UseIterator(IntSet uses) {
      it = uses.intIterator();
    }

    @Override
    public boolean hasNext() {
      return it.hasNext();
    }

    @Override
    public SSAInstruction next() {
      return allInstructions.get(it.next());
    }

    @Override
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
}
