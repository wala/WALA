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
package com.ibm.wala.core.tests.jdk11.nestmates;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class NestmatesTest extends WalaTestCase {
  @Test
  public void testPrivateInterfaceMethods()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            "wala.testdata.txt", CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Lnestmates/TestNestmates");

    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);

    // Find node corresponding to main
    TypeReference tm =
        TypeReference.findOrCreate(ClassLoaderReference.Application, "Lnestmates/TestNestmates");
    MethodReference mm = MethodReference.findOrCreate(tm, "main", "([Ljava/lang/String;)V");
    Assert.assertTrue("expect main node", cg.getNodes(mm).iterator().hasNext());
    CGNode mnode = cg.getNodes(mm).iterator().next();

    // should be from main to Triple()
    TypeReference t1s =
        TypeReference.findOrCreate(ClassLoaderReference.Application, "Lnestmates/Outer$Inner");
    MethodReference t1m = MethodReference.findOrCreate(t1s, "triple", "()I");
    Assert.assertTrue("expect Outer.Inner.triple node", cg.getNodes(t1m).iterator().hasNext());
    CGNode t1node = cg.getNodes(t1m).iterator().next();

    // Check call from main to Triple()
    Assert.assertTrue(
        "should have call site from main to TestNestmates.triple()",
        cg.getPossibleSites(mnode, t1node).hasNext());

    // check that triple() does not call an accessor method
    Assert.assertFalse(
        "there should not be a call from triple() to an accessor method",
        cg.getSuccNodes(t1node).hasNext());
  }
}
