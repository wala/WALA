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
package com.ibm.wala.examples.analysis.dataflow;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
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
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 * Tests of various flow analysis engines.
 */
public class DataflowTest extends WalaTestCase {

  private static AnalysisScope scope;

  private static IClassHierarchy cha;

  // more aggressive exclusions to avoid library blowup
  // in interprocedural tests
  private static final String EXCLUSIONS = "java\\/awt\\/.*\n" + 
  		"javax\\/swing\\/.*\n" + 
  		"sun\\/awt\\/.*\n" + 
  		"sun\\/swing\\/.*\n" + 
  		"com\\/sun\\/.*\n" + 
  		"sun\\/.*\n" + 
  		"org\\/netbeans\\/.*\n" + 
  		"org\\/openide\\/.*\n" + 
  		"com\\/ibm\\/crypto\\/.*\n" + 
  		"com\\/ibm\\/security\\/.*\n" + 
  		"org\\/apache\\/xerces\\/.*\n" + 
  		"java\\/security\\/.*\n" + 
  		"";
  
  
  @BeforeClass
  public static void beforeClass() throws Exception {

    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, null, DataflowTest.class.getClassLoader());

    scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes("UTF-8"))));
    try {
      cha = ClassHierarchyFactory.make(scope);
    } catch (ClassHierarchyException e) {
      throw new Exception();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#tearDown()
   */
  @AfterClass
  public static void afterClass() throws Exception {
    scope = null;
    cha = null;
  }

  @Test
  public void testIntraproc1() {
    AnalysisCache cache = new AnalysisCache();
    final MethodReference ref = MethodReference.findOrCreate(ClassLoaderReference.Application, "Ldataflow/StaticDataflow", "test1",
        "()V");
    IMethod method = cha.resolveMethod(ref);
    IR ir = cache.getIRFactory().makeIR(method, Everywhere.EVERYWHERE, SSAOptions.defaultOptions());
    ExplodedControlFlowGraph ecfg = ExplodedControlFlowGraph.make(ir);
    IntraprocReachingDefs reachingDefs = new IntraprocReachingDefs(ecfg, cha);
    BitVectorSolver<IExplodedBasicBlock> solver = reachingDefs.analyze();
    for (IExplodedBasicBlock ebb : ecfg) {
      if (ebb.getNumber() == 4) {
        IntSet out = solver.getOut(ebb).getValue();
        Assert.assertEquals(1, out.size());
        Assert.assertTrue(out.contains(1));
      }
    }
  }

  @Test
  public void testIntraproc2() {
    AnalysisCache cache = new AnalysisCache();
    final MethodReference ref = MethodReference.findOrCreate(ClassLoaderReference.Application, "Ldataflow/StaticDataflow", "test2",
        "()V");
    IMethod method = cha.resolveMethod(ref);
    IR ir = cache.getIRFactory().makeIR(method, Everywhere.EVERYWHERE, SSAOptions.defaultOptions());
    ExplodedControlFlowGraph ecfg = ExplodedControlFlowGraph.make(ir);
    IntraprocReachingDefs reachingDefs = new IntraprocReachingDefs(ecfg, cha);
    BitVectorSolver<IExplodedBasicBlock> solver = reachingDefs.analyze();
    for (IExplodedBasicBlock ebb : ecfg) {
      if (ebb.getNumber() == 10) {
        IntSet out = solver.getOut(ebb).getValue();
        Assert.assertEquals(2, out.size());
        Assert.assertTrue(out.contains(0));
        Assert.assertTrue(out.contains(2));
      }
    }
  }

  @Test
  public void testContextInsensitive() throws IllegalArgumentException, CallGraphBuilderCancelException {
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "Ldataflow/StaticDataflow");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);
    ExplodedInterproceduralCFG icfg = ExplodedInterproceduralCFG.make(cg);
    ContextInsensitiveReachingDefs reachingDefs = new ContextInsensitiveReachingDefs(icfg, cha);
    BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>> solver = reachingDefs.analyze();
    for (BasicBlockInContext<IExplodedBasicBlock> bb : icfg) {
      if (bb.getNode().toString().contains("testInterproc")) {
        IExplodedBasicBlock delegate = bb.getDelegate();
        if (delegate.getNumber() == 4) {
          IntSet solution = solver.getOut(bb).getValue();
          IntIterator intIterator = solution.intIterator();
          List<Pair<CGNode, Integer>> applicationDefs = new ArrayList<Pair<CGNode,Integer>>();
          while (intIterator.hasNext()) {
            int next = intIterator.next();
            final Pair<CGNode, Integer> def = reachingDefs.getNodeAndInstrForNumber(next);
            if (def.fst.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
              System.out.println(def);
              applicationDefs.add(def);
            }
          }
          Assert.assertEquals(2, applicationDefs.size());
        }
      }
    }
  }

  @Test
  public void testContextSensitive() throws IllegalArgumentException, CancelException {
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "Ldataflow/StaticDataflow");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);
    AnalysisCache cache = new AnalysisCache();
    ContextSensitiveReachingDefs reachingDefs = new ContextSensitiveReachingDefs(cg, cache);
    TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> result = reachingDefs.analyze();
    ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph = reachingDefs.getSupergraph();
    for (BasicBlockInContext<IExplodedBasicBlock> bb : supergraph) {
      if (bb.getNode().toString().contains("testInterproc")) {
        IExplodedBasicBlock delegate = bb.getDelegate();
        if (delegate.getNumber() == 4) {
          IntSet solution = result.getResult(bb);
          IntIterator intIterator = solution.intIterator();
          List<Pair<CGNode, Integer>> applicationDefs = new ArrayList<Pair<CGNode,Integer>>();
          while (intIterator.hasNext()) {
            int next = intIterator.next();
            final Pair<CGNode, Integer> def = reachingDefs.getDomain().getMappedObject(next);
            if (def.fst.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
              System.out.println(def);
              applicationDefs.add(def);
            }
          }
          Assert.assertEquals(1, applicationDefs.size());
        }
      }
    }
  }
}
