/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.callGraph;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Check properties of a call to clone() in RTA
 */
public class LibModelsTest extends WalaTestCase {

  @Test public void testLibModels() throws ClassHierarchyException, IllegalArgumentException,
      CancelException, IOException {

    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    String libModelsTestClass = "Llibmodels/LibModels";
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        libModelsTestClass);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);

    //System.err.println(cg);

    // Find node corresponding to finalize
    TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Application, libModelsTestClass);
    MethodReference m = MethodReference.findOrCreate(t, "reachable1", "()V");
    Assert.assertTrue("expect reachable1 from addShutdownHook",
        cg.getNodes(m).iterator().hasNext());
    MethodReference m2 = MethodReference.findOrCreate(t, "reachable2", "()V");
    Assert.assertTrue("expect reachable2 from uncaught exception handler",
        cg.getNodes(m2).iterator().hasNext());
  }
}
