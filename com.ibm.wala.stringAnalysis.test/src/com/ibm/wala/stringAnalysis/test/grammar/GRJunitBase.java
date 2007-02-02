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
package com.ibm.wala.stringAnalysis.test.grammar;

import java.util.Collection;

import com.ibm.wala.util.debug.Assertions;

import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Symbol;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.ValueDecorator;
import com.ibm.wala.stringAnalysis.test.SAJunitBase;

abstract public class GRJunitBase extends SAJunitBase {
    State s0 = new State("s0");
    State s1 = new State("s1");
    State s2 = new State("s2");
    State s3 = new State("s3");
    State s4 = new State("s4");
    Symbol i0 = new Symbol("i0");
    Symbol i1 = new Symbol("i1");
    Symbol i2 = new Symbol("i2");
    Symbol i3 = new Symbol("i3");
    Symbol i4 = new Symbol("i4");
    Symbol i5 = new Symbol("i5");
    Symbol i6 = new Symbol("i6");
    Variable v0 = new Variable("v0");
    Variable v1 = new Variable("v1");
    Variable v2 = new Variable("v2");
    Variable v3 = new Variable("v3");
    Variable v4 = new Variable("v4");
    Variable v5 = new Variable("v5");
    
    public class DummySSAInstruction extends SSAInstruction {
        private int def;
        private int uses[];
        public DummySSAInstruction(int def, int uses[]) {
            this.def = def;
            this.uses = uses;
        }
        public DummySSAInstruction(int def, int use1) {
            this(def, new int[]{use1});
        }
        public DummySSAInstruction(int def, int use1, int use2) {
            this(def, new int[]{use1, use2});
        }
        public SSAInstruction copyForSSA(int[] defs, int[] uses) {
            return new DummySSAInstruction(defs[0], uses);
        }
        public String toString(SymbolTable symbolTable, ValueDecorator d) {
            StringBuffer buff = new StringBuffer();
            buff.append(Integer.toString(def));
            buff.append(" = ");
            for (int i = 0; i < uses.length; i++) {
                buff.append(Integer.toString(uses[i]));
                if (i < uses.length - 1) {
                    buff.append(", ");
                }
            }
            return buff.toString();
        }
        public void visit(IVisitor v) {
        }
        public int hashCode() {
            int sum = def;
            for (int i = 0; i < uses.length; i++) {
                sum += uses[i];
            }
            return sum;
        }
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (!getClass().equals(obj.getClass())) return false;
            DummySSAInstruction ssai = (DummySSAInstruction) obj;
            if (def != ssai.getDef()) return false;
            if (uses.length != ssai.getNumberOfUses()) return false;
            for (int i = 0; i < uses.length; i++) {
                if (uses[i] != ssai.getUse(i)) return false;
            }
            return true;
        }
        public int getDef() {
            return def;
        }
        public int getDef(int i) {
            Assertions._assert(i == 0);
            return def;
        }
        public boolean hasDef() {
            return true;
        }
        public int getNumberOfDefs() {
            return 1;
        }
        public int getUse(int i) {
            return uses[i];
        }
        public int getNumberOfUses() {
            return uses.length;
        }
        public Collection getExceptionTypes() {
            return null;
        }
        public boolean isFallThrough() {
            return false;
        }
    }
}
