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
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;

public class TestAjaxsltCallGraphShape extends TestJSCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestAjaxsltCallGraphShape.class);
  }

  private static final Object[][] assertionsForAjaxslt = new Object[][] {

  };

  public void testAjaxslt() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("ajaxslt/test/xslt.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForAjaxslt);
  }

  private static final Object[][] assertionsForAjaxpath = new Object[][] {

  };

  public void testAjaxpath() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("ajaxslt/test/xpath.html");
    CallGraph CG = Util.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForAjaxpath);
  }

}
