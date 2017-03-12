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

import java.io.IOException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.CPAContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.strings.Atom;

/**
 * Check properties of a call to clone() in RTA
 */
public class CPATest extends WalaTestCase {

  @Test public void cpaTest1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doCPATest("Lcpa/CPATest1", "(Lcpa/CPATest1$N;)Lcpa/CPATest1$N;");
  }
  
  @Test public void cpaTest2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doCPATest("Lcpa/CPATest2", "(Lcpa/CPATest2$N;I)Lcpa/CPATest2$N;");
  }

  private void doCPATest(String testClass, String testIdSignature) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, testClass);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    builder.setContextSelector(new CPAContextSelector(builder.getContextSelector()));
    CallGraph cg = builder.makeCallGraph(options, null);
    
    // Find id
    TypeReference str = TypeReference.findOrCreate(ClassLoaderReference.Application, testClass);
    MethodReference ct = MethodReference.findOrCreate(str, Atom.findOrCreateUnicodeAtom("id"), Descriptor.findOrCreateUTF8(testIdSignature));
    Set<CGNode> idNodes = cg.getNodes(ct);
  
    System.err.println(cg);

    Assert.assertEquals(2, idNodes.size());
  }
}
