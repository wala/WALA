/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.PropertyNameContextSelector;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

public abstract class TestForInLoopHack extends TestJSCallGraphShape {

  @Before
  public void config() {
    JSSourceExtractor.USE_TEMP_NAME = true;
    JSSourceExtractor.DELETE_UPON_EXIT = false;
  }

  @Test public void testPage3WithoutHack() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/page3.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    CAstCallGraphUtil.dumpCG(builder.getCFAContextInterpreter(), builder.getPointerAnalysis(), CG);
  }

  @Test public void testPage3WithHack() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/page3.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    addHackedForInLoopSensitivity(builder);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    CAstCallGraphUtil.dumpCG(builder.getCFAContextInterpreter(), builder.getPointerAnalysis(), CG);
  }

  @Ignore("This test now blows up due to proper handling of the || construct, used in extend().  Should handle this eventually.")
  @Test public void testJQueryWithHack() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/jquery_hacked.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    addHackedForInLoopSensitivity(builder);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    CAstCallGraphUtil.dumpCG(builder.getCFAContextInterpreter(), builder.getPointerAnalysis(), CG);
  }

  /*
  @Test public void testJQueryEx1WithHack() throws IOException, IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/jquery/ex1.html");
    JSCFABuilder builder = Util.makeHTMLCGBuilder(url);
    addHackedForInLoopSensitivity(builder);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    Util.dumpCG(builder.getPointerAnalysis(), CG);
  }
  */
  
  private static final Object[][] assertionsForBadForin = new Object[][] { 
    new Object[] { ROOT, 
      new String[] { "badforin.js" } },
    new Object[] { "badforin.js", 
      new String[] { "badforin.js/testForIn", "badforin.js/_check_obj_foo", "badforin.js/_check_obj_bar", "badforin.js/_check_copy_foo", "badforin.js/_check_copy_bar"} },
    new Object[] { "badforin.js/testForIn",
      new String[] { "badforin.js/testForIn1", "badforin.js/testForIn2" } },
    new Object[] { "badforin.js/_check_obj_foo",
      new String[] { "badforin.js/testForIn1" } },
    new Object[] { "badforin.js/_check_copy_foo",
      new String[] { "badforin.js/testForIn1" } },
    new Object[] { "badforin.js/_check_obj_bar",
      new String[] { "badforin.js/testForIn2" } },
    new Object[] { "badforin.js/_check_copy_bar",
      new String[] { "badforin.js/testForIn2" } }
  };

  @Test public void testBadForInWithoutHack() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badforin.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForBadForin);
  }

  private static final Object[][] assertionsForBadForinHackPrecision = new Object[][] { 
    new Object[] { "badforin.js/_check_obj_foo",
      new String[] { "!badforin.js/testForIn2" } },
    new Object[] { "badforin.js/_check_copy_foo",
      new String[] { "!badforin.js/testForIn2" } },
    new Object[] { "badforin.js/_check_obj_bar",
      new String[] { "!badforin.js/testForIn1" } },
    new Object[] { "badforin.js/_check_copy_bar",
      new String[] { "!badforin.js/testForIn1" } }
  };

  @Test public void testBadForInWithHack() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badforin.js");
    addHackedForInLoopSensitivity(B);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForBadForin);
    verifyGraphAssertions(CG, assertionsForBadForinHackPrecision);
  }

  private static final Object[][] assertionsForbadforin2 = new Object[][] { 
    new Object[] { ROOT, 
      new String[] { "badforin2.js" } },
    new Object[] { "badforin2.js", 
      new String[] { "badforin2.js/testForIn", "badforin2.js/_check_obj_foo", "badforin2.js/_check_obj_bar", "badforin2.js/_check_copy_foo", "badforin2.js/_check_copy_bar"} },
    new Object[] { "badforin2.js/testForIn",
      new String[] { "badforin2.js/testForIn1", "badforin2.js/testForIn2" } },
    new Object[] { "badforin2.js/_check_obj_foo",
      new String[] { "badforin2.js/testForIn1" } },
    new Object[] { "badforin2.js/_check_copy_foo",
      new String[] { "badforin2.js/testForIn1" } },
    new Object[] { "badforin2.js/_check_obj_bar",
      new String[] { "badforin2.js/testForIn2" } },
    new Object[] { "badforin2.js/_check_copy_bar",
      new String[] { "badforin2.js/testForIn2" } }
  };

  @Test public void testbadforin2WithoutHack() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badforin2.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForbadforin2);
  }

  private static final Object[][] assertionsForbadforin2HackPrecision = new Object[][] { 
    new Object[] { "badforin2.js/_check_obj_foo",
      new String[] { "!badforin2.js/testForIn2" } },
    new Object[] { "badforin2.js/_check_copy_foo",
      new String[] { "!badforin2.js/testForIn2" } },
    new Object[] { "badforin2.js/_check_obj_bar",
      new String[] { "!badforin2.js/testForIn1" } },
    new Object[] { "badforin2.js/_check_copy_bar",
      new String[] { "!badforin2.js/testForIn1" } }
  };

  @Test public void testbadforin2WithHack() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badforin2.js");
    addHackedForInLoopSensitivity(B);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForbadforin2);
    verifyGraphAssertions(CG, assertionsForbadforin2HackPrecision);
  }

  @Test public void testForInRecursion() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badforin3.js");
    addHackedForInLoopSensitivity(B);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    CAstCallGraphUtil.dumpCG(B.getCFAContextInterpreter(), B.getPointerAnalysis(), CG);
  }


  /*
  @Test public void testYahooWithoutHack() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCFABuilder B = Util.makeScriptCGBuilder("frameworks", "yahoo.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    Util.dumpCG(B.getPointerAnalysis(), CG);
  }
  */
  
  private static void addHackedForInLoopSensitivity(JSCFABuilder builder) {
    final ContextSelector orig = builder.getContextSelector();
    builder.setContextSelector(new PropertyNameContextSelector(builder.getAnalysisCache(), orig));
  }

}
