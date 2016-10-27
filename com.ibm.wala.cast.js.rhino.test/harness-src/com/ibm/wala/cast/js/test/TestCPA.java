/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
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

import org.junit.Before;
import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.CPAContextSelector;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

public class TestCPA {

  @Before
  public void setUp() {
    com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
  }

  @SuppressWarnings("static-access")
  @Test public void testCPA() throws IOException, IllegalArgumentException, CancelException, WalaException {
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "cpa.js");
    builder.setContextSelector(new CPAContextSelector(builder.getContextSelector()));
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    JSCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(builder.getPointerAnalysis(), CG);
  }

}
