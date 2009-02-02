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

import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;

public class TestSimpleCallGraphShape extends TestJSCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestSimpleCallGraphShape.class);
  }

  public void setUp() {
    Util.setTranslatorFactory(new JavaScriptTranslatorFactory.CAstRhinoFactory());
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
      new Object[] { "tests/simple.js/weirder", new String[] { "prologue.js/abs" } } };

  public void testSimple() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "simple.js");
    verifyGraphAssertions(CG, assertionsForSimple);
  }

  private static final Object[][] assertionsForObjects = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/objects.js" } },
      new Object[] { "tests/objects.js",
          new String[] { "tests/objects.js/objects_are_fun", "tests/objects.js/other", "tests/objects.js/something" } },
      new Object[] { "tests/objects.js/other",
          new String[] { "tests/objects.js/something", "tests/objects.js/objects_are_fun/nothing" } },
      new Object[] { "tests/objects.js/objects_are_fun", new String[] { "tests/objects.js/other", "tests/objects.js/whatever" } } };

  public void testObjects() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "objects.js");
    verifyGraphAssertions(CG, assertionsForObjects);
  }

  private static final Object[][] assertionsForInherit = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/inherit.js" } },
      new Object[] {
          "tests/inherit.js",
          new String[] { "tests/inherit.js/objectMasquerading", "tests/inherit.js/objectMasquerading/Rectangle/area",
              "tests/inherit.js/Polygon/shape", "tests/inherit.js/sharedClassObject",
              "tests/inherit.js/sharedClassObject/Rectangle/area" } },
  /*
   * new Object[]{"tests/inherit.js/objectMasquerading", new
   * String[]{"ctor:tests/inherit.js/objectMasquerading/Rectangle"}}, new Object[]{"tests/inherit.js/sharedClassObject",
   * new String[]{"ctor:tests/inherit.js/sharedClassObject/Rectangle"}},
   */
  };

  public void testInherit() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "inherit.js");
    verifyGraphAssertions(CG, assertionsForInherit);
  }

  private static final Object[][] assertionsForNewfn = new Object[][] { new Object[] { ROOT, new String[] { "tests/newfn.js" } },
      new Object[] { "tests/newfn.js", new String[] { "ctor 1/_fromctor", "ctor 2/_fromctor", "ctor 3/_fromctor" } } };

  public void testNewfn() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "newfn.js");
    verifyGraphAssertions(CG, assertionsForNewfn);
  }

  private static final Object[][] assertionsForControlflow = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/control-flow.js" } },
      new Object[] {
          "tests/control-flow.js",
          new String[] { "tests/control-flow.js/testSwitch", "tests/control-flow.js/testDoWhile",
              "tests/control-flow.js/testWhile", "tests/control-flow.js/testFor", "tests/control-flow.js/testReturn" } } };

  public void testControlflow() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "control-flow.js");
    verifyGraphAssertions(CG, assertionsForControlflow);
  }

  private static final Object[][] assertionsForMoreControlflow = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/more-control-flow.js" } },
      new Object[] {
          "tests/more-control-flow.js",
          new String[] { "tests/more-control-flow.js/testSwitch", "tests/more-control-flow.js/testIfConvertedSwitch",
              "tests/more-control-flow.js/testDoWhile", "tests/more-control-flow.js/testWhile",
              "tests/more-control-flow.js/testFor", "tests/more-control-flow.js/testReturn" } } };

  public void testMoreControlflow() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "more-control-flow.js");
    verifyGraphAssertions(CG, assertionsForMoreControlflow);
  }

  private static final Object[][] assertionsForForin = new Object[][] { new Object[] { ROOT, new String[] { "tests/forin.js" } },
      new Object[] { "tests/forin.js", new String[] { "tests/forin.js/testForIn" } },
      new Object[] { "tests/forin.js/testForIn", new String[] { "tests/forin.js/testForIn1", "tests/forin.js/testForIn2" } } };

  public void testForin() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "forin.js");
    verifyGraphAssertions(CG, assertionsForForin);
  }

  private static final Object[][] assertionsForSimpleLexical = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/simple-lexical.js" } },
      new Object[] { "tests/simple-lexical.js", new String[] { "tests/simple-lexical.js/outer" } },
      new Object[] {
          "tests/simple-lexical.js/outer",
          new String[] { "tests/simple-lexical.js/outer/indirect", "tests/simple-lexical.js/outer/inner",
              "tests/simple-lexical.js/outer/inner2", "tests/simple-lexical.js/outer/inner3" } },
      new Object[] { "tests/simple-lexical.js/outer/indirect",
          new String[] { "tests/simple-lexical.js/outer/inner", "tests/simple-lexical.js/outer/inner3" } },
      new Object[] { "tests/simple-lexical.js/outer/inner2",
          new String[] { "tests/simple-lexical.js/outer/inner", "tests/simple-lexical.js/outer/inner3" } } };

  public void testSimpleLexical() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "simple-lexical.js");
    verifyGraphAssertions(CG, assertionsForSimpleLexical);
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
          new String[] { "tests/try.js/targetOne", "tests/try.js/targetTwo", "tests/try.js/three", "tests/try.js/two" } } };

  public void testTry() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "try.js", true);
    verifyGraphAssertions(CG, assertionsForTry);
  }

  private static final Object[][] assertionsForStringOp = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/string-op.js" } },
      new Object[] { "tests/string-op.js", new String[] { "tests/string-op.js/getOp", "tests/string-op.js/plusNum" } } };

  public void testStringOp() throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder B = Util.makeScriptCGBuilder("tests", "string-op.js");
    B.getOptions().setTraceStringConstants(true);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForStringOp);
  }

  private static final Object[][] assertionsForUpward = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/upward.js" } },
      new Object[] {
          "tests/upward.js",
          new String[] { "tests/upward.js/Obj/set", "tests/upward.js/Obj/get", "tests/upward.js/tester1", "tests/upward.js/tester2" } } };

  public void testUpward() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "upward.js");
    verifyGraphAssertions(CG, assertionsForUpward);
  }

  private static final Object[][] assertionsForStringPrims = new Object[][] {
      new Object[] { ROOT, new String[] { "tests/string-prims.js" } },
      new Object[] { "tests/string-prims.js", new String[] { "prologue.js/stringSplit", "prologue.js/toUpperCase" } } };

  public void testStringPrims() throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder B = Util.makeScriptCGBuilder("tests", "string-prims.js");
    B.getOptions().setTraceStringConstants(true);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForStringPrims);
  }
  
  private static final Object[][] assertionsForNested = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/nested.js" } },
    new Object[] { "tests/nested.js", 
                   new String[] { "tests/nested.js/f", 
                                  "tests/nested.js/f/ff", 
                                  "tests/nested.js/f/ff/fff" } 
    } 
  };
  
  public void testNested() throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder B = Util.makeScriptCGBuilder("tests", "nested.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForNested);
  }

  private static final Object[][] assertionsForInstanceof = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/instanceof.js" } }
  };
  
  public void testInstanceof() throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder B = Util.makeScriptCGBuilder("tests", "instanceof.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForInstanceof);
  }

  /*
  private static final Object[][] assertionsForWith = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/with.js" } }
  };
  
  public void testWith() throws IOException, IllegalArgumentException, CancelException {
    PropagationCallGraphBuilder B = Util.makeScriptCGBuilder("tests", "with.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    verifyGraphAssertions(CG, assertionsForWith);
  }
  */
  
  public void testCrash1() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "crash1.js");
    verifyGraphAssertions(CG, null);
  }

  public void testCrash2() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "crash2.js");
    verifyGraphAssertions(CG, null);
  }

  private static final Object[][] assertionsForMultivar = new Object[][] {
    new Object[] { ROOT, new String[] { "tests/multivar.js" } },
    new Object[] { "tests/multivar.js", 
        new String[] { "tests/multivar.js/a",
                       "tests/multivar.js/bf",
                       "tests/multivar.js/c"
        }
    }
  };

  public void testMultivar() throws IOException, IllegalArgumentException, CancelException {
    CallGraph CG = Util.makeScriptCG("tests", "multivar.js");
    verifyGraphAssertions(CG, assertionsForMultivar);
  }

}
