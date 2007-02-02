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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.stringAnalysis.grammar.*;
import com.ibm.wala.stringAnalysis.ssa.*;
import com.ibm.wala.stringAnalysis.translator.SSA2Rule.BaseTranslatingProcessor;
import com.ibm.wala.stringAnalysis.util.*;
import com.ibm.wala.types.*;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;

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
