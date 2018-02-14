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
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
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
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

/**
 * Check properties of a call to clone() in RTA
 */
public class DefaultMethodsTest extends WalaTestCase {

  @Test public void testDefaultMethods() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "LdefaultMethods/DefaultMethods");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);

    // Find node corresponding to main
    TypeReference tm = TypeReference.findOrCreate(ClassLoaderReference.Application, "LdefaultMethods/DefaultMethods");
    MethodReference mm = MethodReference.findOrCreate(tm, "main", "([Ljava/lang/String;)V");
    Assert.assertTrue("expect main node", cg.getNodes(mm).iterator().hasNext());
    CGNode mnode = cg.getNodes(mm).iterator().next();

    // Find node corresponding to Interface1.silly
    TypeReference t1s = TypeReference.findOrCreate(ClassLoaderReference.Application, "LdefaultMethods/Interface1");
    MethodReference t1m = MethodReference.findOrCreate(t1s, "silly", "()I");
    Assert.assertTrue("expect Interface1.silly node", cg.getNodes(t1m).iterator().hasNext());
    CGNode t1node = cg.getNodes(t1m).iterator().next();

    // Check call from main to Interface1.silly
    Assert.assertTrue("should have call site from main to Interface1.silly", cg.getPossibleSites(mnode, t1node).hasNext());

    // Find node corresponding to Interface2.silly
    TypeReference t2s = TypeReference.findOrCreate(ClassLoaderReference.Application, "LdefaultMethods/Interface2");
    MethodReference t2m = MethodReference.findOrCreate(t2s, "silly", "()I");
    Assert.assertTrue("expect Interface2.silly node", cg.getNodes(t2m).iterator().hasNext());
    CGNode t2node = cg.getNodes(t1m).iterator().next();

    // Check call from main to Interface2.silly
    Assert.assertTrue("should have call site from main to Interface2.silly", cg.getPossibleSites(mnode, t2node).hasNext());

    // Find node corresponding to Test.silly
    TypeReference tts = TypeReference.findOrCreate(ClassLoaderReference.Application, "LdefaultMethods/DefaultMethods$Test3");
    MethodReference ttm = MethodReference.findOrCreate(tts, "silly", "()I");
    Assert.assertTrue("expect Interface1.silly node", cg.getNodes(ttm).iterator().hasNext());
    CGNode ttnode = cg.getNodes(ttm).iterator().next();

    // Check call from main to Test3.silly
    Assert.assertTrue("should have call site from main to Test3.silly", cg.getPossibleSites(mnode, ttnode).hasNext());
    
    // Check that IClass.getAllMethods() returns default methods #219.
    TypeReference test1Type = TypeReference.findOrCreate(ClassLoaderReference.Application, "LdefaultMethods/DefaultMethods$Test1");
    IClass test1Class = cha.lookupClass(test1Type);

    Collection<? extends IMethod> allMethods = test1Class.getAllMethods();
    IMethod defaultMethod = test1Class.getMethod(t1m.getSelector());
    Assert.assertTrue("Expecting default methods to show up in IClass.allMethods()", allMethods.contains(defaultMethod));
  }
}
