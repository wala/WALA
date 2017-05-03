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
package com.ibm.wala.cast.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.cast.util.CAstPattern.Segments;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.core.tests.util.WalaTestCase;

public class TestCAstPattern extends WalaTestCase {

  private static final int NAME_ASSERTION_SINGLE = 501;

  private static final int NAME_ASSERTION_MULTI = 502;

  private static class TestingCAstImpl extends CAstImpl {
    private final Map testNameMap = new HashMap();

    @Override
    @SuppressWarnings("unchecked")
    public CAstNode makeNode(int kind, CAstNode children[]) {
      if (kind == NAME_ASSERTION_SINGLE || kind == NAME_ASSERTION_MULTI) {
        assert children.length == 2;
        assert children[0].getValue() instanceof String;
        @SuppressWarnings("unused")
        CAstNode result = children[1];
        if (kind == NAME_ASSERTION_SINGLE) {
          testNameMap.put(children[0].getValue(), children[1]);
        } else {
          if (!testNameMap.containsKey(children[0].getValue())) {
            testNameMap.put(children[0].getValue(), new ArrayList());
          }

          ((List) testNameMap.get(children[0].getValue())).add(children[1]);
        }
        return children[1];
      } else {
        return super.makeNode(kind, children);
      }
    }

  }

  private void test(CAstPattern p, CAstNode n, Map names) {
    System.err.println(("testing pattern " + p));
    System.err.println(("testing with input " + CAstPrinter.print(n)));

    if (names == null) {
      Assert.assertFalse(p.match(n, null));
    } else {
      Segments s = CAstPattern.match(p, n);
      Assert.assertTrue(s != null);
      for (Iterator ns = names.keySet().iterator(); ns.hasNext();) {
        String nm = (String) ns.next();
        Object o = names.get(nm);
        if (o instanceof CAstNode) {
          System.err.println(("found " + CAstPrinter.print(s.getSingle(nm)) + " for " + nm));
          Assert.assertTrue("for name " + nm + ": expected " + names.get(nm) + " but got " + s.getSingle(nm), names.get(nm).equals(
              s.getSingle(nm)));
        } else {
          for (Iterator cs = s.getMultiple(nm).iterator(); cs.hasNext();) {
            System.err.println(("found " + CAstPrinter.print((CAstNode) cs.next()) + " for " + nm));
          }
          Assert.assertTrue("for name " + nm + ": expected " + names.get(nm) + " but got " + s.getMultiple(nm), names.get(nm)
              .equals(s.getMultiple(nm)));
        }
      }
    }
  }

  private final CAstPattern simpleNamePattern = CAstPattern.parse("<top>BINARY_EXPR(\"+\",<left>\"prefix\",\"suffix\")");

  private final CAstNode simpleNameAst;

  private final Map simpleNameMap;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    simpleNameAst = Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("top"), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("left"), Ast.makeConstant("prefix")), Ast
        .makeConstant("suffix")));

    simpleNameMap = Ast.testNameMap;
  }

  @Test public void testSimpleName() {
    test(simpleNamePattern, simpleNameAst, simpleNameMap);
  }

  private final CAstPattern simpleStarNamePattern = CAstPattern.parse("<top>BINARY_EXPR(\"+\",*,<right>\"suffix\")");

  private final CAstNode simpleStarNameAst;

  private final Map simpleStarNameMap;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    simpleStarNameAst = Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("top"), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeConstant("prefix"), Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("right"), Ast
        .makeConstant("suffix"))));

    simpleStarNameMap = Ast.testNameMap;
  }

  @Test public void testSimpleStarName() {
    test(simpleStarNamePattern, simpleStarNameAst, simpleStarNameMap);
  }

  private final CAstPattern simpleRepeatedPattern = CAstPattern.parse("<top>BINARY_EXPR(\"+\",<children>@(VAR(*))@)");

  private final CAstNode simpleRepeatedAstOne;

  private final Map simpleRepeatedMapOne;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    simpleRepeatedAstOne = Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("top"), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("children"), Ast.makeNode(CAstNode.VAR, Ast
        .makeConstant("suffix")))));

    simpleRepeatedMapOne = Ast.testNameMap;
  }

  @Test public void testSimpleRepeatedOne() {
    test(simpleRepeatedPattern, simpleRepeatedAstOne, simpleRepeatedMapOne);
  }

  private final CAstNode simpleRepeatedAstTwo;

  private final Map simpleRepeatedMapTwo;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    simpleRepeatedAstTwo = Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("top"), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("children"), Ast.makeNode(CAstNode.VAR, Ast
        .makeConstant("prefix"))), Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("children"), Ast.makeNode(CAstNode.VAR, Ast
        .makeConstant("suffix")))));

    simpleRepeatedMapTwo = Ast.testNameMap;
  }

  @Test public void testSimpleRepeatedTwo() {
    test(simpleRepeatedPattern, simpleRepeatedAstTwo, simpleRepeatedMapTwo);
  }

  private final CAstNode simpleRepeatedAstThree;

  private final Map simpleRepeatedMapThree;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    simpleRepeatedAstThree = Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("top"), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("children"), Ast.makeNode(CAstNode.VAR, Ast
        .makeConstant("prefix"))), Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("children"), Ast.makeNode(CAstNode.VAR, Ast
        .makeConstant("middle"))), Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("children"), Ast.makeNode(CAstNode.VAR, Ast
        .makeConstant("suffix")))));

    simpleRepeatedMapThree = Ast.testNameMap;
  }

  @Test public void testSimpleRepeatedThree() {
    test(simpleRepeatedPattern, simpleRepeatedAstThree, simpleRepeatedMapThree);
  }

  private final CAstPattern simpleDoubleStarPattern = CAstPattern.parse("<top>BINARY_EXPR(\"+\",<children>**)");

  private final CAstNode simpleDoubleStarAst;

  private final Map simpleDoubleStarMap;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    simpleDoubleStarAst = Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("top"), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("children"), Ast.makeNode(CAstNode.VAR, Ast
        .makeConstant("prefix"))), Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("children"), Ast.makeNode(CAstNode.VAR, Ast
        .makeConstant("suffix")))));

    simpleDoubleStarMap = Ast.testNameMap;
  }

  @Test public void testSimpleDoubleStar() {
    test(simpleDoubleStarPattern, simpleDoubleStarAst, simpleDoubleStarMap);
  }

  private final CAstPattern simpleAlternativePattern = CAstPattern
      .parse("<top>BINARY_EXPR(\"+\",<firstchild>|(VAR(\"suffix\")||VAR(\"prefix\"))|,*)");

  private final CAstNode simpleAlternativeAst;

  private final Map simpleAlternativeMap;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    simpleAlternativeAst = Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("top"), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("firstchild"), Ast.makeNode(CAstNode.VAR, Ast
        .makeConstant("prefix"))), Ast.makeNode(CAstNode.VAR, Ast.makeConstant("suffix"))));

    simpleAlternativeMap = Ast.testNameMap;
  }

  @Test public void testSimpleAlternative() {
    test(simpleAlternativePattern, simpleAlternativeAst, simpleAlternativeMap);
  }

  private final CAstPattern simpleOptionalPattern = CAstPattern
      .parse("<top>BINARY_EXPR(\"+\",?(VAR(\"prefix\"))?,<child>VAR(\"suffix\"))");

  private final CAstNode simpleOptionalAstWith;

  private final Map simpleOptionalMapWith;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    simpleOptionalAstWith = Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("top"), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(CAstNode.VAR, Ast.makeConstant("prefix")), Ast.makeNode(NAME_ASSERTION_SINGLE, Ast
        .makeConstant("child"), Ast.makeNode(CAstNode.VAR, Ast.makeConstant("suffix")))));

    simpleOptionalMapWith = Ast.testNameMap;
  }

  @Test public void testSimpleOptionalWith() {
    test(simpleOptionalPattern, simpleOptionalAstWith, simpleOptionalMapWith);
  }

  private final CAstNode simpleOptionalAstNot;

  private final Map simpleOptionalMapNot;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    simpleOptionalAstNot = Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("top"), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(NAME_ASSERTION_SINGLE, Ast.makeConstant("child"), Ast.makeNode(CAstNode.VAR, Ast
        .makeConstant("suffix")))));

    simpleOptionalMapNot = Ast.testNameMap;
  }

  @Test public void testSimpleOptionalNot() {
    test(simpleOptionalPattern, simpleOptionalAstNot, simpleOptionalMapNot);
  }

  private final String recursiveTreeStr = "|({leaf}|(<const>CONSTANT()||VAR(<vars>*))|||{node}BINARY_EXPR(\"+\",`leaf`,|(`leaf`||`node`)|))|";

  private final CAstPattern recursiveTreePattern = CAstPattern.parse(recursiveTreeStr);

  private final CAstNode recursiveTreeOneAst;

  private final Map recursiveTreeOneMap;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    recursiveTreeOneAst = Ast.makeNode(CAstNode.BINARY_EXPR, Ast.makeConstant("+"), Ast.makeNode(CAstNode.VAR, Ast.makeNode(
        NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast.makeConstant("x"))), Ast.makeNode(CAstNode.VAR, Ast.makeNode(
        NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast.makeConstant("y"))));

    recursiveTreeOneMap = Ast.testNameMap;
  }

  @Test public void testRecursiveTreeOne() {
    test(recursiveTreePattern, recursiveTreeOneAst, recursiveTreeOneMap);
  }

  private final CAstNode recursiveTreeTwoAst;

  private final Map recursiveTreeTwoMap;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    recursiveTreeTwoAst = Ast.makeNode(CAstNode.BINARY_EXPR, Ast.makeConstant("+"), Ast.makeNode(CAstNode.VAR, Ast.makeNode(
        NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast.makeConstant("x"))), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(CAstNode.VAR, Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast
        .makeConstant("y"))), Ast.makeNode(CAstNode.VAR, Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast
        .makeConstant("z")))));

    recursiveTreeTwoMap = Ast.testNameMap;
  }

  @Test public void testRecursiveTreeTwo() {
    test(recursiveTreePattern, recursiveTreeTwoAst, recursiveTreeTwoMap);
  }

  private final CAstNode recursiveTreeFiveAst;

  private final Map recursiveTreeFiveMap;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    recursiveTreeFiveAst = Ast.makeNode(CAstNode.BINARY_EXPR, Ast.makeConstant("+"), Ast.makeNode(CAstNode.VAR, Ast.makeNode(
        NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast.makeConstant("u"))), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(CAstNode.VAR, Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast
        .makeConstant("v"))), Ast.makeNode(CAstNode.BINARY_EXPR, Ast.makeConstant("+"), Ast.makeNode(CAstNode.VAR, Ast.makeNode(
        NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast.makeConstant("w"))), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("+"), Ast.makeNode(CAstNode.VAR, Ast.makeNode(NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast
        .makeConstant("x"))), Ast.makeNode(CAstNode.BINARY_EXPR, Ast.makeConstant("+"), Ast.makeNode(CAstNode.VAR, Ast.makeNode(
        NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast.makeConstant("y"))), Ast.makeNode(CAstNode.VAR, Ast.makeNode(
        NAME_ASSERTION_MULTI, Ast.makeConstant("vars"), Ast.makeConstant("z"))))))));

    recursiveTreeFiveMap = Ast.testNameMap;
  }

  @Test public void testRecursiveTreeFive() {
    test(recursiveTreePattern, recursiveTreeFiveAst, recursiveTreeFiveMap);
  }

  private final CAstPattern buggyRecursiveTreePattern = CAstPattern
      .parse("|({leaf}|(<const>CONSTANT()||VAR(<vars>*))|||{node}BINARY_EXPR(\"+\",`leaf`,`node`))|");

  @Test public void testBuggyRecursiveTreeOne() {
    test(buggyRecursiveTreePattern, recursiveTreeOneAst, null);
  }

  @Test public void testBuggyRecursiveTreeTwo() {
    test(buggyRecursiveTreePattern, recursiveTreeTwoAst, null);
  }

  @Test public void testBuggyRecursiveTreeFive() {
    test(buggyRecursiveTreePattern, recursiveTreeFiveAst, null);
  }

  private static final String extraTestsStr = "BINARY_EXPR(|(\"==\"||\"\\==\")|,|(CONSTANT()||VAR(CONSTANT()))|,|(CONSTANT()||VAR(CONSTANT()))|)";

  private final CAstPattern testedTreePattern = CAstPattern.parse("{top}|(" + recursiveTreeStr + "||BINARY_EXPR(\",\","
      + extraTestsStr + ",`top`))|");

  @Test public void testTestedTreeOne() {
    test(testedTreePattern, recursiveTreeOneAst, recursiveTreeOneMap);
  }

  @Test public void testTestedTreeTwo() {
    test(testedTreePattern, recursiveTreeTwoAst, recursiveTreeTwoMap);
  }

  @Test public void testTestedTreeFive() {
    test(testedTreePattern, recursiveTreeFiveAst, recursiveTreeFiveMap);
  }

  private final CAstNode testedTreeOneAst;

  private final Map testedTreeOneMap;

  {
    TestingCAstImpl Ast = new TestingCAstImpl();

    testedTreeOneAst = Ast.makeNode(CAstNode.BINARY_EXPR, Ast.makeConstant(","), Ast.makeNode(CAstNode.BINARY_EXPR, Ast
        .makeConstant("=="), Ast.makeNode(CAstNode.VAR, Ast.makeConstant("x")), Ast.makeConstant(7)), Ast.makeNode(
        CAstNode.BINARY_EXPR, Ast.makeConstant("+"), Ast.makeNode(CAstNode.VAR, Ast.makeNode(NAME_ASSERTION_MULTI, Ast
            .makeConstant("vars"), Ast.makeConstant("x"))), Ast.makeNode(CAstNode.VAR, Ast.makeNode(NAME_ASSERTION_MULTI, Ast
            .makeConstant("vars"), Ast.makeConstant("y")))));

    testedTreeOneMap = Ast.testNameMap;
  }

  @Test public void testTestedTreeOneWithTest() {
    test(testedTreePattern, testedTreeOneAst, testedTreeOneMap);
  }

}
