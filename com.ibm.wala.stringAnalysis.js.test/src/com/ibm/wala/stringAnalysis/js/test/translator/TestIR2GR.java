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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.automaton.grammar.string.IGrammar;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.stringAnalysis.grammar.GR;
import com.ibm.wala.stringAnalysis.grammar.GRule;
import com.ibm.wala.stringAnalysis.js.translator.JSSSA2Rule;
import com.ibm.wala.stringAnalysis.translator.BB2GR;
import com.ibm.wala.stringAnalysis.translator.IR2GR;
import com.ibm.wala.stringAnalysis.translator.SSA2Rule;
import com.ibm.wala.stringAnalysis.translator.TranslationContext;
import com.ibm.wala.stringAnalysis.util.SAUtil;

import junit.framework.TestCase;

public class TestIR2GR extends TestJSTranslatorBase {
    public void testTranslate() {
        for (ListIterator i = getIRs().listIterator(); i.hasNext(); ) {
            int idx = i.nextIndex();
            IR ir = (IR) i.next();
            CGNode node = (CGNode) getCGNodes().get(idx);
            TranslationContext ctx = new TranslationContext(ir, node, null, getCallGraphBuilder());
            SSA2Rule ssa2rule = new JSSSA2Rule();
            BB2GR bb2gr = new BB2GR(ssa2rule);
            IR2GR ir2gr = new IR2GR(bb2gr);
            IGrammar g = ir2gr.translate(ctx);
            assertTrue(g instanceof GR);
            GR gr = (GR) g;
            SSAInstruction instructions[] = ir.getInstructions();

            Trace.println("--- IR:");
            Trace.println(ir);
            Trace.println("");
            Trace.println("--- GR:");
            Trace.println(SAUtil.prettyFormat(gr));

            int ni = 0;
            for (int j = 0; j<instructions.length; j++) {
                if (instructions[j] != null) {
                    Collection translated = ssa2rule.translate(instructions[j], ctx);
                    ni += translated.size();
                    int def = instructions[j].getDef();
                    List rules = new ArrayList(gr.getRules(new Variable("v" + def)));
                    for (Iterator k = rules.iterator(); k.hasNext(); ) {
                        IProductionRule rule = (IProductionRule) k.next();
                        assertTrue(rule instanceof GRule);
                        GRule grule = (GRule) rule;
                        assertEquals(instructions[j], grule.getSSAInstruction());
                    }
                }
            }
            int nrp = 0;
            int nip = 0;
            int nr = g.getRules().size();
            for (Iterator j = gr.getRules().iterator(); j.hasNext(); ) {
              GRule grule = (GRule) j.next();
              if (grule.getSSAInstruction() instanceof SSAPhiInstruction) {
                nrp ++;
              }
              else if (grule.getSSAInstruction() instanceof SSAPiInstruction) {
                nrp ++;
              }
            }
            assertEquals(ni, nr - nrp);

            for (Iterator j = ir.iteratePhis(); j.hasNext(); ) {
              SSAPhiInstruction phi = (SSAPhiInstruction) j.next();
              Set<Integer> s = new HashSet<Integer>(); 
              for (int k = 0; k < phi.getNumberOfUses(); k++) {
                s.add(phi.getUse(k));
              }
              nip += s.size();
            }
            for (Iterator j = ir.iteratePis(); j.hasNext(); ) {
              SSAPiInstruction pi = (SSAPiInstruction) j.next();
              Set<Integer> s = new HashSet<Integer>(); 
              for (int k = 0; k < pi.getNumberOfUses(); k++) {
                s.add(pi.getUse(k));
              }
              nip += s.size();
            }
            assertEquals(ni + nip, nr);
        }
    }
}
