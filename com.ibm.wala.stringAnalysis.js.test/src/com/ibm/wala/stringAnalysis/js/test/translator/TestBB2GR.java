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
package com.ibm.wala.stringAnalysis.js.test.translator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.*;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.stringAnalysis.js.translator.JSSSA2Rule;
import com.ibm.wala.stringAnalysis.translator.*;

public class TestBB2GR extends TestJSTranslatorBase {
    private Set getInstructions(TranslationContext ctx, BasicBlock bb) {
        Set l = new HashSet();
        SSAInstruction instructions[] = ctx.getIR().getInstructions();
        int first = bb.getFirstInstructionIndex();
        int last  = bb.getLastInstructionIndex();
        for (int i = first; i <= last; i++) {
            l.add(instructions[i]);
        }
        for (Iterator i = bb.iteratePhis(); i.hasNext(); ) {
            SSAPhiInstruction phi = (SSAPhiInstruction) i.next();
            l.add(phi);
        }
        for (Iterator i = bb.iteratePis(); i.hasNext(); ) {
            SSAPiInstruction pi = (SSAPiInstruction) i.next();
            l.add(pi);
        }
        return l;
    }
    
    private int getNumberOfRules(TranslationContext ctx, BasicBlock bb, ISSA2Rule ssa2rule) {
        Set l = getInstructions(ctx, bb);
        return getNumberOfRules(ctx, l, ssa2rule);
    }
    
    private int getNumberOfRules(TranslationContext ctx, Set instructions, ISSA2Rule ssa2rule) {
        int n = 0;
        for (Iterator i = instructions.iterator(); i.hasNext(); ) {
            SSAInstruction instruction = (SSAInstruction) i.next();
            n = n + ssa2rule.translate(instruction, ctx).size();
        }
        return n;
    }
    
    public void testTranslate() {
        for (ListIterator i = getIRs().listIterator(); i.hasNext(); ) {
            int idx = i.nextIndex();
            IR ir = (IR) i.next();
            CGNode node = (CGNode) getCGNodes().get(idx);
            SSA2Rule ssa2rule = new JSSSA2Rule();
            BB2GR bb2gr = new BB2GR(ssa2rule);
            SSACFG cfg = ir.getControlFlowGraph();
            TranslationContext ctx = new TranslationContext(ir, node, null, getCallGraphBuilder());
            for (Iterator n = cfg.iterateNodes(); n.hasNext(); ) {
                IBasicBlock bb = (IBasicBlock) n.next();
                IGrammar gr = bb2gr.translate(bb, ctx);
                assertNotNull(gr);
                int nr = getNumberOfRules(ctx, (BasicBlock)bb, ssa2rule);
                Set rules = gr.getRules();
                assertEquals(nr, rules.size());
            }
        }
    }
}
