/*
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import org.junit.Test;

public class TestBoundedFieldBasedCG extends AbstractFieldBasedTest {
  private static final Object[][] assertionsForBound1JS =
      new Object[][] {
        new Object[] {ROOT, new String[] {"suffix:bounded.js"}},
        new Object[] {"suffix:bounded.js", new String[] {"suffix:y", "!suffix:x"}},
        new Object[] {"suffix:call", new String[] {"!suffix:m"}}
      };

  @Test
  public void testBound1Worklist() throws WalaException, Error, CancelException {
    runBoundedTest(
        "tests/fieldbased/bounded.js", assertionsForBound1JS, BuilderType.OPTIMISTIC_WORKLIST, 1);
  }

  private static final Object[][] assertionsForBound2JS =
      new Object[][] {
        new Object[] {ROOT, new String[] {"suffix:bounded.js"}},
        new Object[] {"suffix:bounded.js", new String[] {"suffix:x", "suffix:y"}},
        new Object[] {"suffix:call", new String[] {"!suffix:m"}}
      };

  @Test
  public void testBound2Worklist() throws WalaException, Error, CancelException {
    runBoundedTest(
        "tests/fieldbased/bounded.js", assertionsForBound2JS, BuilderType.OPTIMISTIC_WORKLIST, 2);
  }

  private static final Object[][] assertionsForBound3JS =
      new Object[][] {
        new Object[] {ROOT, new String[] {"suffix:bounded.js"}},
        new Object[] {"suffix:bounded.js", new String[] {"suffix:x", "suffix:y", "suffix:call"}},
        new Object[] {"suffix:call", new String[] {"suffix:m"}},
      };

  @Test
  public void testBound3Worklist() throws WalaException, Error, CancelException {
    runBoundedTest(
        "tests/fieldbased/bounded.js", assertionsForBound3JS, BuilderType.OPTIMISTIC_WORKLIST, 3);
  }
}
