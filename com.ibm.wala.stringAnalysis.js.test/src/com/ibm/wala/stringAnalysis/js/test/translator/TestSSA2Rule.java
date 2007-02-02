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

import java.util.*;

import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.FakeRootClass;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ssa.*;
import com.ibm.wala.stringAnalysis.grammar.InvocationSymbol;
import com.ibm.wala.stringAnalysis.js.ssa.SAJSProcessingInstructionVisitor;
import com.ibm.wala.stringAnalysis.js.translator.JSSSA2Rule;
import com.ibm.wala.stringAnalysis.ssa.*;
import com.ibm.wala.stringAnalysis.translator.SSA2Rule;
import com.ibm.wala.stringAnalysis.translator.TranslationContext;

public class TestSSA2Rule extends TestJSTranslatorBase {
    public IProductionRule firstRule(Collection rules) {
        return (IProductionRule) (new ArrayList(rules)).get(0);
    }
    
    public void testTranslate() {
        final PropagationCallGraphBuilder cgbuilder = getCallGraphBuilder();
        final SSA2Rule s2r = new JSSSA2Rule();
        for (ListIterator i = getIRs().listIterator(); i.hasNext(); ) {
            int idx = i.nextIndex();
            final IR ir = (IR) i.next();
            final CGNode node = (CGNode) getCGNodes().get(idx);
            final TranslationContext ctx = new TranslationContext(ir, node, null, cgbuilder);
            SSAInstructionProcessor.eachInstruction(
          ir.getInstructions(), 
          new SAJSProcessingInstructionVisitor(
            new SAJSProcessingInstructionVisitor.Processor() {
                  public void onUnsupportedInstruction(SSAInstruction instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertEquals(rules.size(), 0);
          }

          public void onSSAAbstractInvokeInstruction(SSAAbstractInvokeInstruction instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    if (instruction.getDeclaredTarget().getDeclaringClass().equals(FakeRootClass.FAKE_ROOT_CLASS)) {
                      assertEquals(rules.size(), 0);
                    }
                    else {
                      assertEquals(rules.size(), 1);
                      IProductionRule rule = firstRule(rules);
                      Trace.println(rule);
                      assertTrue(rule.getRight(0) instanceof InvocationSymbol);
                      InvocationSymbol isym = (InvocationSymbol) rule.getRight(0);
                      assertEquals(instruction.getNumberOfParameters()-1, isym.getParameters().size());
                    }
          }

          public void onSSABinaryOpInstruction(SSABinaryOpInstruction instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertEquals(rules.size(), 1);
                    
                    IProductionRule rule = firstRule(rules);
                    Trace.println(rule);
                    assertEquals(1, rule.getRight().size());
                    assertTrue(rule.getRight(0) instanceof InvocationSymbol);
                    InvocationSymbol isym = (InvocationSymbol) rule.getRight(0);
                    assertEquals(2, isym.getParameters().size());
          }
            
                  public void onSSAAbstractUnaryInstruction(SSAAbstractUnaryInstruction instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertEquals(rules.size(), 1);
                    
                    IProductionRule rule = firstRule(rules);
                    Trace.println(rule);
                    assertEquals(1, rule.getRight().size());
                    assertTrue(rule.getRight(0) instanceof InvocationSymbol);
                    InvocationSymbol isym = (InvocationSymbol) rule.getRight(0);
                    assertEquals(1, isym.getParameters().size());
          }
                
          public void onJavaScriptPropertyRead(JavaScriptPropertyRead instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertTrue(rules.size() > 1);
          }

          public void onSSAGetInstruction(SSAGetInstruction instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertTrue(rules.size() > 1);
          }

          public void onJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertEquals(2, rules.size());

                    IProductionRule rule = firstRule(rules);
                    Trace.println(rule);
                    assertEquals(1, rule.getRight().size());
          }

          public void onSSAPutInstruction(SSAPutInstruction instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertEquals(2, rules.size());

                    IProductionRule rule = firstRule(rules);
                    Trace.println(rule);
                    assertEquals(1, rule.getRight().size());
          }

          public void onSSANewInstruction(SSANewInstruction instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertEquals(rules.size(), 0);
                    
                    // TODO: how do we handle the "NEW" instruction?
                    // Currently, it is considered as an empty set.
                    //IProductionRule rule = firstRule(rules);
                    //Trace.println(rule);
                    //assertEquals(0, rule.getRight().size());
          }

          public void onSSAPhiInstruction(SSAPhiInstruction instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertEquals(rules.size(), 2);

                    IProductionRule rule = firstRule(rules);
                    Trace.println(rule);
                    assertEquals(2, rule.getRight().size());
          }

          public void onSSAConditionalBranchInstruction(SSAConditionalBranchInstruction instruction) {
                    onUnsupportedInstruction(instruction);
          }

          public void onSSAReturnInstruction(SSAReturnInstruction instruction) {
                    onUnsupportedInstruction(instruction);
          }

          public void onAstLexicalRead(AstLexicalRead instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertEquals(rules.size(), instruction.getAccessCount());
          }

          public void onAstLexicalWrite(AstLexicalWrite instruction) {
                    Collection rules = s2r.translate(instruction, ctx);
                    assertEquals(rules.size(), instruction.getAccessCount());
          }
        }));
        }
    }
}
