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

import java.util.Collection;

import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.stringAnalysis.grammar.GR;

public class SSA2RuleFilter implements ISSA2Rule {
  private ISSA2Rule ssa2rule;
  
  public SSA2RuleFilter(ISSA2Rule ssa2rule) {
    this.ssa2rule = ssa2rule;
  }

  public IProductionRule createRule(IR ir, SSAInstruction instruction,
                                    IVariable left, ISymbol[] right) {
    return ssa2rule.createRule(ir, instruction, left, right);
  }

  public ISymbol getDefaultParameterValueSymbol() {
    return ssa2rule.getDefaultParameterValueSymbol();
  }

  public ISymbol getValueSymbol(int v, SSAInstruction instruction, TranslationContext ctx) {
    return ssa2rule.getValueSymbol(v, instruction, ctx);
  }

  public GR postTranslate(GR gr) {
    return ssa2rule.postTranslate(gr);
  }

  public Collection<IProductionRule> translate(SSAInstruction instruction, TranslationContext ctx){
    return ssa2rule.translate(instruction, ctx);
  }
}
