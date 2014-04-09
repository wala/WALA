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

import org.junit.Before;

import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;

public class TestMediawikiCallGraphShapeRhino extends TestMediawikiCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestMediawikiCallGraphShapeRhino.class);
  }

  @Before
  public void setUp() {
	    com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
  }
  
}
