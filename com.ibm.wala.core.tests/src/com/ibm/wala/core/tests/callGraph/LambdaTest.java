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

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.strings.Atom;

/**
 * Check properties of a call to clone() in RTA
 */
public class LambdaTest extends WalaTestCase {

  @Test public void testBug144() throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "Lbug144/A");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    @SuppressWarnings("unused")
    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);
  }
  
  @Test public void testBug137() throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "Lspecial/A");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);
 
    TypeReference A = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lspecial/A");

    MethodReference ct = MethodReference.findOrCreate(A, Atom.findOrCreateUnicodeAtom("<init>"), Descriptor.findOrCreateUTF8("()V"));
    Set<CGNode> ctnodes = cg.getNodes(ct);    
    Assert.assertEquals(1, ctnodes.size());

    MethodReference ts = MethodReference.findOrCreate(A, Atom.findOrCreateUnicodeAtom("toString"), Descriptor.findOrCreateUTF8("()Ljava/lang/String;"));
    Set<CGNode> tsnodes = cg.getNodes(ts);    
    Assert.assertEquals(1, tsnodes.size());
}
  
  @Test public void testSortingExample() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "Llambda/SortingExample");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);
    
    // Find compareTo
    TypeReference str = TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/String");
    MethodReference ct = MethodReference.findOrCreate(str, Atom.findOrCreateUnicodeAtom("compareTo"), Descriptor.findOrCreateUTF8("(Ljava/lang/String;)I"));
    Set<CGNode> ctnodes = cg.getNodes(ct);
   
    checkCompareToCalls(cg, ctnodes, "id1", 1);
    checkCompareToCalls(cg, ctnodes, "id2", 1);
    checkCompareToCalls(cg, ctnodes, "id3", 2);
    checkCompareToCalls(cg, ctnodes, "id4", 1);
  }

  protected void checkCompareToCalls(CallGraph cg, Set<CGNode> ctnodes, String x, int expected) {
    // Find node corresponding to id1
    TypeReference tid1 = TypeReference.findOrCreate(ClassLoaderReference.Application, "Llambda/SortingExample");
    MethodReference mid1 = MethodReference.findOrCreate(tid1, x, "(I)I");
    Assert.assertTrue("expect " + x + " node", cg.getNodes(mid1).iterator().hasNext());
    CGNode id1node = cg.getNodes(mid1).iterator().next();
    
    // caller of id1 is dynamic from sortForward, and has 1 compareTo
    CGNode sfnode = cg.getPredNodes(id1node).next();
    int count = 0;
    for(CallSiteReference site : Iterator2Iterable.make(sfnode.iterateCallSites())) {
     if (ctnodes.containsAll(cg.getPossibleTargets(sfnode, site))) {
       count++;
     }
    }
    Assert.assertEquals("expected one call to compareTo", expected, count);
    System.err.println("found " + count + " compareTo calls in " + sfnode);
  }
}
