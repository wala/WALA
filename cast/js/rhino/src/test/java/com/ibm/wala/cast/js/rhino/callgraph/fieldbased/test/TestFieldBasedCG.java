/*
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.WalaException;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestFieldBasedCG extends AbstractFieldBasedTest {
  private static final List<GraphAssertion> assertionsForSimpleJS =
      List.of(
          new GraphAssertion(ROOT, new String[] {"suffix:simple.js"}),
          new GraphAssertion(
              "suffix:simple.js", new String[] {"suffix:foo", "suffix:bar", "suffix:A"}),
          new GraphAssertion("suffix:foo", new String[] {"suffix:bar"}),
          new GraphAssertion("suffix:aluis", new String[] {"suffix:aluis"}));

  @Test
  public void testSimpleJSPessimistic() throws WalaException, Error, CancelException {
    runTest("tests/field-based/simple.js", assertionsForSimpleJS, BuilderType.PESSIMISTIC);
  }

  @Test
  public void testSimpleJSOptimistic() throws WalaException, Error, CancelException {
    runTest("tests/field-based/simple.js", assertionsForSimpleJS, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testSimpleJSWorklist() throws WalaException, Error, CancelException {
    runTest("tests/field-based/simple.js", assertionsForSimpleJS, BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final List<GraphAssertion> assertionsForOneShot =
      List.of(
          new GraphAssertion(ROOT, new String[] {"suffix:oneshot.js"}),
          new GraphAssertion("suffix:oneshot.js", new String[] {"suffix:f"}),
          new GraphAssertion("suffix:f", new String[] {"suffix:g"}));

  @Test
  public void testOneshotPessimistic() throws WalaException, Error, CancelException {
    runTest("tests/field-based/oneshot.js", assertionsForOneShot, BuilderType.PESSIMISTIC);
  }

  @Test
  public void testOneshotOptimistic() throws WalaException, Error, CancelException {
    runTest("tests/field-based/oneshot.js", assertionsForOneShot, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testOneshotWorklist() throws WalaException, Error, CancelException {
    runTest("tests/field-based/oneshot.js", assertionsForOneShot, BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final List<GraphAssertion> assertionsForCallbacks =
      List.of(
          new GraphAssertion(ROOT, new String[] {"suffix:callbacks.js"}),
          new GraphAssertion("suffix:callbacks.js", new String[] {"suffix:f"}),
          new GraphAssertion("suffix:f", new String[] {"suffix:k", "suffix:n"}),
          new GraphAssertion("suffix:k", new String[] {"suffix:l", "suffix:p"}));

  @Test
  public void testCallbacksOptimistic() throws WalaException, Error, CancelException {
    runTest("tests/field-based/callbacks.js", assertionsForCallbacks, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testCallbacksWorklist() throws WalaException, Error, CancelException {
    runTest(
        "tests/field-based/callbacks.js", assertionsForCallbacks, BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final List<GraphAssertion> assertionsForLexical =
      List.of(new GraphAssertion("suffix:h", new String[] {"suffix:g"}));

  @Test
  public void testLexicalPessimistic() throws WalaException, Error, CancelException {
    runTest("tests/field-based/lexical.js", assertionsForLexical, BuilderType.PESSIMISTIC);
  }

  @Test
  public void testLexicalOptimistic() throws WalaException, Error, CancelException {
    runTest("tests/field-based/lexical.js", assertionsForLexical, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testLexicalWorklist() throws WalaException, Error, CancelException {
    runTest("tests/field-based/lexical.js", assertionsForLexical, BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final List<GraphAssertion> assertionsForReflectiveCall =
      List.of(
          new GraphAssertion(
              "suffix:h",
              new String[] {"suffix:Function_prototype_call", "suffix:Function_prototype_apply"}),
          new GraphAssertion("suffix:Function_prototype_call", new String[] {"suffix:f"}),
          new GraphAssertion("suffix:Function_prototype_apply", new String[] {"suffix:x"}),
          new GraphAssertion("suffix:f", new String[] {"suffix:k"}),
          new GraphAssertion("suffix:p", new String[] {"suffix:n"}));

  @Test
  public void testReflectiveCallOptimistic() throws WalaException, Error, CancelException {
    runTest(
        "tests/field-based/reflective_calls.js",
        assertionsForReflectiveCall,
        BuilderType.OPTIMISTIC);
  }

  @Test
  public void testReflectiveCallWorklist() throws WalaException, Error, CancelException {
    runTest(
        "tests/field-based/reflective_calls.js",
        assertionsForReflectiveCall,
        BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final List<GraphAssertion> assertionsForNew =
      List.of(
          new GraphAssertion("suffix:new.js", new String[] {"suffix:g", "suffix:f"}),
          new GraphAssertion("suffix:g", new String[] {"!suffix:k"}));

  @Test
  public void testNewOptimistic() throws WalaException, Error, CancelException {
    runTest("tests/field-based/new.js", assertionsForNew, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testNewWorklist() throws WalaException, Error, CancelException {
    runTest("tests/field-based/new.js", assertionsForNew, BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final List<GraphAssertion> assertionsForCallbacks2 =
      List.of(
          new GraphAssertion("suffix:callbacks2.js", new String[] {"suffix:g"}),
          new GraphAssertion("suffix:g", new String[] {"suffix:k", "!suffix:l"}));

  @Test
  public void testCallbacks2Optimistic() throws WalaException, Error, CancelException {
    runTest("tests/field-based/callbacks2.js", assertionsForCallbacks2, BuilderType.OPTIMISTIC);
  }

  @Test
  public void testCallbacks2Worklist() throws WalaException, Error, CancelException {
    runTest(
        "tests/field-based/callbacks2.js",
        assertionsForCallbacks2,
        BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Test
  public void testNewFnEmptyNoCrash() throws WalaException, Error, CancelException {
    runTest("tests/field-based/new_fn_empty.js", List.of(), BuilderType.OPTIMISTIC_WORKLIST);
  }

  private static final List<GraphAssertion> assertionsForRecursiveLexWrite =
      List.of(new GraphAssertion("suffix:outer", new String[] {"suffix:foo", "suffix:bar"}));

  @Test
  public void testRecursiveLexWrite() throws WalaException, Error, CancelException {
    runTest(
        "tests/recursive_lex_write.js",
        assertionsForRecursiveLexWrite,
        BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Test
  public void testNamedFnTwice() throws WalaException, Error, CancelException {
    // hack since Windows column offsets are different
    String secondFunName =
        PlatformUtil.onWindows() ? "suffix:testFunExp@390" : "suffix:testFunExp@381";
    runTest(
        "tests/named_fn_twice.js",
        List.of(
            new GraphAssertion(
                "suffix:named_fn_twice.js", new String[] {"suffix:testFunExp", secondFunName})),
        BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Test
  public void testSwitchDefault() throws WalaException, Error, CancelException {
    runTest(
        "tests/switch_default.js",
        List.of(
            new GraphAssertion("suffix:withSwitch", new String[] {"suffix:fun1", "suffix:fun2"}),
            new GraphAssertion(
                "suffix:withSwitchStr", new String[] {"suffix:fun3", "suffix:fun4"})),
        BuilderType.OPTIMISTIC_WORKLIST);
  }

  @Disabled
  @Test
  public void testBug2979() throws WalaException, Error, CancelException {
    System.err.println(
        runTest(
            "pages/2979.html",
            List.of(),
            BuilderType.PESSIMISTIC,
            BuilderType.OPTIMISTIC,
            BuilderType.OPTIMISTIC_WORKLIST));
  }

  @Test
  public void testBadNewFunctionCall() throws WalaException, CancelException {
    runTest(
        "tests/field-based/bad_new_function_call.js", List.of(), BuilderType.OPTIMISTIC_WORKLIST);
  }
}
