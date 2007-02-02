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
package com.ibm.wala.stringAnalysis.ssa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.stringAnalysis.util.*;

public class SSAInstructionProcessor {
    private List instructions;
    
    private SSAInstructionProcessor() {
        instructions = new ArrayList();
    }
    
    public SSAInstructionProcessor(List instructions) {
        this();
        this.instructions.addAll(instructions);
    }
    
    public SSAInstructionProcessor(SSAInstruction instructions[]) {
        this(SAUtil.list(instructions));
    }
    
    public void eachInstruction(SSAInstruction.IVisitor visitor) {
        for (Iterator i = instructions.iterator(); i.hasNext(); ) {
            SSAInstruction instruction = (SSAInstruction) i.next();
            if (instruction != null) {
                instruction.visit(visitor);
            }
        }
    }

    static public void eachInstruction(SSAInstruction instructions[], SSAInstruction.IVisitor visitor) {
        (new SSAInstructionProcessor(instructions)).eachInstruction(visitor);
    }

    static public void eachInstruction(List instructions, SSAInstruction.IVisitor visitor) {
        (new SSAInstructionProcessor(instructions)).eachInstruction(visitor);
    }
}
