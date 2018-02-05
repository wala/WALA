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
package com.ibm.wala.core.tests.ptrs;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.OrdinalSet;

public class ZeroLengthArrayTest {

  @Test
  public void testZeroLengthArray() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA,
        CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        TestConstants.ZERO_LENGTH_ARRAY_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);
    PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();
//    System.err.println(pa);

    HeapModel heapModel = pa.getHeapModel();
    CGNode mainNode = cg.getNode(
        cha.resolveMethod(MethodReference.findOrCreate(
            TypeReference.findOrCreate(ClassLoaderReference.Application, TestConstants.ZERO_LENGTH_ARRAY_MAIN),
            Selector.make("main([Ljava/lang/String;)V"))), Everywhere.EVERYWHERE);
    OrdinalSet<InstanceKey> pointsToSet = pa.getPointsToSet(heapModel.getPointerKeyForLocal(mainNode, 4));
    Assert.assertEquals(1, pointsToSet.size());
    InstanceKey arrayKey = pointsToSet.iterator().next();
    OrdinalSet<InstanceKey> arrayContents = pa.getPointsToSet(heapModel.getPointerKeyForArrayContents(arrayKey));
    System.err.println(arrayContents);
    Assert.assertEquals(0, arrayContents.size());
    
  }
}
