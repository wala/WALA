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
package com.ibm.wala.core.tests.cha;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.StringStuff;

public class ExclusionsTest {

  @Test
  public void testExclusions() throws IOException {
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, (new FileProvider()).getFile("GUIExclusions.txt"),
        ExclusionsTest.class.getClassLoader());
    TypeReference buttonRef = TypeReference.findOrCreate(ClassLoaderReference.Application,
        StringStuff.deployment2CanonicalTypeString("java.awt.Button"));
    Assert.assertTrue(scope.getExclusions().contains(buttonRef.getName().toString().substring(1)));
  }
}
