/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.basic;

import org.junit.Test;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * JUnit tests for some {@link OrdinalSet} operations.
 */
public class OrdinalSetTest extends WalaTestCase {

  @Test public void test1() {
    OrdinalSet.unify(OrdinalSet.empty(), OrdinalSet.empty());
  }
}
