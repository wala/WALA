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

import java.net.URL;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

public abstract class TestPrototypeCallGraphShape extends TestJSCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestPrototypeCallGraphShape.class);
  }

  private static final Object[][] assertionsForPrototype = new Object[][] {

  };

  @Ignore("reminder that this no longer works with correlation tracking")
  @Test 
  public void testPrototype() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/prototype.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
    verifyGraphAssertions(CG, assertionsForPrototype);
  }

}
