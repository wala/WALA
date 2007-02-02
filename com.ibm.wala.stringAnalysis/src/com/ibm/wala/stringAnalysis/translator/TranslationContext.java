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
package com.ibm.wala.stringAnalysis.translator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.stringAnalysis.util.LocalNameTable;

public class TranslationContext {
    private IR ir;
    private CallSiteReference callSite;
    private CGNode node;
    private PropagationCallGraphBuilder cgbuilder;
    private LocalNameTable ltable;
    
    public TranslationContext(IR ir, CGNode node, CallSiteReference callSite, PropagationCallGraphBuilder cgbuilder){
        this.ir = ir;
        this.node = node;
        this.callSite = callSite;
        this.cgbuilder = cgbuilder;
        this.ltable = new LocalNameTable(ir);
    }
    
    public TranslationContext(TranslationContext ctx) {
        this(ctx.getIR(), ctx.getCGNode(), ctx.getCallSiteReference(), ctx.getCGBuilder());
    }
    
    public IR getIR(){
        return this.ir;
    }
    
    public CGNode getCGNode(){
        return this.node;
    }
    
    public int getInstructionIndex(SSAInstruction instruction) {
      SSAInstruction instructions[] = ir.getInstructions();
      for (int i = 0; i < instructions.length; i++) {
        if (instruction.equals(instructions[i])) {
          return i;
        }
      }
      return -1;
    }
    
    public CallSiteReference getCallSiteReference() {
        return this.callSite;
    }
    
    public PropagationCallGraphBuilder getCGBuilder(){
        return this.cgbuilder;
    }
    
    public LocalNameTable getLocalNameTable() {
        return this.ltable;
    }
}
