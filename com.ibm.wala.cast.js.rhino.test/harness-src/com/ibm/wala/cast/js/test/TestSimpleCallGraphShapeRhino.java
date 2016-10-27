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

import org.junit.Before;
import org.junit.Test;

import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.Util;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

public class TestSimpleCallGraphShapeRhino extends TestSimpleCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestSimpleCallGraphShapeRhino.class);
  }

  @Override
  @Before
  public void setUp() {
    com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
  }

  @Test
  public void test214631() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCFABuilder b = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "214631.js");
    b.makeCallGraph(b.getOptions());
    b.getPointerAnalysis();
    // just make sure this does not crash
  }

  @Test
  public void testRewriterDoesNotChangeLabelsBug() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "rewrite_does_not_change_lables_bug.js");
    // all we need is for it to finish building CG successfully.
  }

  @Test
  public void testRepr() throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "repr.js");
  }

  @Test
  public void testTranslateToCAstCrash1() throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "rhino_crash1.js");
  }
  
  @Test
  public void testTranslateToCAstCrash2() throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "rhino_crash2.js");
  }

  @Test
  public void testTranslateToCAstCrash3() throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "rhino_crash3.js");
  }
  
  @Test
  public void testNonLoopBreakLabel() throws IllegalArgumentException, IOException, CancelException, WalaException {
	  JSCallGraphBuilderUtil.makeScriptCG("tests", "non_loop_break.js");
  }

  @Test
  public void testForInName() throws IllegalArgumentException, IOException, CancelException, WalaException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "for_in_name.js");
  }

  @Test(expected = WalaException.class)
  public void testParseError() throws IllegalArgumentException, IOException, CancelException, WalaException {
    PropagationCallGraphBuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "portal-example-simple.html");
    B.makeCallGraph(B.getOptions());
    Util.checkForFrontEndErrors(B.getClassHierarchy());
  }

}
