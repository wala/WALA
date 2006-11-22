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
package com.ibm.wala.core.tests.callGraph;

import java.util.Set;

import junit.framework.Assert;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * Check handling of class constants (test for part of 1.5 support)
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class ClassConstantTest extends WalaTestCase {

  public void testClassConstants() throws ClassHierarchyException {

    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Trace.println("setup warnings:");
    Trace.println(warnings);

    // make sure we have the test class
    TypeReference mainClassRef = TypeReference.findOrCreate(ClassLoaderReference.Application, TestConstants.CLASSCONSTANT_MAIN);
    Assert.assertTrue(cha.lookupClass(mainClassRef) != null);

    // make call graph
    Entrypoints entrypoints = Util.makeMainEntrypoints(scope, cha, TestConstants.CLASSCONSTANT_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, cha, scope, warnings);
    Trace.println("\nCall graph:");
    Trace.println(cg);

    // make sure the main method is reached
    MethodReference mainMethodRef = MethodReference.findOrCreate(mainClassRef, "main", "([Ljava/lang/String;)V");
    Set<CGNode> mainMethodNodes = cg.getNodes(mainMethodRef);
    Assert.assertFalse(mainMethodNodes.isEmpty());
    CGNode mainMethodNode = (CGNode) mainMethodNodes.iterator().next();
    Trace.println("main IR:");
    Trace.println(cg.getInterpreter(mainMethodNode).getIR(mainMethodNode, warnings));

    // Make sure call to hashCode is there (it uses the class constant)
    TypeReference classRef = TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Class");
    MethodReference hashCodeRef = MethodReference.findOrCreate(classRef, "hashCode", "()I");
    Set<CGNode> hashCodeNodes = cg.getNodes(hashCodeRef);
    Assert.assertFalse(hashCodeNodes.isEmpty());

    // make sure call to hashCode from main
    Assert.assertTrue(cg.hasEdge(mainMethodNode, hashCodeNodes.iterator().next()));
  }

}
