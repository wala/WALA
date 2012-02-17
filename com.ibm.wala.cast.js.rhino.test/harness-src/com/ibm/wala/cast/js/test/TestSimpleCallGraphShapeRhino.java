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
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.util.CancelException;

public class TestSimpleCallGraphShapeRhino extends TestSimpleCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestSimpleCallGraphShapeRhino.class);
  }

  @Before
  public void setUp() {
    com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
  }

  @Test
  public void test214631() throws IOException, IllegalArgumentException, CancelException {
    JSCFABuilder b = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "214631.js");
    b.makeCallGraph(b.getOptions());
    PointerAnalysis PA = b.getPointerAnalysis();
    // just make sure this does not crash
    computeIkIdToVns(PA);
  }

  @Test
  public void testRewriterDoesNotChangeLablesBug() throws IOException, IllegalArgumentException, CancelException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "rewrite_does_not_change_lables_bug.js");
    // all we need is for it to finish building CG successfully.
  }

  @Test
  public void testRepr() throws IllegalArgumentException, IOException, CancelException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "repr.js");
  }

  @Test
  public void testTranslateToCAstCrash1() throws IllegalArgumentException, IOException, CancelException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "rhino_crash1.js");
  }
  
  @Test
  public void testTranslateToCAstCrash2() throws IllegalArgumentException, IOException, CancelException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "rhino_crash2.js");
  }

  @Test
  public void testTranslateToCAstCrash3() throws IllegalArgumentException, IOException, CancelException {
    JSCallGraphBuilderUtil.makeScriptCG("tests", "rhino_crash3.js");
  }
}
