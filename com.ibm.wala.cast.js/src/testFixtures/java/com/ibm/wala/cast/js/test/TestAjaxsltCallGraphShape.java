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

import static org.junit.Assert.assertNotNull;

import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import java.net.URL;
import org.junit.Test;

public abstract class TestAjaxsltCallGraphShape extends TestJSCallGraphShape {

  private static final Object[][] assertionsForAjaxslt = new Object[][] {};

  @Test
  public void testAjaxslt() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("ajaxslt/test/xslt.html");
    assertNotNull("cannot find resource \"ajaxslt/test/xslt.html\"", url);
    // need to turn off call/apply handling for this to scale; alternatively use 1-CFA
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url, CGBuilderType.ZERO_ONE_CFA_NO_CALL_APPLY);

    verifyGraphAssertions(CG, assertionsForAjaxslt);
  }

  private static final Object[][] assertionsForAjaxpath = new Object[][] {};

  @Test
  public void testAjaxpath() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("ajaxslt/test/xpath.html");
    assertNotNull("cannot find resource \"ajaxslt/test/xpath.html\"", url);
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForAjaxpath);
  }
}
