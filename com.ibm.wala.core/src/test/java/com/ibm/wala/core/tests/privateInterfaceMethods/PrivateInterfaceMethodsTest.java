/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.privateInterfaceMethods;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
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
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.Collection;
import org.junit.Assert; // added for CallGraphTestUtil, not present in DefaultMethodsTest
import org.junit.Test; // added for CallGraphTestUtil, not present in DefaultMethodsTest not certain

// if necessary

public class PrivateInterfaceMethodsTest extends WalaTestCase {
  @Test
  public void testPrivateInterfaceMethods()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            "wala.testdata11.txt", CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
            scope, cha, "LprivateInterfaceMethods/testArrayReturn/testArrayReturn");

    // System.out.println("entry point: " + entrypoints.iterator().next());

    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg =
        CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);

    // Find node corresponding to main
    TypeReference tm =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application,
            "LprivateInterfaceMethods/testArrayReturn/testArrayReturn");
    MethodReference mm = MethodReference.findOrCreate(tm, "main", "([Ljava/lang/String;)V");
    Assert.assertTrue("expect main node", cg.getNodes(mm).iterator().hasNext());
    // System.out.println("main has next: " + cg.getNodes(mm).iterator().hasNext());
    CGNode mnode = cg.getNodes(mm).iterator().next();

    // should be from main to RetT
    TypeReference t2s =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application,
            "LprivateInterfaceMethods/testArrayReturn/returnArray");
    MethodReference t2m = MethodReference.findOrCreate(t2s, "RetT", "(Ljava/lang/Object;)V");
    // System.out.println("RetTnode: " + cg.getNodes(t2m).iterator());
    Assert.assertTrue("expect RetT node", cg.getNodes(t2m).iterator().hasNext());
    CGNode t2node = cg.getNodes(t2m).iterator().next();

    // Check call from main to RetT(string)
    Assert.assertTrue(
        "should have call site from main to testArrayRetur.retT",
        cg.getPossibleSites(mnode, t2node).hasNext());

    // Find node corresponding to getT(string) called by retT(string) from main
    TypeReference t3s =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application,
            "LprivateInterfaceMethods/testArrayReturn/returnArray");
    MethodReference t3m =
        MethodReference.findOrCreate(t3s, "GetT", "(Ljava/lang/Object;)Ljava/lang/Object;");

    Assert.assertTrue("expect returnArray.GetT() node", cg.getNodes(t3m).iterator().hasNext());
    CGNode t3node = cg.getNodes(t3m).iterator().next();

    // Check call from RetT to GetT
    Assert.assertTrue(
        "should have call site from RetT to returnArray.GetT()",
        cg.getPossibleSites(t2node, t3node).hasNext());

    // check that Iclass.getAllMethods() returns
    TypeReference test1Type =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application,
            "LprivateInterfaceMethods/testArrayReturn/returnArray");
    IClass test1Class = cha.lookupClass(test1Type);

    Collection<? extends IMethod> allMethods = test1Class.getAllMethods();
    IMethod defaultMethod = test1Class.getMethod(t2m.getSelector());
    IMethod privateMethod = test1Class.getMethod(t3m.getSelector());

    Assert.assertTrue(
        "Expecting default methods to show up in IClass.allMethods()",
        allMethods.contains(defaultMethod));
    Assert.assertTrue(
        "Expecting private methods to show up in IClass.allMethods()",
        allMethods.contains(privateMethod));
  }
}