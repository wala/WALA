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
package com.ibm.wala.automaton.test.regex.string;

import com.ibm.wala.automaton.parser.*;
import com.ibm.wala.automaton.regex.string.IPattern;
import com.ibm.wala.automaton.regex.string.StringPatternCompiler;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.test.AutomatonJunitBase;

public class TestPatternCompiler extends AutomatonJunitBase {
  private AmtParser parser;
  private StringPatternCompiler stringPatternCompiler;

  public void setUp() {
    parser = new AmtParser();
    stringPatternCompiler = new StringPatternCompiler();
  }

  public void testCompileAnyChar1() {
    IPattern pat = (IPattern) parser.parse("/./").get(new Variable("_"));
    IAutomaton a = stringPatternCompiler.compile(pat);
    assertTrue(a.accept(StringSymbol.toCharSymbols("\n")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("b")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("ab")));
  }

  public void testCompileConcatenation1() {
    IPattern pat = (IPattern) parser.parse("/abc/").get(new Variable("_"));
    IAutomaton a = stringPatternCompiler.compile(pat);
    assertTrue(a.accept(StringSymbol.toCharSymbols("abc")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("abcd")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("ab")));
  }

  public void testCompileUnion1() {
    IPattern pat = (IPattern) parser.parse("/(ab)|(bc)/").get(new Variable("_"));
    IAutomaton a = stringPatternCompiler.compile(pat);
    assertTrue(a.accept(StringSymbol.toCharSymbols("ab")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("bc")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("abc")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("a")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("bcd")));
  }

  public void testCompileUnion2() {
    IPattern pat = (IPattern) parser.parse("/ab?/").get(new Variable("_"));
    IAutomaton a = stringPatternCompiler.compile(pat);
    assertTrue(a.accept(StringSymbol.toCharSymbols("ab")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("a")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("abc")));
  }

  public void testCompileIteration1() {
    IPattern pat = (IPattern) parser.parse("/(ab)+/").get(new Variable("_"));
    IAutomaton a = stringPatternCompiler.compile(pat);
    assertTrue(a.accept(StringSymbol.toCharSymbols("ab")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("abab")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("aba")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("abb")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("")));
  }

  public void testCompileComplement1() {
    IPattern pat = (IPattern) parser.parse("/~(ab)/").get(new Variable("_"));
    IAutomaton a = stringPatternCompiler.compile(pat);
    assertFalse(a.accept(StringSymbol.toCharSymbols("ab")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("abab")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("aba")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("abb")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("")));
  }

  public void testCompileComplement2() {
    IPattern pat = (IPattern) parser.parse("/(~a)*/").get(new Variable("_"));
    IAutomaton a = stringPatternCompiler.compile(pat);
    assertFalse(a.accept(StringSymbol.toCharSymbols("a")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("b")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("aa")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("ab")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("bb")));
  }
  public void testCompileComplement3() {
    IPattern pat = (IPattern) parser.parse("/[^a]*/").get(new Variable("_"));
    IAutomaton a = stringPatternCompiler.compile(pat);
    assertFalse(a.accept(StringSymbol.toCharSymbols("a")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("b")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("aa")));
    assertFalse(a.accept(StringSymbol.toCharSymbols("ab")));
    assertTrue(a.accept(StringSymbol.toCharSymbols("bb")));
  }
}
