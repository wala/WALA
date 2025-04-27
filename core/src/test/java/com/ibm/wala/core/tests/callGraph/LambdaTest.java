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

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.strings.Atom;
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
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Check properties of a call to clone() in RTA */
public class LambdaTest extends WalaTestCase {

  @Test
  public void testBug144()
      throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Lbug144/A");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    @SuppressWarnings("unused")
    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);
  }

  @Test
  public void testBug137()
      throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Lspecial/A");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);

    TypeReference A = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lspecial/A");

    MethodReference ct =
        MethodReference.findOrCreate(
            A, Atom.findOrCreateUnicodeAtom("<init>"), Descriptor.findOrCreateUTF8("()V"));
    Set<CGNode> ctnodes = cg.getNodes(ct);
    assertThat(ctnodes).hasSize(1);

    MethodReference ts =
        MethodReference.findOrCreate(
            A,
            Atom.findOrCreateUnicodeAtom("toString"),
            Descriptor.findOrCreateUTF8("()Ljava/lang/String;"));
    Set<CGNode> tsnodes = cg.getNodes(ts);
    assertThat(tsnodes).hasSize(1);
  }

  @Test
  public void testSortingExample()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Llambda/SortingExample");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);

    // Find compareTo
    TypeReference str =
        TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/String");
    MethodReference ct =
        MethodReference.findOrCreate(
            str,
            Atom.findOrCreateUnicodeAtom("compareTo"),
            Descriptor.findOrCreateUTF8("(Ljava/lang/String;)I"));
    Set<CGNode> ctnodes = cg.getNodes(ct);

    checkCompareToCalls(cg, ctnodes, "id1", 1);
    checkCompareToCalls(cg, ctnodes, "id2", 1);
    checkCompareToCalls(cg, ctnodes, "id3", 2);
    checkCompareToCalls(cg, ctnodes, "id4", 1);
  }

  protected void checkCompareToCalls(CallGraph cg, Set<CGNode> ctnodes, String x, int expected) {
    // Find node corresponding to id1
    TypeReference tid1 =
        TypeReference.findOrCreate(ClassLoaderReference.Application, "Llambda/SortingExample");
    MethodReference mid1 = MethodReference.findOrCreate(tid1, x, "(I)I");
    assertThat(cg.getNodes(mid1).iterator()).withFailMessage("expect %s node", x).hasNext();
    CGNode id1node = cg.getNodes(mid1).iterator().next();

    // caller of id1 is dynamic from sortForward, and has 1 compareTo
    CGNode sfnode = cg.getPredNodes(id1node).next();
    int count = 0;
    for (CallSiteReference site : Iterator2Iterable.make(sfnode.iterateCallSites())) {
      if (ctnodes.containsAll(cg.getPossibleTargets(sfnode, site))) {
        count++;
      }
    }
    assertThat(count).withFailMessage("expected one call to compareTo").isEqualTo(expected);
    System.err.println("found " + count + " compareTo calls in " + sfnode);
  }

  @Test
  public void testMethodRefs()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Llambda/MethodRefs");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);

    Function<String, MethodReference> getTargetRef =
        (klass) ->
            MethodReference.findOrCreate(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Llambda/MethodRefs$" + klass),
                Atom.findOrCreateUnicodeAtom("target"),
                Descriptor.findOrCreateUTF8("()V"));

    assertThat(cg.getNodes(getTargetRef.apply("C1")))
        .withFailMessage("expected C1.target() to be reachable")
        .hasSize(1);
    assertThat(cg.getNodes(getTargetRef.apply("C2")))
        .withFailMessage("expected C2.target() to be reachable")
        .hasSize(1);
    assertThat(cg.getNodes(getTargetRef.apply("C3")))
        .withFailMessage("expected C3.target() to be reachable")
        .hasSize(1);
    assertThat(cg.getNodes(getTargetRef.apply("C4")))
        .withFailMessage("expected C4.target() to be reachable")
        .hasSize(1);
    assertThat(cg.getNodes(getTargetRef.apply("C5")))
        .withFailMessage("expected C5.target() to *not* be reachable")
        .isEmpty();
  }

  @Test
  public void testParamsAndCapture()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Llambda/ParamsAndCapture");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);

    Function<String, MethodReference> getTargetRef =
        (klass) ->
            MethodReference.findOrCreate(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Llambda/ParamsAndCapture$" + klass),
                Atom.findOrCreateUnicodeAtom("target"),
                Descriptor.findOrCreateUTF8("()V"));

    Consumer<String> checkCalledFromOneSite =
        (klassName) -> {
          Set<CGNode> nodes = cg.getNodes(getTargetRef.apply(klassName));
          assertThat(nodes)
              .withFailMessage("expected %s.target() to be reachable", klassName)
              .hasSize(1);
          CGNode node = nodes.iterator().next();
          List<CGNode> predNodes = Iterator2Collection.toList(cg.getPredNodes(node));
          assertThat(predNodes)
              .withFailMessage(
                  () -> "expected " + klassName + ".target() to be invoked from one calling method")
              .hasSize(1);
          CGNode pred = predNodes.get(0);
          List<CallSiteReference> sites =
              Iterator2Collection.toList(cg.getPossibleSites(pred, node));
          assertThat(sites)
              .withFailMessage(
                  () -> "expected " + klassName + ".target() to be invoked from one call site")
              .hasSize(1);
        };

    checkCalledFromOneSite.accept("C1");
    checkCalledFromOneSite.accept("C2");
    checkCalledFromOneSite.accept("C3");
    checkCalledFromOneSite.accept("C4");
    checkCalledFromOneSite.accept("C5");
  }

  @Test
  public void testLambaMetafactoryCall()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Llambda/CallMetaFactory");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    // shouldn't crash
    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);
    // exceedingly unlikely to fail, but ensures that optimizer won't remove buildZeroCFA call
    assertThat(cg.getNumberOfNodes()).isPositive();
  }
}
