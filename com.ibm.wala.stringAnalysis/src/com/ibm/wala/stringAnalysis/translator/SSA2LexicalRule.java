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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.ibm.wala.automaton.grammar.string.DeepGrammarCopier;
import com.ibm.wala.automaton.grammar.string.DeepRuleCopier;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.string.DeepSymbolCopier;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.stringAnalysis.grammar.GR;
import com.ibm.wala.stringAnalysis.grammar.GRule;
import com.ibm.wala.stringAnalysis.grammar.LexicalVariable;

public class SSA2LexicalRule extends SSA2RuleFilter {
  private Map<ISymbol,LexicalVariable> lexicalMap;
  
  public SSA2LexicalRule(ISSA2Rule ssa2rule) {
    super(ssa2rule);
    this.lexicalMap = new HashMap<ISymbol,LexicalVariable>();
  }
  
  protected void translateAstLexicalRead(AstLexicalRead instruction, TranslationContext ctx, Collection rules) {
    AstLexicalAccess.Access accesses[] = instruction.getAccesses();
    for (int i = 0; i < accesses.length; i++) {
      IVariable left = (IVariable) getValueSymbol(accesses[i].valueNumber, instruction, ctx);
      // TODO: the lexical variable is identified by its name and definer's name at this moment.
      String name = accesses[i].variableName;
      if (accesses[i].variableDefiner != null) {
        name = name + "@" + accesses[i].variableDefiner;
      }
      LexicalVariable right = new LexicalVariable(name);
      lexicalMap.put(left, right);
    }
  }

  protected void translateAstLexicalWrite(AstLexicalWrite instruction, TranslationContext ctx, Collection rules) {
    AstLexicalAccess.Access accesses[] = instruction.getAccesses();
    for (int i = 0; i < accesses.length; i++) {
      String name = accesses[i].variableName;
      if (accesses[i].variableDefiner != null) {
        name = name + "@" + accesses[i].variableDefiner;
      }
      LexicalVariable left = new LexicalVariable(name);
      ISymbol right = getValueSymbol(accesses[i].valueNumber, instruction, ctx);
      if (lexicalMap.containsKey(right)) {
        LexicalVariable prevLeft = lexicalMap.get(right);
        rules.add(new GRule(ctx.getIR(), instruction, left, new ISymbol[]{prevLeft}));
        //rules.add(new GRule(ctx.getIR(), instruction, prevLeft, new ISymbol[]{left}));
      }
      else {
        lexicalMap.put(right, left);
        rules.add(new GRule(ctx.getIR(), instruction, left, new ISymbol[]{right}));
      }
    }
  }
  
  public Collection<IProductionRule> translate(SSAInstruction instruction, TranslationContext ctx) {
    if (instruction instanceof AstLexicalRead) {
      Collection<IProductionRule> rules = new HashSet<IProductionRule>();
      translateAstLexicalRead((AstLexicalRead)instruction, ctx, rules);
      return rules;
    }
    else if (instruction instanceof AstLexicalWrite) {
      Collection<IProductionRule> rules = new HashSet<IProductionRule>();
      translateAstLexicalWrite((AstLexicalWrite)instruction, ctx, rules);
      return rules;
    }
    else {
      return super.translate(instruction, ctx);
    }
  }

  public GR postTranslate(GR gr) {
    gr = super.postTranslate(gr);
    gr = (GR) gr.copy(new DeepGrammarCopier(new DeepRuleCopier(new DeepSymbolCopier(){
      public ISymbol copy(ISymbol s) {
        //System.err.println(s);
        if (lexicalMap.containsKey(s)) {
          System.err.println(s + " -> " + lexicalMap.get(s));
          return lexicalMap.get(s);
        }
        else {
          return super.copy(s);
        }
      }
    })));
    return gr;
  }
}
