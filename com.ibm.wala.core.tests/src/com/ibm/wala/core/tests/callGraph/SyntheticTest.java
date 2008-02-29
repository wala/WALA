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
import java.util.Collections;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.SubtypesEntrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

/**
 * Tests for synthetic methods
 * 
 * @author sfink
 */

public class SyntheticTest extends WalaTestCase {

  public void testMultiSubtypes() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA,
        CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchy.make(scope);
    TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Application, "LmultiTypes/Foo");
    MethodReference mref = MethodReference.findOrCreate(t, "foo", "(LmultiTypes/Foo$A;)V");
    IMethod m = cha.resolveMethod(mref);
    assert m != null;
    SubtypesEntrypoint e = new SubtypesEntrypoint(m, cha);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, Collections.<Entrypoint>singleton(e));

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCache(), cha, scope, false);
    
    TypeReference tA = TypeReference.findOrCreate(ClassLoaderReference.Application, "LmultiTypes/Foo$A");
    MethodReference barA = MethodReference.findOrCreate(tA, "bar", "()V");
    TypeReference tB = TypeReference.findOrCreate(ClassLoaderReference.Application, "LmultiTypes/Foo$B");
    MethodReference barB = MethodReference.findOrCreate(tB, "bar", "()V");
    assertTrue(cg.getNodes(barA).size() == 1);
    assertTrue(cg.getNodes(barB).size() == 1);
    
    CGNode root = cg.getFakeRootNode();
    IR ir = root.getIR();
    assertTrue(ir.iteratePhis().hasNext());
  }

}
