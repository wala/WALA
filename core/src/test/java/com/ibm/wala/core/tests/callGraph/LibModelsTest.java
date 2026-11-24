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
package com.ibm.wala.core.tests.callGraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

/** Check properties of a call to clone() in RTA */
public class LibModelsTest extends WalaTestCase {

  @Test
  public void testLibModels()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    String libModelsTestClass = "Llibmodels/LibModels";
    Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(cha, libModelsTestClass);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);

    // System.err.println(cg);

    // Find node corresponding to finalize
    TypeReference t =
        TypeReference.findOrCreate(ClassLoaderReference.Application, libModelsTestClass);
    MethodReference m = MethodReference.findOrCreate(t, "reachable1", "()V");
    assertThat(cg.getNodes(m).iterator()).hasNext();
    MethodReference m2 = MethodReference.findOrCreate(t, "reachable2", "()V");
    assertThat(cg.getNodes(m2).iterator()).hasNext();
  }

  @Test
  void standardStreamSettersUpdateTheCorrespondingFields()
      throws ClassHierarchyException, CancelException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    TypeReference subject =
        TypeReference.findOrCreate(ClassLoaderReference.Application, "Llibmodels/LibModels");
    MethodReference method = MethodReference.findOrCreate(subject, "setStandardStreams", "()V");
    AnalysisOptions options =
        CallGraphTestUtil.makeAnalysisOptions(scope, List.of(new DefaultEntrypoint(method, cha)));
    SSAPropagationCallGraphBuilder builder =
        Util.makeZeroCFABuilder(JavaLanguage.get(), options, new AnalysisCacheImpl(), cha);

    builder.makeCallGraph(options);

    PointerAnalysis<InstanceKey> pointers = builder.getPointerAnalysis();
    IClass system = cha.lookupClass(TypeReference.JavaLangSystem);
    assertSoftly(softly -> {
      softly
          .assertThat(pointedToTypes(pointers, system, "in"))
          .as("System.in")
          .contains("Ljava/io/ByteArrayInputStream");
      softly
          .assertThat(pointedToTypes(pointers, system, "out"))
          .as("System.out")
          .contains("Llibmodels/LibModels$Stdout");
      softly
          .assertThat(pointedToTypes(pointers, system, "err"))
          .as("System.err")
          .contains("Llibmodels/LibModels$Stderr");
    });
  }

  private static List<String> pointedToTypes(
      PointerAnalysis<InstanceKey> pointers, IClass declaringClass, String fieldName) {
    PointerKey field =
        pointers
            .getHeapModel()
            .getPointerKeyForStaticField(
                declaringClass.getField(Atom.findOrCreateAsciiAtom(fieldName)));
    return StreamSupport.stream(pointers.getPointsToSet(field).spliterator(), false)
        .map(instance -> instance.concreteType().getName().toString())
        .toList();
  }
}
