/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.callGraph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Check handling of class constants (test for part of 1.5 support) */
public class ClassConstantTest extends WalaTestCase {

  @Test
  public void testClassConstants()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);

    // make sure we have the test class
    TypeReference mainClassRef =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application, TestConstants.CLASSCONSTANT_MAIN);
    assertNotNull(cha.lookupClass(mainClassRef));

    // make call graph
    Iterable<Entrypoint> entrypoints =
        Util.makeMainEntrypoints(cha, TestConstants.CLASSCONSTANT_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);
    // System.out.println("\nCall graph:");
    // Trace.println(cg);

    // make sure the main method is reached
    MethodReference mainMethodRef =
        MethodReference.findOrCreate(mainClassRef, "main", "([Ljava/lang/String;)V");
    Set<CGNode> mainMethodNodes = cg.getNodes(mainMethodRef);
    assertFalse(mainMethodNodes.isEmpty());
    CGNode mainMethodNode = mainMethodNodes.iterator().next();
    // Trace.println("main IR:");
    // Trace.println(mainMethodNode.getIR());

    // Make sure call to hashCode is there (it uses the class constant)
    TypeReference classRef =
        TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Class");
    MethodReference hashCodeRef = MethodReference.findOrCreate(classRef, "hashCode", "()I");
    Set<CGNode> hashCodeNodes = cg.getNodes(hashCodeRef);
    assertFalse(hashCodeNodes.isEmpty());

    // make sure call to hashCode from main
    assertTrue(cg.hasEdge(mainMethodNode, hashCodeNodes.iterator().next()));
  }

  @Test
  public void classHierarchyToJson() throws ClassHierarchyException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Gson gson = new Gson();
    Type type = new TypeToken<HashMap<String, Set<String>>>() {}.getType();
    String json = cha.toJson();
    System.err.println(json);
    HashMap<String, Set<String>> list = gson.fromJson(json, type);
    assertTrue(list.containsKey(nodeToString(cha.getRootClass())));

    Set<String> subclassNames = new HashSet<>();
    Iterator<IClass> children = cha.computeSubClasses(cha.getRootClass().getReference()).iterator();
    while (children.hasNext()) {
      String temp = nodeToString(children.next());
      subclassNames.add(temp);
    }
    assertTrue(subclassNames.containsAll(list.get(nodeToString(cha.getRootClass()))));
  }

  private String nodeToString(IClass klass) {
    return StringStuff.jvmToBinaryName(klass.getName().toString());
  }
}
