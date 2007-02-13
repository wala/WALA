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

import com.ibm.wala.automaton.grammar.string.CFLReachability;
import com.ibm.wala.automaton.grammar.string.Grammars;
import com.ibm.wala.automaton.grammar.string.IContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.ISimplify;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.stringAnalysis.grammar.GR;
import com.ibm.wala.stringAnalysis.grammar.LexicalVariable;
import com.ibm.wala.stringAnalysis.js.translator.JSFunctionNameResolver;
import com.ibm.wala.stringAnalysis.js.translator.JSSSA2Rule;
import com.ibm.wala.stringAnalysis.js.translator.JSTranslatorRepository;
import com.ibm.wala.stringAnalysis.translator.BB2GR;
import com.ibm.wala.stringAnalysis.translator.CG2GR;
import com.ibm.wala.stringAnalysis.translator.FunctionNameCalleeResolver;
import com.ibm.wala.stringAnalysis.translator.GR2CFG;
import com.ibm.wala.stringAnalysis.translator.IR2GR;
import com.ibm.wala.stringAnalysis.translator.ISSA2Rule;
import com.ibm.wala.stringAnalysis.util.SAUtil;
import com.ibm.wala.util.debug.Trace;

public class TestGR2CFG extends TestJSTranslatorBase {
  private GR gr;
  private GR2CFG gr2cfg;

  public void setUp() throws Exception {
    super.setUp();
    ISSA2Rule ssa2rule = createSSA2Rule();
    BB2GR bb2gr = new BB2GR(ssa2rule);
    IR2GR ir2gr = new IR2GR(bb2gr);
    CG2GR cg2gr = new CG2GR(ir2gr, new FunctionNameCalleeResolver(new JSFunctionNameResolver()));
    gr2cfg = new GR2CFG(new JSTranslatorRepository());
    ISimplify g = cg2gr.translate(getCallGraphBuilder());
    assertTrue(g instanceof GR);
    gr = (GR) g;
  }

  protected ISSA2Rule createSSA2Rule() {
    return new JSSSA2Rule();
  }

  protected IContextFreeGrammar verifyCFG(IVariable v, String pattern) {
    return verifyCFG(v, pattern(pattern));
  }

  protected IContextFreeGrammar verifyCFG(IVariable v, IAutomaton pattern) {
    IContextFreeGrammar cfg = gr2cfg.solve(gr, v);
    Trace.println("-- context-free grammar for " + v + ": ");
    Trace.println(SAUtil.prettyFormat(cfg));
    Trace.println("-- context-free grammar for " + v + ": ");
    Grammars.normalize(cfg, null);
    Trace.println(SAUtil.prettyFormat(cfg));
    super.verifyCFG(pattern, cfg);
    return cfg;
  }

  public void testConcat() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("r1"), "strAstrB");
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA" + "strB")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strA" + "strB" + "strB")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strA")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("strAstrB")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA(strB)*")));
  }


  public void testConditionalBranch() {
    IVariable r2 = new LexicalVariable("r2");
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("r2"), "astrA|bstrB");
    assertTrue(CFLReachability.containsSome(cfg, pattern("a" + "strA")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("b" + "strB")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("a" + "strB")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("b" + "strA")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("astrA|bstrB")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("(a|b)(strA|strB)")));
  }


  public void testForLoop() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("r3"), "strA(strB)*");
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA" + "strB")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA" + "strB" + "strB")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strB")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA(strB)*")));
  }

  public void testWhileLoop() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("r4"), "strA(strB)*");
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA" + "strB")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA" + "strB" + "strB")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strB")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA(strB)*")));
  }

  public void testRecursiveFunction() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("r5"), "a*strAb*");
    assertTrue(CFLReachability.containsSome(cfg, pattern("strA")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("astrAb")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("aastrAbb")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strAa")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("bstrA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("bstrAa")));
  }

  public void testArray1() {
    IContextFreeGrammar cfg2 = verifyCFG(new LexicalVariable("r11"), "ARY|strA|strB");
    assertTrue(CFLReachability.containsSome(cfg2, pattern("ARY")));
    assertTrue(CFLReachability.containsSome(cfg2, pattern("strA")));
    assertTrue(CFLReachability.containsSome(cfg2, pattern("strB")));
  }

  public void testAssocArray1() {
    //TODO:
    //assertTrue(CFLReachability.containsAll(pattern("strA|strB|ASSOC"), cfg));
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("r21"), "ASSOC");
  }

  public void testSubstring1() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rSubstr1"), "str");
    assertTrue(CFLReachability.containsSome(cfg, pattern("str")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strA")));
  }

  public void testSubstring2() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rSubstr2"), "str|B");
    assertTrue(CFLReachability.containsSome(cfg, pattern("str")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("B")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strB")));
    assertTrue(CFLReachability.containsSome(cfg, pattern("str|B")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strA|strB")));
  }

  public void testSubstr3() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rSubstr3"), "trA");
    assertTrue(CFLReachability.containsSome(cfg, pattern("trA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("str")));
  }

  public void testToUpperCase1() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rToUpperCase1"), "STRA");
    assertTrue(CFLReachability.containsSome(cfg, pattern("STRA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("stra")));
  }

  public void testToLowerCase1() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rToLowerCase1"), "stra");
    assertTrue(CFLReachability.containsSome(cfg, pattern("stra")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("STRA")));
  }

  public void testToLocaleUpperCase1() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rToLocaleUpperCase1"), "STRA");
    assertTrue(CFLReachability.containsSome(cfg, pattern("STRA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("stra")));
  }

  public void testToLocaleLowerCase1() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rToLocaleLowerCase1"), "stra");
    assertTrue(CFLReachability.containsSome(cfg, pattern("stra")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("strA")));
    assertFalse(CFLReachability.containsSome(cfg, pattern("STRA")));
  }

  public void testConcat1() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rConcat1"), "strA(strB)*");
    assertTrue(CFLReachability.containsSome(cfg, pattern("strAstrBstrBstrB")));
    assertTrue(CFLReachability.containsAll(pattern("strAstrBstrBstrB"), cfg));
  }

  public void testCharAt1() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rCharAt1"), "t");
    assertTrue(CFLReachability.containsSome(cfg, pattern("t")));
  }

  public void testCharAt2() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rCharAt2"), "");
    assertTrue(CFLReachability.containsSome(cfg, pattern("")));
    assertFalse(CFLReachability.containsAll(pattern("."), cfg));
  }

  public void testSplit1() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rSplit1"), "a|b|c|()");
  }

  public void testProp1() {
    /*
        IAutomaton spec =
            Automatons.createUnion(
                pattern("foo"),
                Automatons.createAutomaton(new ISymbol[]{new NumberSymbol(123.0)}));
     */
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rProp1"), pattern("foo"));
  }

  public void testCyclic1() {
    IContextFreeGrammar cfg = verifyCFG(new LexicalVariable("rCyclic1"), "(abcdefg)|(bcd)");
  }
}
