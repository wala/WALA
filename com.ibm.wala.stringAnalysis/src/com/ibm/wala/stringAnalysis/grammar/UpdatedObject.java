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
package com.ibm.wala.stringAnalysis.grammar;

import com.ibm.wala.automaton.string.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.stringAnalysis.util.*;

public class UpdatedObject extends VariableWrapper implements IVariable {
    private IR ir;
    private int pc;
    
    public UpdatedObject(IVariable v, IR ir, int pc) {
        super(v);
        this.ir = ir;
        this.pc = pc;
    }

    public UpdatedObject(IVariable v, IR ir) {
        super(v);
        this.ir = ir;
        this.pc = -1;
    }
    
    public UpdatedObject(IVariable v, IR ir, SSAInstruction instruction) {
        super(v);
        this.ir = ir;
        this.pc = SAUtil.Domo.findPC(this.ir,instruction);
    }
    
    public void setIR(IR ir) {
        this.ir = ir;
    }
    
    public void setPC(int pc) {
        this.pc = pc;
    }
    
    public IR getIR() {
        return ir;
    }
    
    public int getPC() {
        return pc;
    }
    
    public int hashCode() {
        return super.hashCode()
            + ((ir == null) ? 0 : ir.hashCode())
            + pc;
    }
    
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            UpdatedObject uo = (UpdatedObject) obj;
            if (ir == null) {
                return (uo.ir == null) && (pc == uo.pc);
            }
            else {
                return ir.equals(uo.ir) && (pc == uo.pc);
            }
        }
        else {
            return false;
        }
    }
    
    public String toString() {
        return super.toString() + "_" + pc;
    }

    public String getName() {
        return super.getName() + "_" + pc;
    }

    public boolean matches(ISymbol symbol, IMatchContext context) {
        if (!getClass().equals(symbol.getClass())) return false;
        if (this == symbol) return true;
        UpdatedObject uo = (UpdatedObject) symbol;
        if (super.matches(uo, context)) {
            if (ir == null) {
                return (uo.ir == null) && (pc == uo.pc);
            }
            else {
                return ir.equals(uo.ir) && (pc == uo.pc);
            }
        }
        else {
            return false;
        }
    }
}
