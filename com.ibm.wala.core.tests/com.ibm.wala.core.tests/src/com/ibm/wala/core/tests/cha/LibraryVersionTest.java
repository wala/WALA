/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.cha;

import junit.framework.Assert;

import com.ibm.wala.core.tests.ir.DeterministicIRTest;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.ipa.callgraph.AnalysisScope;

/**
 * Test code that attempts to find the library version from
 * the analysis scope.
 *
 * @author JulianDolby (dolby@us.ibm.com)
 */
public class LibraryVersionTest extends WalaTestCase {
  
  private static final ClassLoader MY_CLASSLOADER = DeterministicIRTest.class.getClassLoader();

  public void testLibraryVersion() {
    AnalysisScope scope = new EMFScopeWrapper(TestConstants.WALA_TESTDATA, "J2SEClassHierarchyExclusions.xml", MY_CLASSLOADER);
    System.err.println("java library version is " + scope.getJavaLibraryVersion());
    Assert.assertTrue(scope.isJava15Libraries()||scope.isJava14Libraries());
  }

}
