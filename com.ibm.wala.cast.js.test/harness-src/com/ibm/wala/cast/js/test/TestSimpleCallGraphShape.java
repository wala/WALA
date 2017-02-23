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
package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.PropertyNameContextSelector;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.ProgressMaster;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Collection;

public abstract class TestSimpleCallGraphShape extends TestJSCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestSimpleCallGraphShape.class);
  }

  protected static final Object[][] assertionsForArgs = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/args.js" } },
    new Object[] {
        "tests/args.js",
        new String[] { "tests/args.js/a" } },
    new Object[] { "tests/args.js/a", new String[] { "tests/args.js/x", "tests/args.js/y" } } };

@Test public void testArgs() throws IOException, IllegalArgumentException, CancelException, WalaException {
  CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "args.js");
  verifyGraphAssertions(CG, assertionsForArgs);
}

  protected static final Object[][] assertionsForSimple = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/simple.js" } },
      new Object[] {
          "tests/simple.js",
          new String[] { "tests/simple.js/bad", "tests/simple.js/silly", "tests/simple.js/fib", "tests/simple.js/stranger",
              "tests/simple.js/trivial", "tests/simple.js/rubbish", "tests/simple.js/weirder" } },
      new Object[] { "tests/simple.js/trivial", new String[] { "tests/simple.js/trivial/inc" } },
      new Object[] { "tests/simple.js/rubbish",
          new String[] { "tests/simple.js/weirder", "tests/simple.js/stranger", "tests/simple.js/rubbish" } },
      new Object[] { "tests/simple.js/fib", new String[] { "tests/simple.js/fib" } },
      new Object[] { "tests/simple.js/weirder", new String[] { "prologue.js/Math_abs" } } };

  @Test
  public void testSimple() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "simple.js");
    verifyGraphAssertions(CG, assertionsForSimple);
  }

  private static final Object[][] assertionsForObjects = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/objects.js" } },
      new Object[] { "tests/objects.js",
          new String[] { "tests/objects.js/objects_are_fun", "tests/objects.js/other", "tests/objects.js/something" } },
      new Object[] { "tests/objects.js/other",
          new String[] { "tests/objects.js/something", "tests/objects.js/objects_are_fun/nothing" } },
      new Object[] { "tests/objects.js/objects_are_fun", new String[] { "tests/objects.js/other", "tests/objects.js/whatever" } } };

  @Test
  public void testObjects() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "objects.js");
    verifyGraphAssertions(CG, assertionsForObjects);
  }

  private static final Object[][] cfgAssertionsForInherit = new Object[][] {
    new Object[]{"ctor:tests/inherit.js/objectMasquerading/Rectangle",
        new int[][]{{1,7},{2},{3,7},{4,7},{5,6},{7},{7}}
    },
    new Object[]{"ctor:tests/inherit.js/sharedClassObject/Rectangle",
        new int[][]{{1,7},{2},{3,7},{4,7},{5,6},{7},{7}}
    }
  };
  
  private static final Object[][] assertionsForInherit = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/inherit.js" } },
      new Object[] {
          "tests/inherit.js",
          new String[] { "tests/inherit.js/objectMasquerading", "tests/inherit.js/objectMasquerading/Rectangle/area",
              "tests/inherit.js/Polygon/shape", "tests/inherit.js/sharedClassObject",
              "tests/inherit.js/sharedClassObject/Rectangle/area" } },
      new Object[]{
          "tests/inherit.js/objectMasquerading", 
          new String[]{"ctor:tests/inherit.js/objectMasquerading/Rectangle"}}, 
      new Object[]{
          "ctor:tests/inherit.js/objectMasquerading/Rectangle" ,
          new String[]{"tests/inherit.js/objectMasquerading/Rectangle"}}, 
      new Object[]{"tests/inherit.js/objectMasquerading/Rectangle",
          new String[]{"tests/inherit.js/Polygon"}}, 
      new Object[]{
          "tests/inherit.js/sharedClassObject", 
          new String[]{"ctor:tests/inherit.js/sharedClassObject/Rectangle"}},
      new Object[]{"ctor:tests/inherit.js/sharedClassObject/Rectangle",
          new String[]{"tests/inherit.js/sharedClassObject/Rectangle"}}
  };

  @Test
  public void testInherit() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "inherit.js");
    verifyGraphAssertions(CG, assertionsForInherit);
    verifyCFGAssertions(CG, cfgAssertionsForInherit);
  }

  private static final Object[][] assertionsForNewfn = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/newfn.js" } },
      new Object[] { "tests/newfn.js",
          new String[] { "suffix:ctor$1/_fromctor", "suffix:ctor$2/_fromctor", "suffix:ctor$3/_fromctor" } } };

  @Test
  public void testNewfn() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "newfn.js");
    verifyGraphAssertions(CG, assertionsForNewfn);
  }

  private static final Object[][] assertionsForControlflow = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/control-flow.js" } },
      new Object[] {
          "tests/control-flow.js",
          new String[] { "tests/control-flow.js/testSwitch", "tests/control-flow.js/testDoWhile",
              "tests/control-flow.js/testWhile", "tests/control-flow.js/testFor", "tests/control-flow.js/testReturn" } } };

  @Test
  public void testControlflow() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "control-flow.js");
    verifyGraphAssertions(CG, assertionsForControlflow);
  }

  private static final Object[][] assertionsForMoreControlflow = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/more-control-flow.js" } },
      new Object[] {
          "tests/more-control-flow.js",
          new String[] { "tests/more-control-flow.js/testSwitch", "tests/more-control-flow.js/testIfConvertedSwitch",
              "tests/more-control-flow.js/testDoWhile", "tests/more-control-flow.js/testWhile",
              "tests/more-control-flow.js/testFor", "tests/more-control-flow.js/testReturn" } } };

  @Test
  public void testMoreControlflow() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "more-control-flow.js");
    verifyGraphAssertions(CG, assertionsForMoreControlflow);
  }

  private static final Object[][] assertionsForForin = new Object[][] { new Object[] { ROOT, new String[] { "tests/forin.js" } },
      new Object[] { "tests/forin.js", new String[] { "tests/forin.js/testForIn" } },
      new Object[] { "tests/forin.js/testForIn", new String[] { "tests/forin.js/testForIn1", "tests/forin.js/testForIn2" } } };

  @Test
  public void testForin() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "forin.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
//    JSCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForForin);
  }

  private static final Object[][] assertionsForSimpleLexical = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/simple-lexical.js" } },
      new Object[] { "tests/simple-lexical.js", new String[] { "tests/simple-lexical.js/outer" } },
      new Object[] {
          "tests/simple-lexical.js/outer",
          new String[] { "tests/simple-lexical.js/outer/indirect", "tests/simple-lexical.js/outer/inner",
              "tests/simple-lexical.js/outer/inner2", "tests/simple-lexical.js/outer/inner3" } },
      new Object[] { "tests/simple-lexical.js/outer/inner2",
          new String[] { "tests/simple-lexical.js/outer/inner", "tests/simple-lexical.js/outer/inner3" } },
      new Object[] { "tests/simple-lexical.js/outer/indirect",
          new String[] { "tests/simple-lexical.js/outer/inner", "tests/simple-lexical.js/outer/inner3" } } };

  @Test
  public void testSimpleLexical() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "simple-lexical.js");
    verifyGraphAssertions(CG, assertionsForSimpleLexical);
  }

  @Test
  public void testRecursiveLexical() throws IOException, IllegalArgumentException, CancelException, WalaException {
    // just checking that we have a sufficient bailout to ensure termination
    JSCallGraphBuilderUtil.makeScriptCG("tests", "recursive_lexical.js");
  }
  
  private static final Object[][] assertionsForLexicalMultiple = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/lexical_multiple_calls.js" } },
    new Object[] { "suffix:lexical_multiple_calls.js", new String[] { "suffix:reachable1" } }, 
    new Object[] { "suffix:lexical_multiple_calls.js", new String[] { "suffix:reachable2" } }};
  
  @Test
  public void testLexicalMultiple() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "lexical_multiple_calls.js");
    verifyGraphAssertions(CG, assertionsForLexicalMultiple);
  }

  
  private static final Object[][] assertionsForTry = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/try.js" } },
      new Object[] { "tests/try.js",
          new String[] { "tests/try.js/tryCatch", "tests/try.js/tryFinally", "tests/try.js/tryCatchFinally" } },
      new Object[] { "tests/try.js/tryCatch",
          new String[] { "tests/try.js/targetOne", "tests/try.js/targetTwo", "tests/try.js/two" } },
      new Object[] { "tests/try.js/tryFinally",
          new String[] { "tests/try.js/targetOne", "tests/try.js/targetTwo", "tests/try.js/two" } },
      new Object[] { "tests/try.js/tryCatchFinally",
          new String[] { "tests/try.js/targetOne", "tests/try.js/targetTwo", "tests/try.js/three", "tests/try.js/two" } },
      new Object[] { "tests/try.js/tryCatchTwice",
          new String[] { "tests/try.js/targetOne", "tests/try.js/targetTwo", "tests/try.js/three", "tests/try.js/two" } },
      new Object[] { "tests/try.js/testRet",
          new String[] { "tests/try.js/three", "tests/try.js/two" } }
  };

  @Test
  public void testTry() throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "try.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    /*
    boolean x = CAstCallGraphUtil.AVOID_DUMP;
    CAstCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    CAstCallGraphUtil.AVOID_DUMP = x;
    */
    verifyGraphAssertions(CG, assertionsForTry);
  }

  private static final Object[][] assertionsForStringOp = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/string-op.js" } },
      new Object[] { "tests/string-op.js", new String[] { "tests/string-op.js/getOp", "tests/string-op.js/plusNum" } } };

  @Test
  public void testStringOp() throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "string-op.js");
    B.getOptions().setTraceStringConstants(true);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForStringOp);
  }

  private static final Object[][] assertionsForUpward = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/upward.js" } },
      new Object[] {
          "tests/upward.js",
          new String[] { "tests/upward.js/Obj/setit", "tests/upward.js/Obj/getit", "tests/upward.js/tester1",
              "tests/upward.js/tester2" } } };

  @Test
  public void testUpward() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "upward.js");
    verifyGraphAssertions(CG, assertionsForUpward);
  }

  private static final Object[][] assertionsForStringPrims = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/string-prims.js" } },
      new Object[] { "tests/string-prims.js", new String[] { "prologue.js/String_prototype_split", "prologue.js/String_prototype_toUpperCase" } } };

  @Test
  public void testStringPrims() throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "string-prims.js");
    B.getOptions().setTraceStringConstants(true);
    CallGraph CG = B.makeCallGraph(B.getOptions());
//    JSCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForStringPrims);
  }

  private static final Object[][] assertionsForNested = new Object[][] { new Object[] { ROOT, new String[] { "tests/nested.js" } },
      new Object[] { "tests/nested.js", new String[] { "tests/nested.js/f", "tests/nested.js/f/ff", "tests/nested.js/f/ff/fff" } } };

  @Test
  public void testNested() throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "nested.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForNested);
  }

  private static final Object[][] assertionsForInstanceof = new Object[][] { new Object[] { ROOT,
      new String[] { "tests/instanceof.js" } } };

  @Test
  public void testInstanceof() throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "instanceof.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForInstanceof);
  }

  /*
   * private static final Object[][] assertionsForWith = new Object[][] { new
   * Object[] { ROOT, new String[] { "tests/with.js" } } };
   * 
   * @Test public void testWith() throws IOException, IllegalArgumentException,
   * CancelException { PropagationCallGraphBuilder B =
   * Util.makeScriptCGBuilder("tests", "with.js"); CallGraph CG =
   * B.makeCallGraph(B.getOptions()); verifyGraphAssertions(CG,
   * assertionsForWith); }
   */

  @Test
  public void testCrash1() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "crash1.js");
    verifyGraphAssertions(CG, null);
  }

  @Test
  public void testCrash2() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "crash2.js");
    verifyGraphAssertions(CG, null);
  }

  @Test
  public void testLexicalCtor() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "lexical-ctor.js");
    verifyGraphAssertions(CG, null);
  }

  private static final Object[][] assertionsForMultivar = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/multivar.js" } },
      new Object[] { "tests/multivar.js", new String[] { "tests/multivar.js/a", "tests/multivar.js/bf", "tests/multivar.js/c" } } };

  @Test
  public void testMultivar() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "multivar.js");
    verifyGraphAssertions(CG, assertionsForMultivar);
  }

  private static final Object[][] assertionsForPrototypeContamination = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/prototype_contamination_bug.js" } },
      new Object[] { "suffix:test1", new String[] { "suffix:foo_of_A" } },
      new Object[] { "suffix:test2", new String[] { "suffix:foo_of_B" } } };

  @Test
  public void testProtoypeContamination() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "prototype_contamination_bug.js");
    verifyGraphAssertions(CG, assertionsForPrototypeContamination);
    verifyNoEdges(CG, "suffix:test1", "suffix:foo_of_B");
    verifyNoEdges(CG, "suffix:test2", "suffix:foo_of_A");
  }

  @Test
  public void testStackOverflowOnSsaConversionBug() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "stack_overflow_on_ssa_conversion.js");
    // all we need is for it to finish building CG successfully.
  }

  @Test
  public void testExtJSSwitch() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "extjs_switch.js");
    // all we need is for it to finish building CG successfully.
  }


  @Test
  public void testFunctionDotCall() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph cg = JSCallGraphBuilderUtil.makeScriptCG("tests", "function_call.js");
    for (CGNode n : cg) {
      if (n.getMethod().getName().toString().equals("call4")) {
        Assert.assertEquals(2, cg.getSuccNodeCount(n));
        // ugh
        List<CGNode> succs = Iterator2Collection.toList(cg.getSuccNodes(n));
        Assert
            .assertEquals(
                "[Node: <Code body of function Ltests/function_call.js/foo> Context: Everywhere, Node: <Code body of function Ltests/function_call.js/bar> Context: Everywhere]",
                succs.toString());
      }
    }
  }

  private static final Object[][] assertionsForFunctionApply = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/function_apply.js" } },
    new Object[] { "suffix:function_apply.js", new String[] { "suffix:theOne" } }, 
    new Object[] { "suffix:function_apply.js", new String[] { "suffix:theTwo" } } };


  @Test
  public void testFunctionDotApply() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "function_apply.js");
    verifyGraphAssertions(CG, assertionsForFunctionApply);
  }

  private static final Object[][] assertionsForFunctionApply2 = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/function_apply2.js" } },
    new Object[] { "suffix:function_apply2.js", new String[] { "suffix:theThree" } } }; 

  @Test
  public void testFunctionDotApply2() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "function_apply2.js");
    verifyGraphAssertions(CG, assertionsForFunctionApply2);
  }

  private static final Object[][] assertionsForFunctionApply3 = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/function_apply3.js" } },
    new Object[] { "suffix:apply", new String[] { "suffix:foo" } } }; 

  @Test
  public void testFunctionDotApply3() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "function_apply3.js");
    verifyGraphAssertions(CG, assertionsForFunctionApply3);
  }

  private static final Object[][] assertionsForWrap1 = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/wrap1.js" } },
    new Object[] { "suffix:wrap1.js", new String[] { "suffix:i_am_reachable" } } };

  @Test
  public void testWrap1() throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "wrap1.js");
    verifyGraphAssertions(CG, assertionsForWrap1);
  }

  private static final Object[][] assertionsForWrap2 = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/wrap2.js" } },
    new Object[] { "suffix:wrap2.js", new String[] { "suffix:i_am_reachable" } } };

  @Test
  public void testWrap2() throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "wrap2.js");
    verifyGraphAssertions(CG, assertionsForWrap2);
  }

  private static final Object[][] assertionsForWrap3 = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/wrap3.js" } },
    new Object[] { "suffix:wrap3.js", new String[] { "suffix:i_am_reachable" } } };

  @Test
  public void testWrap3() throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "wrap3.js");
    verifyGraphAssertions(CG, assertionsForWrap3);
  }

  private static final Object[][] assertionsForComplexCall = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/complex_call.js" } },
    new Object[] { "suffix:call.js", new String[] { "suffix:f3" } } };

  @Test
  public void testComplexCall() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "complex_call.js");
    for(CGNode nd : CG)
      System.out.println(nd);
    verifyGraphAssertions(CG, assertionsForComplexCall);
  }


  private static final Object[][] assertionsForGlobalObj = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/global_object.js" } },
    new Object[] { "suffix:global_object.js", new String[] { "suffix:biz" } } };

  @Test
  public void testGlobalObjPassing() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "global_object.js");
    verifyGraphAssertions(CG, assertionsForGlobalObj);
  }
  
  private static final Object[][] assertionsForGlobalObj2 = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/global_object2.js" } },
    new Object[] { "suffix:global_object2.js", new String[] { "suffix:foo" } } };

  @Test
  public void testGlobalObj2() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "global_object2.js");
    verifyGraphAssertions(CG, assertionsForGlobalObj2);
  }
 
  
  private static final Object[][] assertionsForReturnThis = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/return_this.js" } },
    new Object[] { "suffix:return_this.js", new String[] { "suffix:foo" } }, 
    new Object[] { "suffix:return_this.js", new String[] { "suffix:bar" } } };

  @Test
  public void testReturnThis() throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "return_this.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
//    JSCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForReturnThis);
  }
  
  private static final Object[][] assertionsForReturnThis2 = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/return_this2.js" } },
    new Object[] { "suffix:return_this2.js", new String[] { "suffix:A" } }, 
    new Object[] { "suffix:return_this2.js", new String[] { "suffix:foo" } }, 
    new Object[] { "suffix:return_this2.js", new String[] { "suffix:test1" } }, 
    new Object[] { "suffix:return_this2.js", new String[] { "suffix:test2" } }, 
    new Object[] { "suffix:test1", new String[] { "suffix:bar1" } }, 
    new Object[] { "suffix:test2", new String[] { "suffix:bar2" } } 
  };
  // when using the ObjectSensitivityContextSelector, we additionally know that test1 does not call bar2,
  // and test2 does not call bar1

  @Test
  public void testReturnThis2() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "return_this2.js");
    verifyGraphAssertions(CG, assertionsForReturnThis2);
  }
  
  private static final Object[][] assertionsForArguments = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/arguments.js" } },
    new Object[] { "suffix:arguments.js", new String[] { "suffix:f" } },
    new Object[] { "suffix:f", new String[] { "!suffix:g1", "!suffix:g2", "suffix:g3", } }
  };
  
  @Test
  public void testArguments() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "arguments.js");
    verifyGraphAssertions(CG, assertionsForArguments);
  }
  
  private static final Object[][] assertionsForFunctionIsAFunction = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/Function_is_a_function.js" } },
    new Object[] { "suffix:Function_is_a_function.js", new String[] { "suffix:Function_prototype_call" } } }; 

  @Test
  public void testFunctionIsAFunction() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "Function_is_a_function.js");
    verifyGraphAssertions(CG, assertionsForFunctionIsAFunction);
  }
  
  private static final Object[][] assertionsForLexicalBroken = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/lexical_broken.js" } },
    new Object[] { "suffix:lexical_broken.js", new String[] { "suffix:f" } },
    new Object[] { "suffix:f", new String[] { "suffix:g" } }
  };
  
  @Test
  public void testLexicalBroken() throws IOException, IllegalArgumentException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "lexical_broken.js");
    verifyGraphAssertions(CG, assertionsForLexicalBroken);
  }
  
  @Test
  public void testDeadPhi() throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "dead_phi.js");
  }

  private static final Object[][] assertionsForScopingOverwriteFunction = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/scoping_test.js" } },
    new Object[] { "suffix:scoping_test.js", new String[] { "suffix:i_am_reachable" } } 
  };

  @Test
  public void testScopingOverwriteFunction() throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "scoping_test.js");
    verifyGraphAssertions(CG, assertionsForScopingOverwriteFunction);
  }
  
  private static final Object[][] assertionsForNestedParamAssign = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/nested_assign_to_param.js" } },
    new Object[] { "suffix:nested_assign_to_param.js", new String[] { "suffix:i_am_reachable" } } 
  };
  
  @Test
  public void testNestedAssignToParam() throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph CG = JSCallGraphBuilderUtil.makeScriptCG("tests", "nested_assign_to_param.js");
    verifyGraphAssertions(CG, assertionsForNestedParamAssign);
  }

  private static final Object[][] assertionsForDispatch = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/dispatch.js" } },
    new Object[] { "tests/dispatch.js", new String[] { "tests/dispatch.js/left_outer", "tests/dispatch.js/right_outer" } },
    new Object[] { "tests/dispatch.js/left_outer", new String[]{ "tests/dispatch.js/left_inner" } },
    new Object[] { "tests/dispatch.js/right_outer", new String[]{ "tests/dispatch.js/right_inner" } }
  };

  @Test
  public void testDispatch() throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "dispatch.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
//    JSCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForDispatch);
  }

  private static final Object[][] assertionsForDispatchSameTarget = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/dispatch_same_target.js" } },
    new Object[] { "tests/dispatch_same_target.js/f3", new String[] { "tests/dispatch_same_target.js/f4" } } 
  };


  @Test
  public void testDispatchSameTarget() throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "dispatch_same_target.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
//    JSCallGraphUtil.AVOID_DUMP = false;
//    JSCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForDispatchSameTarget);
  }
  
  
  private static final Object[][] assertionsForForInPrototype = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/for_in_prototype.js" } },
    new Object[] { "tests/for_in_prototype.js", new String[] { "suffix:A",
                                                               "suffix:reachable",
                                                               "suffix:also_reachable" } }
  };
  
  @Test
  public void testForInPrototype() throws IllegalArgumentException, IOException, CancelException, WalaException {
    CallGraph cg = JSCallGraphBuilderUtil.makeScriptCG("tests", "for_in_prototype.js");
    verifyGraphAssertions(cg, assertionsForForInPrototype);
  }
  
  private static final Object[][] assertionsForArrayIndexConv = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/array_index_conv.js" } },
    new Object[] { "tests/array_index_conv.js", new String[] { "suffix:reachable1",
                                                               "suffix:reachable2",
                                                               "suffix:reachable3",
                                                               "suffix:reachable4" } }
  };
  
  @Test
  public void testArrayIndexConv() throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder b = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "array_index_conv.js");
    CallGraph cg = b.makeCallGraph(b.getOptions());
    verifyGraphAssertions(cg, assertionsForArrayIndexConv);
  }

  private static final Object[][] assertionsForArrayIndexConv2 = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/array_index_conv2.js" } },
    new Object[] { "tests/array_index_conv2.js", new String[] { "suffix:invokeOnA" } },
    new Object[] { "suffix:invokeOnA", new String[] { "suffix:reachable",
                                                      "suffix:also_reachable",
                                                      "suffix:reachable_too" } }
  };
  
  @Test
  public void testArrayIndexConv2() throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder b = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "array_index_conv2.js");
    b.setContextSelector(new PropertyNameContextSelector(b.getAnalysisCache(), b.getContextSelector()));
    CallGraph cg = b.makeCallGraph(b.getOptions());
    //JSCallGraphUtil.AVOID_DUMP = false;
    //JSCallGraphUtil.dumpCG(b.getPointerAnalysis(), cg);
    verifyGraphAssertions(cg, assertionsForArrayIndexConv2);
  }

  private static final Object[][] assertionsForDateProperty = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/date-property.js" } },
    new Object[] { "tests/date-property.js", new String[] { "suffix:_fun" } }
  };

    @Test
  public void testDateAsProperty() throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "date-property.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    //JSCallGraphUtil.AVOID_DUMP = false;
    //JSCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForDateProperty);
  }
    private static final Object[][] assertionsForDeadCode = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/dead.js" } },
      new Object[] { "tests/dead.js", new String[] { "suffix:twoReturns" } }
    };

    @Test
    public void testDeadCode() throws IllegalArgumentException, IOException, CancelException, WalaException {
      PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "dead.js");
      CallGraph CG = B.makeCallGraph(B.getOptions());
      //JSCallGraphUtil.AVOID_DUMP = false;
      //JSCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
      verifyGraphAssertions(CG, assertionsForDeadCode);
    }
    
    private static final Object[][] assertionsForShadow = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/shadow_test.js" } },
      new Object[] { "tests/shadow_test.js", new String[] { "tests/shadow_test.js/test" } },
      new Object[] { "tests/shadow_test.js/test", new String[] { "tests/shadow_test.js/bad" } },
      new Object[] { "tests/shadow_test.js/test", new String[] { "tests/shadow_test.js/global_bad" } }
    };
    
    @Test
    public void testShadow() throws IOException, WalaException, IllegalArgumentException, CancelException {
      JSCFABuilder builder = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "shadow_test.js");
      CallGraph cg = builder.makeCallGraph(builder.getOptions());
      verifyGraphAssertions(cg, assertionsForShadow);
    }

    private static final Object[][] assertionsForExtend = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/extend.js" } },
      new Object[] { "tests/extend.js", new String[] { "suffix:bar", "!suffix:foo" } }
    };
    
    @Test
    public void testExtend() throws IOException, WalaException, IllegalArgumentException, CancelException {
      JSCFABuilder builder = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "extend.js");
      CallGraph cg = builder.makeCallGraph(builder.getOptions());
      verifyGraphAssertions(cg, assertionsForExtend);
    }

    @Test
    public void testDeadCatch() throws IllegalArgumentException, IOException, CancelException, WalaException {
      JSCallGraphBuilderUtil.makeScriptCG("tests", "dead_catch.js");
    }

    @Test
    public void testUglyLoopCrash() throws IllegalArgumentException, IOException, CancelException, WalaException {
      JSCallGraphBuilderUtil.makeScriptCG("tests", "ssa-crash.js");
    }

    @Test
    public void testTryFinallyCrash() throws IllegalArgumentException, IOException, CancelException, WalaException {      
      JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "try-finally-crash.js");
      CallGraph CG = B.makeCallGraph(B.getOptions());
      boolean save = CAstCallGraphUtil.AVOID_DUMP;
      //CAstCallGraphUtil.AVOID_DUMP = false;
      CAstCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
      CAstCallGraphUtil.AVOID_DUMP = save;
    }


    @Test(expected = CallGraphBuilderCancelException.class)
    public void testManyStrings() throws IllegalArgumentException, IOException, CancelException, WalaException {
      SSAPropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "many-strings.js");
      B.getOptions().setTraceStringConstants(true);
      ProgressMaster monitor = ProgressMaster.make(new NullProgressMonitor(), 10000, false);
      monitor.beginTask("build CG", 1);
      CallGraph CG = B.makeCallGraph(B.getOptions(), monitor);
      monitor.done();
      CAstCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    }

  @Test
  public void testTutorialExample() throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "tutorial-example.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    // verifyGraphAssertions(CG, assertionsForDateProperty);
  }

  private static final Object[][] assertionsForLoops = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/loops.js" } },
    new Object[] { "tests/loops.js", new String[] { "tests/loops.js/three",  "tests/loops.js/four"} }
  };

  @Ignore("need to fix this.  bug from Sukyoung's group")
  @Test
  public void testLoops() throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "loops.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    boolean x = CAstCallGraphUtil.AVOID_DUMP;
    CAstCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    CAstCallGraphUtil.AVOID_DUMP = x;
    verifyGraphAssertions(CG, assertionsForLoops);
  }

  private static final Object[][] assertionsForPrimitiveStrings = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/primitive_strings.js" } },
    new Object[] { "tests/primitive_strings.js", new String[] { "tests/primitive_strings.js/f1", "tests/primitive_strings.js/f1"} },
    new Object[] { "tests/primitive_strings.js/f2", new String[] { "prologue.js/String_prototype_concat" } },
    new Object[] { "tests/primitive_strings.js/f1", new String[] { "prologue.js/String_prototype_concat" } },
  };

  @Ignore("need to fix this.  bug from Sukyoung's group")
  @Test
  public void testPrimitiveStrings() throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "primitive_strings.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    boolean x = CAstCallGraphUtil.AVOID_DUMP;
    CAstCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(B.getPointerAnalysis(), CG);
    CAstCallGraphUtil.AVOID_DUMP = x;
    verifyGraphAssertions(CG, assertionsForPrimitiveStrings);
  }

  Object[][] renamingAssertions =  { 
      { "tests/rename-example.js/f", new Name[]{ new Name(9, 7, "x"), new Name(9, 7, "y") } },
      { "tests/rename-example.js/ff", new Name[]{ new Name(11, 10, "x"), new Name(11, 10, "y"), new Name(11, 10, "z") } }
  };
  
  @Test
  public void testRenaming() throws IOException, WalaException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "rename-example.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyNameAssertions(CG, renamingAssertions);
  }

}
