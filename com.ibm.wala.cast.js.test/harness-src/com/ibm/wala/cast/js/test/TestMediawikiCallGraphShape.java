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

import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.CallGraph;

public class TestMediawikiCallGraphShape extends TestJSCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestMediawikiCallGraphShape.class);
  }

  public void setUp() {
    Util.setTranslatorFactory(new JavaScriptTranslatorFactory.CAstRhinoFactory());
  }

  private static final Object[][] assertionsForSwineFlu = new Object[][] {

  };

  public void testSwineFlu() throws IOException, IllegalArgumentException, CancelException {
    URL url = new URL("http://en.wikipedia.org/wiki/2009_swine_flu_outbreak");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForSwineFlu);
  }

}
