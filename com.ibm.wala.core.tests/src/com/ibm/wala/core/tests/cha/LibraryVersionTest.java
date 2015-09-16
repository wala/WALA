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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.core.tests.ir.DeterministicIRTest;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

/**
 * Test code that attempts to find the library version from
 * the analysis scope.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class LibraryVersionTest extends WalaTestCase {
  
  private static final ClassLoader MY_CLASSLOADER = DeterministicIRTest.class.getClassLoader();

  @Test public void testLibraryVersion() throws IOException {
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"), MY_CLASSLOADER);
    System.err.println("java library version is " + scope.getJavaLibraryVersion());
    Assert.assertTrue(scope.isJava18Libraries() || scope.isJava17Libraries() || scope.isJava16Libraries() || scope.isJava15Libraries()||scope.isJava14Libraries());
  }

}
