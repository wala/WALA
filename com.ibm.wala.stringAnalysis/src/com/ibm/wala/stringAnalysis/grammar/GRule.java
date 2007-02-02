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

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.ssa.*;

public class GRule extends ProductionRule {
    private IR ir;
    private SSAInstruction ssai;
    
    public GRule(IR ir, SSAInstruction ssai, IVariable left, List right) {
        super(left, right);
        this.ir = ir;
        this.ssai = ssai;
    }
    
    public GRule(IR ir, SSAInstruction ssai, IVariable left, ISymbol right[]) {
        super(left, right);
        this.ir = ir;
        this.ssai = ssai;
    }
    
    public GRule(GRule instruction) {
        super(instruction);
        this.ir = instruction.getIR();
        this.ssai = instruction.getSSAInstruction();
    }
    
    public SSAInstruction getSSAInstruction() {
        return ssai;
    }
    
    public IR getIR() {
        return ir;
    }

    public int hashCode() {
        return super.hashCode() + (ssai==null ? 0 : ssai.hashCode());
    }
    
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            GRule gi = (GRule) obj;
            return ((ssai==null) ? (gi.getSSAInstruction()==null) : ssai.equals(gi.getSSAInstruction()));
        }
        else {
            return false;
        }
    }
    
    public String toString() {
        return "{ir:" + ir.getMethod().getName() + ", ssai:" + (ssai==null ? "null" : ssai.toString()) + ", rule:" + super.toString() + "}";
    }
}
