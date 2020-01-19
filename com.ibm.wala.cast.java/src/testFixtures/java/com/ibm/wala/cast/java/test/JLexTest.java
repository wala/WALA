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
package com.ibm.wala.cast.java.test;

import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Test;

public abstract class JLexTest extends IRTests {

  public JLexTest() {
    super(null);
  }

  @Override
  protected String singleJavaInputForTest() {
    return "JLex";
  }

  @Test
  public void testJLex() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, new String[] {"LJLex/Main"}, emptyList, false, null);
  }

  @Override
  protected String singleJavaPkgInputForTest(String pkgName) {
    return "";
  }
}
