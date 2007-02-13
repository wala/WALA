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
package com.ibm.wala.automaton.test.parser;

import java.util.*;

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.grammar.tree.*;
import com.ibm.wala.automaton.parser.AmtParser;
import com.ibm.wala.automaton.regex.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.tree.*;


import junit.framework.TestCase;

public class TestAmtParser extends TestCase {
    private AmtParser parser;
    
    public void setUp() {
        parser = new AmtParser();
        parser.parse("search com.ibm.wala.automaton.string");
    }
    
    public void verifyVariable(IVariable v, Object obj) {
        assertEquals(obj, parser.getResult().get(v));
    }

    public void verifyVariable(String v, Object obj) {
        verifyVariable(new Variable(v), obj);
    }
    
    public void testSymbol() {
        parser.parse("$x = a");
        verifyVariable("x", new StringSymbol("a"));
    }
    
    public void testVariableRef() {
        parser.parse("$x = a; $y = $x");
        verifyVariable("x", new StringSymbol("a"));
    }

    public void testVariable() {
        parser.parse("$x = a; $y = $$x");
        verifyVariable("y", new Variable("x"));
    }

    public void testPrefixedSymbol() {
        parser.parse("$x = prefix:foo");
        verifyVariable("x", new PrefixedSymbol(new StringSymbol("prefix"), new StringSymbol("foo")));
    }
    
    public void testInstantiation() {
        parser.parse("$x = Symbol(\"a\")");
        verifyVariable("x", new Symbol("a"));
    }

    public void testInstantiationWithAlias() {
        parser.parse("alias S = StringSymbol");
        parser.parse("$x = S(\"a\")");
        parser.parse("alias S = NumberSymbol");
        parser.parse("$n = S(123)");
        parser.parse("delete alias S");
        verifyVariable("x", new StringSymbol("a"));
        verifyVariable("n", new NumberSymbol(123));
    }

    public void testInstantiationWithProps() {
        parser.parse("$x = FlexibleNamingSymbol(\"a\"){ name = \"b\" }");
        verifyVariable("x", new FlexibleNamingSymbol("b"));
    }
    
    public void testInteger() {
        parser.parse("$x = -21");
        verifyVariable("x", new Long(-21));
    }
    
    public void testDouble() {
        parser.parse("$x = -21.0");
        verifyVariable("x", new Double(-21.0));
    }
    
    public void testList() {
        parser.parse("$x = [\"a\", \"b\", \"c\"]");
        verifyVariable("x", AUtil.list(new String[]{"a","b","c"}));
    }

    public void testSet() {
        parser.parse("$x = {\"a\", \"b\", \"c\"}");
        verifyVariable("x", AUtil.set(new String[]{"a","b","c"}));
    }
    
    public void testTree() {
        parser.parse("$t = t1[t11[],t12[]]");
        verifyVariable("t", new Tree("t1", new Tree[]{new Tree("t11"), new Tree("t12")}));
    }

    public void testTree2() {
        parser.parse("$t = t1[t11,t12]");
        verifyVariable("t", new Tree("t1", new Tree[]{new Tree("t11"), new Tree("t12")}));
    }
    
    public void testBTree() {
        parser.parse("$t = t1[t11[;];t12[;]]");
        verifyVariable("t", new BinaryTree("t1", new BinaryTree("t11"), new BinaryTree("t12")));
    }

    public void testBTree2() {
        parser.parse("$t = t1[t11;t12]");
        verifyVariable("t", new BinaryTree("t1", new BinaryTree("t11"), new BinaryTree("t12")));
    }

    public void testBTree3() {
        parser.parse("$t = t1[t11[null;null];t12[null;null]]");
        verifyVariable("t", new BinaryTree("t1", new BinaryTree("t11"), new BinaryTree("t12")));
    }
    
    public void testConcatenationPattern() {
        parser.parse("$p = /abc/");
        verifyVariable(
                "p",
                new ConcatenationPattern(
                        new SymbolPattern(new CharSymbol("a")),
                        new ConcatenationPattern(
                                new SymbolPattern(new CharSymbol("b")),
                                new SymbolPattern(new CharSymbol("c")))));
    }
    
    public void testUnionPattern() {
        parser.parse("$p = /a|b/");
        verifyVariable(
            "p",
            new UnionPattern(
                new SymbolPattern(new CharSymbol("a")),
                new SymbolPattern(new CharSymbol("b"))));
    }
    
    public void testIterationPattern() {
        parser.parse("$p = /ab*/");
        verifyVariable(
                "p",
                new ConcatenationPattern(
                    new SymbolPattern(new CharSymbol("a")),
                    new IterationPattern(
                        new SymbolPattern(new CharSymbol("b")), true)));
    }

    public void testIterationPattern2() {
        parser.parse("$p = /ab+/");
        verifyVariable(
                "p",
                new ConcatenationPattern(
                    new SymbolPattern(new CharSymbol("a")),
                    new IterationPattern(
                        new SymbolPattern(new CharSymbol("b")), false)));
    }
    
    public void testIntersectionPattern1() {
        parser.parse("$p = /(a|A)(b|B)&ab/");
        IPattern a = new SymbolPattern(new CharSymbol("a"));
        IPattern b = new SymbolPattern(new CharSymbol("b"));
        IPattern A = new SymbolPattern(new CharSymbol("A"));
        IPattern B = new SymbolPattern(new CharSymbol("B"));
        IPattern aA = new VariableBindingPattern(new UnionPattern(a, A), new Variable("1"));
        IPattern bB = new VariableBindingPattern(new UnionPattern(b, B), new Variable("2"));
        IPattern aAbB = new ConcatenationPattern(aA, bB);
        IPattern ab = new ConcatenationPattern(a, b);
        IPattern expected = new IntersectionPattern(aAbB, ab);
        verifyVariable("p", expected);
    }
    
    public void testSubtractionPattern1() {
        parser.parse("$p = /(a|A)(b|B)-ab/");
        IPattern a = new SymbolPattern(new CharSymbol("a"));
        IPattern b = new SymbolPattern(new CharSymbol("b"));
        IPattern A = new SymbolPattern(new CharSymbol("A"));
        IPattern B = new SymbolPattern(new CharSymbol("B"));
        IPattern aA = new VariableBindingPattern(new UnionPattern(a, A), new Variable("1"));
        IPattern bB = new VariableBindingPattern(new UnionPattern(b, B), new Variable("2"));
        IPattern aAbB = new ConcatenationPattern(aA, bB);
        IPattern ab = new ConcatenationPattern(a, b);
        IPattern expected = new IntersectionPattern(aAbB, new ComplementPattern(ab));
        verifyVariable("p", expected);
    }
    
    public void testEmptyPattern1() {
        parser.parse("$p = /ab?/");
        verifyVariable(
                "p",
                new ConcatenationPattern(
                        new SymbolPattern(new CharSymbol("a")),
                        new UnionPattern(
                                new SymbolPattern(new CharSymbol("b")),
                                new EmptyPattern())));
    }
    
    public void testEmptyPattern2() {
        parser.parse("$p = /()/");
        verifyVariable("p", new VariableBindingPattern(new EmptyPattern(), new Variable("1")));
    }
    
    public void testAnyCharPattern1() {
      parser.parse("$p = /./");
      verifyVariable("p", new SymbolPattern(new CharPatternSymbol("\\.")));
    }
    
    public void testAnyCharPattern2() {
      parser.parse("$p = /\\./");
      verifyVariable("p", new SymbolPattern(new CharSymbol(".")));
    }
    
    public void testSpecialWord() {
        parser.parse("$p = /\\(\\)/");
        verifyVariable(
                "p",
                new ConcatenationPattern(
                        new SymbolPattern(new CharSymbol("(")),
                        new SymbolPattern(new CharSymbol(")"))));
    }
    
    public void testSpecialWord2() {
      parser.parse("$p = /\\a/");
      verifyVariable("p", new SymbolPattern(new CharPatternSymbol("\\a")));
    }
    
    public void testSpecialWord3() {
      parser.parse("$p = /\\\\\\a/");
      verifyVariable(
        "p",
        new ConcatenationPattern(
          new SymbolPattern(new CharSymbol("\\")),
          new SymbolPattern(new CharPatternSymbol("\\a"))
        )
      );
    }
    
    public void testSpecialWord4() {
      parser.parse("$p = /\\\\a/");
      verifyVariable(
        "p",
        new ConcatenationPattern(
          new SymbolPattern(new CharSymbol("\\")),
          new SymbolPattern(new CharSymbol("a"))
        )
      );
    }
    
    public void testSpecialWord5() {
      parser.parse("$p = /a\\//");
      verifyVariable(
        "p",
        new ConcatenationPattern(
          new SymbolPattern(new CharSymbol("a")),
          new SymbolPattern(new CharSymbol("/"))
        )
      );
    }
    
    public void testSpecialWord6() {
      parser.parse("$p = /a\\&/");
      verifyVariable(
        "p",
        new ConcatenationPattern(
          new SymbolPattern(new CharSymbol("a")),
          new SymbolPattern(new CharSymbol("&"))
        )
      );
    }
    
    public void testComplexPattern() {
        parser.parse("$p = /a(b(c))/");
        IPattern expect = new SymbolPattern(new CharSymbol("c"));
        expect = new VariableBindingPattern(expect, new Variable("2"));
        expect = new ConcatenationPattern(new SymbolPattern(new CharSymbol("b")), expect);
        expect = new VariableBindingPattern(expect, new Variable("1"));
        expect = new ConcatenationPattern(new SymbolPattern(new CharSymbol("a")), expect);
        verifyVariable("p", expect);
    }
    
    public void testComplexPattern2() {
        parser.parse("$p = /a(b)(c)/");
        IPattern a = new SymbolPattern(new CharSymbol("a"));
        IPattern b = new VariableBindingPattern(new SymbolPattern(new CharSymbol("b")), new Variable("1"));
        IPattern c = new VariableBindingPattern(new SymbolPattern(new CharSymbol("c")), new Variable("2"));
        IPattern expect = new ConcatenationPattern(a, new ConcatenationPattern(b, c));
        verifyVariable("p", expect);
    }
    
    public void testComplexPattern3() {
        parser.parse("$p = /a(bc|d)/");
        verifyVariable(
                "p",
                new ConcatenationPattern(
                        new SymbolPattern(new CharSymbol("a")),
                        new VariableBindingPattern(
                                new UnionPattern(
                                        new ConcatenationPattern(new SymbolPattern(new CharSymbol("b")), new SymbolPattern(new CharSymbol("c"))),
                                        new SymbolPattern(new CharSymbol("d"))),
                                new Variable("1"))));
    }

    public void testComplexPattern4() {
        parser.parse("$p = /a*b+/");
        verifyVariable(
                "p",
                new ConcatenationPattern(
                        new IterationPattern(new SymbolPattern(new CharSymbol("a")), true),
                        new IterationPattern(new SymbolPattern(new CharSymbol("b")), false)));
    }

    
    public void testComplementPattern1() {
        parser.parse("$p = /~ab/");
        verifyVariable(
                "p",
                new ConcatenationPattern(
                        new ComplementPattern(new SymbolPattern(new CharSymbol("a"))),
                        new SymbolPattern(new CharSymbol("b"))));
    }
    
    public void testComplementPattern2() {
        parser.parse("$p = /~(ab)/");
        verifyVariable(
            "p",
            new ComplementPattern(
                new VariableBindingPattern(
                    new ConcatenationPattern(
                        new SymbolPattern(new CharSymbol("a")),
                        new SymbolPattern(new CharSymbol("b"))),
                new Variable("1"))));
    }
    
    public void testEmptyTreePattern() {
        parser.parse("$p = %{()}");
        verifyVariable("p", new EmptyPattern());
    }
    
    public void testVariableBindingTreePattern() {
        parser.parse("$p = %{a as $x}");
        verifyVariable("p", new VariableBindingPattern(new SymbolPattern("a"), new Variable("x")));
    }
    
    public void testVariableReferenceTreePattern() {
        parser.parse("$p = %{ref($x)}");
        verifyVariable("p", new VariableReferencePattern(new Variable("x")));
    }
    
    public void testTreeGrammar1() {
        parser.parse("$t1 = a[$$t2;$$t3];");
        parser.parse("$t2 = a[$$t2;$$t3];");
        parser.parse(
                "TreeGrammar $$t1 {" +
                "  $$t1 -> $t1;" +
                "  $$t2 -> $t2;" + 
                "  $$t3 -> null;" +
                "}");
        Map m = parser.getResult();
        verifyVariable("_",
                new TreeGrammar(
                        new BinaryTreeVariable("t1"),
                        new ProductionRule[]{
                            new ProductionRule(new BinaryTreeVariable("t1"), (IBinaryTree) m.get(new Variable("t1"))),
                            new ProductionRule(new BinaryTreeVariable("t2"), (IBinaryTree) m.get(new Variable("t2"))),
                            new ProductionRule(new BinaryTreeVariable("t3"), BinaryTree.LEAF),
                        }
                ));
    }
    
    public void testLastExpression1() {
        parser.parse("abc");
        verifyVariable("_", new StringSymbol("abc"));
    }
    
    public void testLastExpression2() {
        parser.parse("/a/");
        verifyVariable("_", new SymbolPattern(new CharSymbol("a")));
    }
    
    public void testAutomaton() {
        parser.parse(
          "$fst = " +
          "Automaton(<s1>, {<s3>}, {" +
          "  Transition(<s1>, <s2>, CharSymbol(\"a\"))," +
          "  Transition(<s2>, <s3>, CharSymbol(\"b\"))," +
          "})");
        IAutomaton fst = (IAutomaton) parser.getResult().get(new Variable("fst"));
        
        assertTrue(fst != null);
        assertTrue(fst.accept(StringSymbol.toCharSymbols("ab")));
    }

    public void testCharSetPattern1() {
      parser.parse("$p = /[ab]/");
      verifyVariable(
              "p",
              new UnionPattern(
                new SymbolPattern(new CharSymbol("a")),
                new SymbolPattern(new CharSymbol("b"))));
    }
  
    public void testCharSetPattern2() {
      parser.parse("$p = /[a-c]/");
      
      verifyVariable(
              "p",
              new UnionPattern(
                new UnionPattern(
                new SymbolPattern(new CharSymbol("a")),
                new SymbolPattern(new CharSymbol("b"))),
              new SymbolPattern(new CharSymbol("c"))));
    }
    public void testCharSetPattern3() {
      parser.parse("$p = /[a-cA-C]/");
      verifyVariable(
              "p",
              new UnionPattern(
                new UnionPattern(
                  new UnionPattern(
                    new SymbolPattern(new CharSymbol("a")),
                    new SymbolPattern(new CharSymbol("b"))
                  ),
                  new SymbolPattern(new CharSymbol("c"))
                ),
                new UnionPattern(
                    new UnionPattern(
                      new SymbolPattern(new CharSymbol("A")),
                      new SymbolPattern(new CharSymbol("B"))
                    ),
                    new SymbolPattern(new CharSymbol("C"))
                  )
                )
      );
    }
    public void testCharSetPattern4() {
      parser.parse("$p = /[-]/");
      verifyVariable(
              "p",
               new SymbolPattern(new CharSymbol("-")));
    }

    public void testCharSetPattern5() {
      parser.parse("$p = /[]-]/");
      verifyVariable(
              "p",
              new UnionPattern(
                new SymbolPattern(new CharSymbol("]")),
                new SymbolPattern(new CharSymbol("-"))));
    }

    public void testCharSetPattern6() {
      parser.parse("$p = /[--.]/");
      verifyVariable(
              "p",
              new UnionPattern(
                new SymbolPattern(new CharSymbol("-")),
                new SymbolPattern(new CharSymbol("."))));
    }

    public void testComplementCharSetPattern1() {
      parser.parse("$p = /[^ab]/");
      verifyVariable(
              "p",
              new IntersectionPattern( new SymbolPattern(new CharPatternSymbol("\\.")),
              new ComplementPattern(
                new UnionPattern(
                  new SymbolPattern(new CharSymbol("a")),
                  new SymbolPattern(new CharSymbol("b"))
                ))));
    }
  
    public void testComplementCharSetPattern2() {
      parser.parse("$p = /[^a-c]/");
      verifyVariable(
              "p",
              new IntersectionPattern( new SymbolPattern(new CharPatternSymbol("\\.")),
              new ComplementPattern(
                new UnionPattern(
                  new UnionPattern(
                    new SymbolPattern(new CharSymbol("a")),
                    new SymbolPattern(new CharSymbol("b"))),
                  new SymbolPattern(new CharSymbol("c"))
                ))));
    }
    public void testComplementCharSetPattern3() {
      parser.parse("$p = /[^a-cA-C]/");
      verifyVariable(
              "p",
              new IntersectionPattern( new SymbolPattern(new CharPatternSymbol("\\.")),
              new ComplementPattern(
                new UnionPattern(
                  new UnionPattern(
                    new UnionPattern(
                      new SymbolPattern(new CharSymbol("a")),
                      new SymbolPattern(new CharSymbol("b"))),
                    new SymbolPattern(new CharSymbol("c"))
                  ),
                  new UnionPattern(
                    new UnionPattern(
                        new SymbolPattern(new CharSymbol("A")),
                        new SymbolPattern(new CharSymbol("B"))),
                    new SymbolPattern(new CharSymbol("C"))
                  ))
              ))
      );
    }
    public void testComplementCharSetPattern4() {
      parser.parse("$p = /[^-]/");
      verifyVariable(
              "p",
              new IntersectionPattern( new SymbolPattern(new CharPatternSymbol("\\.")),
               new ComplementPattern(new SymbolPattern(new CharSymbol("-")))));
    }

    public void testComplementCharSetPattern5() {
      parser.parse("$p = /[^]-]/");
      verifyVariable(
              "p",
              new IntersectionPattern( new SymbolPattern(new CharPatternSymbol("\\.")),
              new ComplementPattern(
                new UnionPattern(
                  new SymbolPattern(new CharSymbol("]")),
                  new SymbolPattern(new CharSymbol("-"))
              ))));
    }

    public void testComplementCharSetPattern6() {
      parser.parse("$p = /[^--.]/");
      verifyVariable(
              "p",
              new IntersectionPattern( new SymbolPattern(new CharPatternSymbol("\\.")),
              new ComplementPattern(
                new UnionPattern(
                  new SymbolPattern(new CharSymbol("-")),
                  new SymbolPattern(new CharSymbol("."))))));
    }
    public void testComplementCharSetPattern7() {
      parser.parse("$p = /[^^]/");
      verifyVariable(
              "p",
              new IntersectionPattern( new SymbolPattern(new CharPatternSymbol("\\.")),
              new ComplementPattern(new SymbolPattern(new CharSymbol("^")))));
    }
}
