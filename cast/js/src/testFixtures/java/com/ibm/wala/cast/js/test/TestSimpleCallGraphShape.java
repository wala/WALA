/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.PropertyNameContextSelector;
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil;
import com.ibm.wala.core.util.ProgressMaster;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;
import java.io.IOException;
import java.util.List;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public abstract class TestSimpleCallGraphShape extends TestJSCallGraphShape {

  protected static final List<GraphAssertion> assertionsForArgs =
      List.of(
          new GraphAssertion(ROOT, new String[] {"args.js"}),
          new GraphAssertion("args.js", new String[] {"args.js/a"}),
          new GraphAssertion("args.js/a", new String[] {"args.js/x", "args.js/y"}));

  @Test
  public void testArgs()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "args.js");
    verifyGraphAssertions(CG, assertionsForArgs);
  }

  protected static final List<GraphAssertion> assertionsForSimple =
      List.of(
          new GraphAssertion(ROOT, new String[] {"simple.js"}),
          new GraphAssertion(
              "simple.js",
              new String[] {
                "simple.js/bad",
                "simple.js/silly",
                "simple.js/fib",
                "simple.js/stranger",
                "simple.js/trivial",
                "simple.js/rubbish",
                "simple.js/weirder"
              }),
          new GraphAssertion("simple.js/trivial", new String[] {"simple.js/trivial/inc"}),
          new GraphAssertion(
              "simple.js/rubbish",
              new String[] {"simple.js/weirder", "simple.js/stranger", "simple.js/rubbish"}),
          new GraphAssertion("simple.js/fib", new String[] {"simple.js/fib"}),
          new GraphAssertion("simple.js/weirder", new String[] {"prologue.js/Math_abs"}));

  @Test
  public void testSimple()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "simple.js");
    verifyGraphAssertions(CG, assertionsForSimple);
  }

  private static final List<GraphAssertion> assertionsForObjects =
      List.of(
          new GraphAssertion(ROOT, new String[] {"objects.js"}),
          new GraphAssertion(
              "objects.js",
              new String[] {
                "objects.js/objects_are_fun", "objects.js/other", "objects.js/something"
              }),
          new GraphAssertion(
              "objects.js/other",
              new String[] {"objects.js/something", "objects.js/objects_are_fun/nothing"}),
          new GraphAssertion(
              "objects.js/objects_are_fun",
              new String[] {"objects.js/other", "objects.js/whatever"}));

  @Test
  public void testObjects()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "objects.js");
    verifyGraphAssertions(CG, assertionsForObjects);
  }

  private static final List<CFGAssertion> cfgAssertionsForInherit =
      List.of(
          new CFGAssertion(
              "ctor:inherit.js/objectMasquerading/Rectangle",
              new int[][] {{1, 7}, {2}, {3, 7}, {4, 7}, {5, 6}, {7}, {7}}),
          new CFGAssertion(
              "ctor:inherit.js/sharedClassObject/Rectangle",
              new int[][] {{1, 7}, {2}, {3, 7}, {4, 7}, {5, 6}, {7}, {7}}));

  private static final List<GraphAssertion> assertionsForInherit =
      List.of(
          new GraphAssertion(ROOT, new String[] {"inherit.js"}),
          new GraphAssertion(
              "inherit.js",
              new String[] {
                "inherit.js/objectMasquerading",
                "inherit.js/objectMasquerading/Rectangle/area",
                "inherit.js/Polygon/shape",
                "inherit.js/sharedClassObject",
                "inherit.js/sharedClassObject/Rectangle/area"
              }),
          new GraphAssertion(
              "inherit.js/objectMasquerading",
              new String[] {"ctor:inherit.js/objectMasquerading/Rectangle"}),
          new GraphAssertion(
              "ctor:inherit.js/objectMasquerading/Rectangle",
              new String[] {"inherit.js/objectMasquerading/Rectangle"}),
          new GraphAssertion(
              "inherit.js/objectMasquerading/Rectangle", new String[] {"inherit.js/Polygon"}),
          new GraphAssertion(
              "inherit.js/sharedClassObject",
              new String[] {"ctor:inherit.js/sharedClassObject/Rectangle"}),
          new GraphAssertion(
              "ctor:inherit.js/sharedClassObject/Rectangle",
              new String[] {"inherit.js/sharedClassObject/Rectangle"}));

  @Test
  public void testInherit()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "inherit.js");
    verifyGraphAssertions(CG, assertionsForInherit);
    verifyCFGAssertions(CG, cfgAssertionsForInherit);
  }

  private static final List<GraphAssertion> assertionsForNewfn =
      List.of(
          new GraphAssertion(ROOT, new String[] {"newfn.js"}),
          new GraphAssertion(
              "newfn.js",
              new String[] {
                "suffix:ctor$1/_fromctor", "suffix:ctor$2/_fromctor", "suffix:ctor$3/_fromctor"
              }));

  @Test
  public void testNewfn()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "newfn.js");
    verifyGraphAssertions(CG, assertionsForNewfn);
  }

  private static final List<GraphAssertion> assertionsForControlflow =
      List.of(
          new GraphAssertion(ROOT, new String[] {"control-flow.js"}),
          new GraphAssertion(
              "control-flow.js",
              new String[] {
                "control-flow.js/testSwitch",
                "control-flow.js/testDoWhile",
                "control-flow.js/testWhile",
                "control-flow.js/testFor",
                "control-flow.js/testReturn"
              }));

  @Test
  public void testControlflow()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "control-flow.js");
    verifyGraphAssertions(CG, assertionsForControlflow);
  }

  private static final List<GraphAssertion> assertionsForMoreControlflow =
      List.of(
          new GraphAssertion(ROOT, new String[] {"more-control-flow.js"}),
          new GraphAssertion(
              "more-control-flow.js",
              new String[] {
                "more-control-flow.js/testSwitch",
                "more-control-flow.js/testIfConvertedSwitch",
                "more-control-flow.js/testDoWhile",
                "more-control-flow.js/testWhile",
                "more-control-flow.js/testFor",
                "more-control-flow.js/testReturn"
              }));

  @Test
  public void testMoreControlflow()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "more-control-flow.js");
    verifyGraphAssertions(CG, assertionsForMoreControlflow);
  }

  private static final List<GraphAssertion> assertionsForForin =
      List.of(
          new GraphAssertion(ROOT, new String[] {"forin.js"}),
          new GraphAssertion("forin.js", new String[] {"forin.js/testForIn"}),
          new GraphAssertion(
              "forin.js/testForIn", new String[] {"forin.js/testForIn1", "forin.js/testForIn2"}));

  @Test
  public void testForin()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "forin.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForForin);
  }

  private static final List<GraphAssertion> assertionsForSimpleLexical =
      List.of(
          new GraphAssertion(ROOT, new String[] {"simple-lexical.js"}),
          new GraphAssertion("simple-lexical.js", new String[] {"simple-lexical.js/outer"}),
          new GraphAssertion(
              "simple-lexical.js/outer",
              new String[] {
                "simple-lexical.js/outer/indirect",
                "simple-lexical.js/outer/inner",
                "simple-lexical.js/outer/inner2",
                "simple-lexical.js/outer/inner3"
              }),
          new GraphAssertion(
              "simple-lexical.js/outer/inner2",
              new String[] {"simple-lexical.js/outer/inner", "simple-lexical.js/outer/inner3"}),
          new GraphAssertion(
              "simple-lexical.js/outer/indirect",
              new String[] {"simple-lexical.js/outer/inner", "simple-lexical.js/outer/inner3"}));

  @Test
  public void testSimpleLexical()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "simple-lexical.js");
    verifyGraphAssertions(CG, assertionsForSimpleLexical);
  }

  @Test
  public void testRecursiveLexical()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    // just checking that we have a sufficient bailout to ensure termination
    JSCallGraphBuilderUtil.makeScriptCG("tests", "recursive_lexical.js");
  }

  private static final List<GraphAssertion> assertionsForLexicalMultiple =
      List.of(
          new GraphAssertion(ROOT, new String[] {"lexical_multiple_calls.js"}),
          new GraphAssertion(
              "suffix:lexical_multiple_calls.js", new String[] {"suffix:reachable1"}),
          new GraphAssertion(
              "suffix:lexical_multiple_calls.js", new String[] {"suffix:reachable2"}));

  @Test
  public void testLexicalMultiple()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "lexical_multiple_calls.js");
    verifyGraphAssertions(CG, assertionsForLexicalMultiple);
  }

  private static final List<GraphAssertion> assertionsForTry =
      List.of(
          new GraphAssertion(ROOT, new String[] {"try.js"}),
          new GraphAssertion(
              "try.js",
              new String[] {"try.js/tryCatch", "try.js/tryFinally", "try.js/tryCatchFinally"}),
          new GraphAssertion(
              "try.js/tryCatch",
              new String[] {"try.js/targetOne", "try.js/targetTwo", "try.js/two"}),
          new GraphAssertion(
              "try.js/tryFinally",
              new String[] {"try.js/targetOne", "try.js/targetTwo", "try.js/two"}),
          new GraphAssertion(
              "try.js/tryCatchFinally",
              new String[] {"try.js/targetOne", "try.js/targetTwo", "try.js/three", "try.js/two"}),
          new GraphAssertion(
              "try.js/tryCatchTwice",
              new String[] {"try.js/targetOne", "try.js/targetTwo", "try.js/three", "try.js/two"}),
          new GraphAssertion("try.js/testRet", new String[] {"try.js/three", "try.js/two"}));

  @Test
  public void testTry()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "try.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());

    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) B.getContextInterpreter(), B.getPointerAnalysis(), CG);

    verifyGraphAssertions(CG, assertionsForTry);
  }

  private static final List<GraphAssertion> assertionsForStringOp =
      List.of(
          new GraphAssertion(ROOT, new String[] {"string-op.js"}),
          new GraphAssertion(
              "string-op.js", new String[] {"string-op.js/getOp", "string-op.js/plusNum"}));

  @Test
  public void testStringOp()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "string-op.js");
    B.getOptions().setTraceStringConstants(true);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForStringOp);
  }

  private static final List<GraphAssertion> assertionsForUpward =
      List.of(
          new GraphAssertion(ROOT, new String[] {"upward.js"}),
          new GraphAssertion(
              "upward.js",
              new String[] {
                "upward.js/Obj/setit",
                "upward.js/Obj/getit",
                "upward.js/tester1",
                "upward.js/tester2"
              }));

  @Test
  public void testUpward()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "upward.js");
    verifyGraphAssertions(CG, assertionsForUpward);
  }

  private static final List<GraphAssertion> assertionsForStringPrims =
      List.of(
          new GraphAssertion(ROOT, new String[] {"string-prims.js"}),
          new GraphAssertion(
              "string-prims.js",
              new String[] {
                "prologue.js/String_prototype_split", "prologue.js/String_prototype_toUpperCase"
              }));

  @Test
  public void testStringPrims()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    SSAPropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "string-prims.js");
    B.getOptions().setTraceStringConstants(true);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForStringPrims);
  }

  private static final List<GraphAssertion> assertionsForNested =
      List.of(
          new GraphAssertion(ROOT, new String[] {"nested.js"}),
          new GraphAssertion(
              "nested.js", new String[] {"nested.js/f", "nested.js/f/ff", "nested.js/f/ff/fff"}));

  @Test
  public void testNested()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "nested.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForNested);
  }

  private static final List<GraphAssertion> assertionsForInstanceof =
      List.of(new GraphAssertion(ROOT, new String[] {"instanceof.js"}));

  @Test
  public void testInstanceof()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "instanceof.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForInstanceof);
  }

  /*List<GraphAssertion>*/

  @Test
  public void testCrash1()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "crash1.js");
    verifyGraphAssertions(CG, null);
  }

  @Test
  public void testCrash2()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "crash2.js");
    verifyGraphAssertions(CG, null);
  }

  @Test
  public void testLexicalCtor()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "lexical-ctor.js");
    verifyGraphAssertions(CG, null);
  }

  private static final List<GraphAssertion> assertionsForMultivar =
      List.of(
          new GraphAssertion(ROOT, new String[] {"multivar.js"}),
          new GraphAssertion(
              "multivar.js", new String[] {"multivar.js/a", "multivar.js/bf", "multivar.js/c"}));

  @Test
  public void testMultivar()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "multivar.js");
    verifyGraphAssertions(CG, assertionsForMultivar);
  }

  private static final List<GraphAssertion> assertionsForPrototypeContamination =
      List.of(
          new GraphAssertion(ROOT, new String[] {"prototype_contamination_bug.js"}),
          new GraphAssertion("suffix:test1", new String[] {"suffix:foo_of_A"}),
          new GraphAssertion("suffix:test2", new String[] {"suffix:foo_of_B"}));

  @Test
  public void testPrototypeContamination()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "prototype_contamination_bug.js");
    verifyGraphAssertions(CG, assertionsForPrototypeContamination);
    verifyNoEdges(CG, "suffix:test1", "suffix:foo_of_B");
    verifyNoEdges(CG, "suffix:test2", "suffix:foo_of_A");
  }

  @Tag("slow")
  @Test
  public void testStackOverflowOnSsaConversionBug()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "stack_overflow_on_ssa_conversion.js");
    // all we need is for it to finish building CG successfully.
  }

  @Test
  public void testExtJSSwitch()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "extjs_switch.js");
    // all we need is for it to finish building CG successfully.
  }

  @Test
  public void testFunctionDotCall()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph cg = JSCallGraphBuilderUtil.makeScriptCG("tests", "function_call.js");
    assertThat(cg)
        .filteredOn(n -> n.getMethod().getName().toString().equals("$$ call_4"))
        .singleElement()
        .extracting(cg::getSuccNodes, InstanceOfAssertFactories.iterator(CGNode.class))
        .toIterable()
        .extracting(Object::toString)
        .containsExactlyInAnyOrder(
            "Node: <Code body of function Lfunction_call.js/bar> Context: Everywhere",
            "Node: <Code body of function Lfunction_call.js/foo> Context: Everywhere");
  }

  private static final List<GraphAssertion> assertionsForFunctionApply =
      List.of(
          new GraphAssertion(ROOT, new String[] {"function_apply.js"}),
          new GraphAssertion("suffix:function_apply.js", new String[] {"suffix:theOne"}),
          new GraphAssertion("suffix:function_apply.js", new String[] {"suffix:theTwo"}));

  @Test
  public void testFunctionDotApply()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "function_apply.js");
    verifyGraphAssertions(CG, assertionsForFunctionApply);
  }

  private static final List<GraphAssertion> assertionsForFunctionApply2 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"function_apply2.js"}),
          new GraphAssertion("suffix:function_apply2.js", new String[] {"suffix:theThree"}));

  @Test
  public void testFunctionDotApply2()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "function_apply2.js");
    verifyGraphAssertions(CG, assertionsForFunctionApply2);
  }

  private static final List<GraphAssertion> assertionsForFunctionApply3 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"function_apply3.js"}),
          new GraphAssertion("suffix:apply", new String[] {"suffix:foo"}));

  @Test
  public void testFunctionDotApply3()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "function_apply3.js");
    verifyGraphAssertions(CG, assertionsForFunctionApply3);
  }

  private static final List<GraphAssertion> assertionsForWrap1 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"wrap1.js"}),
          new GraphAssertion("suffix:wrap1.js", new String[] {"suffix:i_am_reachable"}));

  @Test
  public void testWrap1()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "wrap1.js");
    verifyGraphAssertions(CG, assertionsForWrap1);
  }

  private static final List<GraphAssertion> assertionsForWrap2 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"wrap2.js"}),
          new GraphAssertion("suffix:wrap2.js", new String[] {"suffix:i_am_reachable"}));

  @Test
  public void testWrap2()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "wrap2.js");
    verifyGraphAssertions(CG, assertionsForWrap2);
  }

  private static final List<GraphAssertion> assertionsForWrap3 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"wrap3.js"}),
          new GraphAssertion("suffix:wrap3.js", new String[] {"suffix:i_am_reachable"}));

  @Test
  public void testWrap3()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "wrap3.js");
    verifyGraphAssertions(CG, assertionsForWrap3);
  }

  private static final List<GraphAssertion> assertionsForComplexCall =
      List.of(
          new GraphAssertion(ROOT, new String[] {"complex_call.js"}),
          new GraphAssertion("suffix:call.js", new String[] {"suffix:f3"}));

  @Test
  public void testComplexCall()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "complex_call.js");
    for (CGNode nd : CG) System.out.println(nd);
    verifyGraphAssertions(CG, assertionsForComplexCall);
  }

  private static final List<GraphAssertion> assertionsForGlobalObj =
      List.of(
          new GraphAssertion(ROOT, new String[] {"global_object.js"}),
          new GraphAssertion("suffix:global_object.js", new String[] {"suffix:biz"}));

  @Test
  public void testGlobalObjPassing()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "global_object.js");
    verifyGraphAssertions(CG, assertionsForGlobalObj);
  }

  private static final List<GraphAssertion> assertionsForGlobalObj2 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"global_object2.js"}),
          new GraphAssertion("suffix:global_object2.js", new String[] {"suffix:foo"}));

  @Test
  public void testGlobalObj2()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "global_object2.js");
    verifyGraphAssertions(CG, assertionsForGlobalObj2);
  }

  private static final List<GraphAssertion> assertionsForReturnThis =
      List.of(
          new GraphAssertion(ROOT, new String[] {"return_this.js"}),
          new GraphAssertion("suffix:return_this.js", new String[] {"suffix:foo"}),
          new GraphAssertion("suffix:return_this.js", new String[] {"suffix:bar"}));

  @Test
  public void testReturnThis()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    SSAPropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "return_this.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForReturnThis);
  }

  private static final List<GraphAssertion> assertionsForReturnThis2 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"return_this2.js"}),
          new GraphAssertion("suffix:return_this2.js", new String[] {"suffix:A"}),
          new GraphAssertion("suffix:return_this2.js", new String[] {"suffix:foo"}),
          new GraphAssertion("suffix:return_this2.js", new String[] {"suffix:test1"}),
          new GraphAssertion("suffix:return_this2.js", new String[] {"suffix:test2"}),
          new GraphAssertion("suffix:test1", new String[] {"suffix:bar1"}),
          new GraphAssertion("suffix:test2", new String[] {"suffix:bar2"}));

  // when using the ObjectSensitivityContextSelector, we additionally know that test1 does not call
  // bar2,
  // and test2 does not call bar1

  @Test
  public void testReturnThis2()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "return_this2.js");
    verifyGraphAssertions(CG, assertionsForReturnThis2);
  }

  private static final List<GraphAssertion> assertionsForArguments =
      List.of(
          new GraphAssertion(ROOT, new String[] {"arguments.js"}),
          new GraphAssertion("suffix:arguments.js", new String[] {"suffix:f"}),
          new GraphAssertion(
              "suffix:f",
              new String[] {
                "!suffix:g1", "!suffix:g2", "suffix:g3",
              }));

  @Test
  public void testArguments()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "arguments.js");
    verifyGraphAssertions(CG, assertionsForArguments);
  }

  private static final List<GraphAssertion> assertionsForFunctionIsAFunction =
      List.of(
          new GraphAssertion(ROOT, new String[] {"Function_is_a_function.js"}),
          new GraphAssertion(
              "suffix:Function_is_a_function.js", new String[] {"suffix:Function_prototype_call"}));

  @Test
  public void testFunctionIsAFunction()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "Function_is_a_function.js");
    verifyGraphAssertions(CG, assertionsForFunctionIsAFunction);
  }

  private static final List<GraphAssertion> assertionsForLexicalBroken =
      List.of(
          new GraphAssertion(ROOT, new String[] {"lexical_broken.js"}),
          new GraphAssertion("suffix:lexical_broken.js", new String[] {"suffix:f"}),
          new GraphAssertion("suffix:f", new String[] {"suffix:g"}));

  @Test
  public void testLexicalBroken()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "lexical_broken.js");
    verifyGraphAssertions(CG, assertionsForLexicalBroken);
  }

  @Test
  public void testDeadPhi()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "dead_phi.js");
  }

  private static final List<GraphAssertion> assertionsForScopingOverwriteFunction =
      List.of(
          new GraphAssertion(ROOT, new String[] {"scoping_test.js"}),
          new GraphAssertion("suffix:scoping_test.js", new String[] {"suffix:i_am_reachable"}));

  @Test
  public void testScopingOverwriteFunction()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "scoping_test.js");
    verifyGraphAssertions(CG, assertionsForScopingOverwriteFunction);
  }

  private static final List<GraphAssertion> assertionsForNestedParamAssign =
      List.of(
          new GraphAssertion(ROOT, new String[] {"nested_assign_to_param.js"}),
          new GraphAssertion(
              "suffix:nested_assign_to_param.js", new String[] {"suffix:i_am_reachable"}));

  @Test
  public void testNestedAssignToParam()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "nested_assign_to_param.js");
    verifyGraphAssertions(CG, assertionsForNestedParamAssign);
  }

  private static final List<GraphAssertion> assertionsForDispatch =
      List.of(
          new GraphAssertion(ROOT, new String[] {"dispatch.js"}),
          new GraphAssertion(
              "dispatch.js", new String[] {"dispatch.js/left_outer", "dispatch.js/right_outer"}),
          new GraphAssertion("dispatch.js/left_outer", new String[] {"dispatch.js/left_inner"}),
          new GraphAssertion("dispatch.js/right_outer", new String[] {"dispatch.js/right_inner"}));

  @Test
  public void testDispatch()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    SSAPropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "dispatch.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForDispatch);
  }

  private static final List<GraphAssertion> assertionsForDispatchSameTarget =
      List.of(
          new GraphAssertion(ROOT, new String[] {"dispatch_same_target.js"}),
          new GraphAssertion(
              "dispatch_same_target.js/f3", new String[] {"dispatch_same_target.js/f4"}));

  @Test
  public void testDispatchSameTarget()
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "dispatch_same_target.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    //    JSCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForDispatchSameTarget);
  }

  private static final List<GraphAssertion> assertionsForForInPrototype =
      List.of(
          new GraphAssertion(ROOT, new String[] {"for_in_prototype.js"}),
          new GraphAssertion(
              "for_in_prototype.js",
              new String[] {"suffix:A", "suffix:reachable", "suffix:also_reachable"}));

  @Test
  public void testForInPrototype()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph cg = JSCallGraphBuilderUtil.makeScriptCG("tests", "for_in_prototype.js");
    verifyGraphAssertions(cg, assertionsForForInPrototype);
  }

  private static final List<GraphAssertion> assertionsForArrayIndexConv =
      List.of(
          new GraphAssertion(ROOT, new String[] {"array_index_conv.js"}),
          new GraphAssertion(
              "array_index_conv.js",
              new String[] {
                "suffix:reachable1", "suffix:reachable2", "suffix:reachable3", "suffix:reachable4"
              }));

  @Test
  public void testArrayIndexConv()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder b =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "array_index_conv.js");
    CallGraph cg = b.makeCallGraph(b.getOptions());
    verifyGraphAssertions(cg, assertionsForArrayIndexConv);
  }

  private static final List<GraphAssertion> assertionsForArrayIndexConv2 =
      List.of(
          new GraphAssertion(ROOT, new String[] {"array_index_conv2.js"}),
          new GraphAssertion("array_index_conv2.js", new String[] {"suffix:invokeOnA"}),
          new GraphAssertion(
              "suffix:invokeOnA",
              new String[] {"suffix:reachable", "suffix:also_reachable", "suffix:reachable_too"}));

  @Test
  public void testArrayIndexConv2()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder b =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "array_index_conv2.js");
    b.setContextSelector(
        new PropertyNameContextSelector(b.getAnalysisCache(), b.getContextSelector()));
    CallGraph cg = b.makeCallGraph(b.getOptions());
    // JSCallGraphUtil.dumpCG(b.getPointerAnalysis(), cg);
    verifyGraphAssertions(cg, assertionsForArrayIndexConv2);
  }

  private static final List<GraphAssertion> assertionsForDateProperty =
      List.of(
          new GraphAssertion(ROOT, new String[] {"date-property.js"}),
          new GraphAssertion("date-property.js", new String[] {"suffix:_fun"}));

  @Test
  public void testDateAsProperty()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "date-property.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    // JSCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForDateProperty);
  }

  private static final List<GraphAssertion> assertionsForDeadCode =
      List.of(
          new GraphAssertion(ROOT, new String[] {"dead.js"}),
          new GraphAssertion("dead.js", new String[] {"suffix:twoReturns"}));

  @Test
  public void testDeadCode()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "dead.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    // JSCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForDeadCode);
  }

  private static final List<GraphAssertion> assertionsForShadow =
      List.of(
          new GraphAssertion(ROOT, new String[] {"shadow_test.js"}),
          new GraphAssertion("shadow_test.js", new String[] {"shadow_test.js/test"}),
          new GraphAssertion("shadow_test.js/test", new String[] {"shadow_test.js/bad"}),
          new GraphAssertion("shadow_test.js/test", new String[] {"shadow_test.js/global_bad"}));

  @Test
  public void testShadow()
      throws IOException, WalaException, IllegalArgumentException, CancelException {
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "shadow_test.js");
    CallGraph cg = builder.makeCallGraph(builder.getOptions());
    verifyGraphAssertions(cg, assertionsForShadow);
  }

  private static final List<GraphAssertion> assertionsForExtend =
      List.of(
          new GraphAssertion(ROOT, new String[] {"extend.js"}),
          new GraphAssertion("extend.js", new String[] {"suffix:bar", "!suffix:foo"}));

  @Test
  public void testExtend()
      throws IOException, WalaException, IllegalArgumentException, CancelException {
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "extend.js");
    CallGraph cg = builder.makeCallGraph(builder.getOptions());
    verifyGraphAssertions(cg, assertionsForExtend);
  }

  @Test
  public void testDeadCatch()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "dead_catch.js");
  }

  @Test
  public void testUglyLoopCrash()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "ssa-crash.js");
  }

  @Test
  public void testTryFinallyCrash()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "try-finally-crash.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
  }

  @Tag("slow")
  @Test
  public void testManyStrings() throws IllegalArgumentException, IOException, WalaException {
    SSAPropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "many-strings.js");
    B.getOptions().setTraceStringConstants(true);
    IProgressMonitor monitor = ProgressMaster.make(new NullProgressMonitor(), 10000, false);
    assertThatThrownBy(
            () -> {
              monitor.beginTask("build CG", 1);
              CallGraph CG = B.makeCallGraph(B.getOptions(), monitor);
              monitor.done();
              CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
            })
        .isInstanceOf(CallGraphBuilderCancelException.class);
  }

  @Test
  public void testTutorialExample()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    SSAPropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "tutorial-example.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    // verifyGraphAssertions(CG, assertionsForDateProperty);
  }

  private static final List<GraphAssertion> assertionsForLoops =
      List.of(
          new GraphAssertion(ROOT, new String[] {"loops.js"}),
          new GraphAssertion("loops.js", new String[] {"loops.js/three", "loops.js/four"}));

  @Disabled("need to fix this.  bug from Sukyoung's group")
  @Test
  public void testLoops()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    SSAPropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "loops.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForLoops);
  }

  private static final List<GraphAssertion> assertionsForPrimitiveStrings =
      List.of(
          new GraphAssertion(ROOT, new String[] {"primitive_strings.js"}),
          new GraphAssertion(
              "primitive_strings.js",
              new String[] {"primitive_strings.js/f1", "primitive_strings.js/f1"}),
          new GraphAssertion(
              "primitive_strings.js/f2", new String[] {"prologue.js/String_prototype_concat"}),
          new GraphAssertion(
              "primitive_strings.js/f1", new String[] {"prologue.js/String_prototype_concat"}));

  @Disabled("need to fix this.  bug from Sukyoung's group")
  @Test
  public void testPrimitiveStrings()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    SSAPropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "primitive_strings.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForPrimitiveStrings);
  }

  List<Pair<String, List<Name>>> renamingAssertions =
      List.of(
          Pair.make("rename-example.js/f", List.of(new Name(9, 7, "x"), new Name(9, 7, "y"))),
          Pair.make(
              "rename-example.js/ff",
              List.of(new Name(11, 10, "x"), new Name(11, 10, "y"), new Name(11, 10, "z"))));

  @Test
  public void testRenaming()
      throws IOException, WalaException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "rename-example.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyNameAssertions(CG, renamingAssertions);
  }

  @Test
  public void testLexicalCatch()
      throws IOException, WalaException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder B =
        JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "lexical_catch.js");
    B.makeCallGraph(B.getOptions());
    // test is just not to crash
  }

  @Test
  public void testThrowCrash()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badthrow.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
  }

  @Test
  public void testNrWrapperCrash()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "nrwrapper.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
  }

  @Test
  public void testFinallyCrash()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "finallycrash.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
  }

  @Test
  public void testForInExpr()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "for_in_expr.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
  }

  private static final List<GraphAssertion> assertionsForComplexFinally =
      List.of(
          new GraphAssertion(ROOT, new String[] {"complex_finally.js"}),
          new GraphAssertion("complex_finally.js", new String[] {"complex_finally.js/e"}),
          new GraphAssertion(
              "complex_finally.js/e",
              new String[] {
                "complex_finally.js/base",
                "complex_finally.js/bad",
                "complex_finally.js/good",
                "complex_finally.js/oo1",
                "complex_finally.js/oo2",
              }));

  @Test
  public void testComplexFinally()
      throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "complex_finally.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForComplexFinally);

    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
  }
}
