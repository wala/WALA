/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.examples.analysis.dataflow;

import static com.ibm.wala.util.intset.IntSetAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.ExplodedInterproceduralCFG;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.PatternsFilter;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests of various flow analysis engines. */
public class DataflowTest extends WalaTestCase {

  private static AnalysisScope scope;

  private static IClassHierarchy cha;

  // more aggressive exclusions to avoid library blowup
  // in interprocedural tests
  private static final String[] EXCLUSIONS = {
    "com/ibm/crypto",
    "com/ibm/security",
    "com/sun",
    "java/awt",
    "java/security",
    "javax/swing",
    "org/apache/xerces",
    "org/netbeans",
    "org/openide",
    "sun",
    "sun/awt",
    "sun/swing",
  };

  @BeforeAll
  public static void beforeClass() throws Exception {

    scope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA, null, DataflowTest.class.getClassLoader());

    // Each element of `EXCLUSIONS` is the qualified name of a package, but with `/` instead of `.`
    // as the delimiter between names.  Here we turn each of those into a regular expression with
    // a trailing `/.*` to match anything in or under the named package.
    scope.setExclusions(
        new PatternsFilter(
            Arrays.stream(EXCLUSIONS).map(exclusion -> Pattern.quote(exclusion) + "/.*")));
    try {
      cha = ClassHierarchyFactory.make(scope);
    } catch (ClassHierarchyException e) {
      throw new Exception(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see junit.framework.TestCase#tearDown()
   */
  @AfterAll
  public static void afterClass() throws Exception {
    scope = null;
    cha = null;
  }

  @Test
  public void testIntraproc1() {
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    final MethodReference ref =
        MethodReference.findOrCreate(
            ClassLoaderReference.Application, "Ldataflow/StaticDataflow", "test1", "()V");
    IMethod method = cha.resolveMethod(ref);
    IR ir = cache.getIRFactory().makeIR(method, Everywhere.EVERYWHERE, SSAOptions.defaultOptions());
    ExplodedControlFlowGraph ecfg = ExplodedControlFlowGraph.make(ir);
    IntraprocReachingDefs reachingDefs = new IntraprocReachingDefs(ecfg, cha);
    BitVectorSolver<IExplodedBasicBlock> solver = reachingDefs.analyze();
    for (IExplodedBasicBlock ebb : ecfg) {
      if (ebb.getNumber() == 4) {
        IntSet out = solver.getOut(ebb).getValue();
        assertThat(out).toCollection().containsExactly(1);
      }
    }
  }

  @Test
  public void testIntraproc2() {
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    final MethodReference ref =
        MethodReference.findOrCreate(
            ClassLoaderReference.Application, "Ldataflow/StaticDataflow", "test2", "()V");
    IMethod method = cha.resolveMethod(ref);
    IR ir = cache.getIRFactory().makeIR(method, Everywhere.EVERYWHERE, SSAOptions.defaultOptions());
    ExplodedControlFlowGraph ecfg = ExplodedControlFlowGraph.make(ir);
    IntraprocReachingDefs reachingDefs = new IntraprocReachingDefs(ecfg, cha);
    BitVectorSolver<IExplodedBasicBlock> solver = reachingDefs.analyze();
    for (IExplodedBasicBlock ebb : ecfg) {
      if (ebb.getNumber() == 10) {
        IntSet out = solver.getOut(ebb).getValue();
        assertThat(out).toCollection().containsExactly(0, 2);
      }
    }
  }

  @Test
  public void testContextInsensitive()
      throws IllegalArgumentException, CallGraphBuilderCancelException {
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Ldataflow/StaticDataflow");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);
    ExplodedInterproceduralCFG icfg = ExplodedInterproceduralCFG.make(cg);
    ContextInsensitiveReachingDefs reachingDefs = new ContextInsensitiveReachingDefs(icfg, cha);
    BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>> solver = reachingDefs.analyze();
    assertThat(icfg)
        .filteredOn(
            bb ->
                bb.getNode().toString().contains("testInterproc")
                    && bb.getDelegate().getNumber() == 4)
        .singleElement()
        .satisfies(
            bb -> {
              IntSet solution = solver.getOut(bb).getValue();
              IntIterator intIterator = solution.intIterator();
              List<Pair<CGNode, Integer>> applicationDefs = new ArrayList<>();
              while (intIterator.hasNext()) {
                int next = intIterator.next();
                final Pair<CGNode, Integer> def = reachingDefs.getNodeAndInstrForNumber(next);
                if (def.fst
                    .getMethod()
                    .getDeclaringClass()
                    .getClassLoader()
                    .getReference()
                    .equals(ClassLoaderReference.Application)) {
                  System.out.println(def);
                  applicationDefs.add(def);
                }
              }
              assertThat(applicationDefs).hasSize(2);
            });
  }

  @Test
  public void testContextSensitive() throws IllegalArgumentException, CancelException {
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Ldataflow/StaticDataflow");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);
    ContextSensitiveReachingDefs reachingDefs = new ContextSensitiveReachingDefs(cg);
    TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>>
        result = reachingDefs.analyze();
    ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph =
        reachingDefs.getSupergraph();
    assertThat(supergraph)
        .filteredOn(
            bb ->
                bb.getNode().toString().contains("testInterproc")
                    && bb.getDelegate().getNumber() == 4)
        .singleElement()
        .satisfies(
            bb -> {
              IntSet solution = result.getResult(bb);
              IntIterator intIterator = solution.intIterator();
              List<Pair<CGNode, Integer>> applicationDefs = new ArrayList<>();
              while (intIterator.hasNext()) {
                int next = intIterator.next();
                final Pair<CGNode, Integer> def = reachingDefs.getDomain().getMappedObject(next);
                if (def.fst
                    .getMethod()
                    .getDeclaringClass()
                    .getClassLoader()
                    .getReference()
                    .equals(ClassLoaderReference.Application)) {
                  System.out.println(def);
                  applicationDefs.add(def);
                }
              }
              assertThat(applicationDefs).hasSize(1);
            });
  }
}
