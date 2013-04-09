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
import java.net.URL;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

public abstract class TestMediawikiCallGraphShape extends TestJSCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestMediawikiCallGraphShape.class);
  }

  private static final Object[][] assertionsForSwineFlu = new Object[][] {

  };

  @Ignore("not terminating; Julian, take a look?")
  @Test public void testSwineFlu() throws IOException, IllegalArgumentException, CancelException, WalaException {
    URL url = new URL("http://en.wikipedia.org/wiki/2009_swine_flu_outbreak");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForSwineFlu);
  }

}
