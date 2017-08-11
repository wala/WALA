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
package com.ibm.wala.core.tests.cfg.exc.inter;


import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.cfg.exc.ExceptionPruningAnalysis;
import com.ibm.wala.cfg.exc.InterprocAnalysisResult;
import com.ibm.wala.cfg.exc.NullPointerAnalysis;
import com.ibm.wala.cfg.exc.intra.IntraprocNullPointerAnalysis;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.util.warnings.Warnings;

/**
 * Test validity and precision of inter-procedural NullpointerException-Analysis {@link IntraprocNullPointerAnalysis}
 * 
 */
public class NullPointerExceptionInterTest extends WalaTestCase {

  private static AnalysisScope scope;

  private static ClassHierarchy cha;
  
  private static CallGraph cg;
  
  private static IAnalysisCacheView cache;

  @BeforeClass
  public static void beforeClass() throws Exception {
    cache = new AnalysisCacheImpl();
    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA,
        (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS), NullPointerExceptionInterTest.class.getClassLoader());
    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions());
    try {
      cha = ClassHierarchyFactory.make(scope, factory);
      Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, "Lcfg/exc/inter/CallFieldAccess");
      AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
      
      CallGraphBuilder<InstanceKey> builder = Util.makeNCFABuilder(1, options, cache, cha, scope);
      cg = builder.makeCallGraph(options, null);
    } catch (ClassHierarchyException e) {
      throw new Exception(e);
    }
  }

  @AfterClass
  public static void afterClass() throws Exception {
    Warnings.clear();
    scope = null;
    cha = null;
    cg = null;
    cache = null;
  }

  public static void main(String[] args) {
    justThisTest(NullPointerExceptionInterTest.class);
  }

  @Test
  public void testIfException() throws UnsoundGraphException, CancelException, WalaException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.inter.CallFieldAccess.callIfException()Lcfg/exc/intra/B");

    InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interExplodedCFG = 
        NullPointerAnalysis.computeInterprocAnalysis(cg, new NullProgressMonitor());

    Assert.assertEquals(1, cg.getNodes(mr).size());
    final CGNode callNode = cg.getNodes(mr).iterator().next();

    ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG = interExplodedCFG.getResult(callNode);

    Assert.assertTrue(intraExplodedCFG.hasExceptions());
  }
  
  @Test
  public void testDynamicIfException() throws UnsoundGraphException, CancelException, WalaException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.inter.CallFieldAccess.callDynamicIfException()Lcfg/exc/intra/B");

    Assert.assertEquals(1, cg.getNodes(mr).size());
    final CGNode callNode = cg.getNodes(mr).iterator().next();

    InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interExplodedCFG = 
        NullPointerAnalysis.computeInterprocAnalysis(cg, new NullProgressMonitor());


    ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG = interExplodedCFG.getResult(callNode);

    Assert.assertTrue(intraExplodedCFG.hasExceptions());
  }

  
  @Test
  public void testIfNoException() throws UnsoundGraphException, CancelException, WalaException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.inter.CallFieldAccess.callIfNoException()Lcfg/exc/intra/B");

    InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interExplodedCFG = 
        NullPointerAnalysis.computeInterprocAnalysis(cg, new NullProgressMonitor());

    Assert.assertEquals(1, cg.getNodes(mr).size());
    final CGNode callNode = cg.getNodes(mr).iterator().next();

    ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG = interExplodedCFG.getResult(callNode);
    Assert.assertFalse(intraExplodedCFG.hasExceptions());
  }
  
  @Test
  public void testDynamicIfNoException() throws UnsoundGraphException, CancelException, WalaException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.inter.CallFieldAccess.callDynamicIfNoException()Lcfg/exc/intra/B");

    InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interExplodedCFG = 
        NullPointerAnalysis.computeInterprocAnalysis(cg, new NullProgressMonitor());

    Assert.assertEquals(1, cg.getNodes(mr).size());
    final CGNode callNode = cg.getNodes(mr).iterator().next();

    ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG = interExplodedCFG.getResult(callNode);
    Assert.assertFalse(intraExplodedCFG.hasExceptions());
  }
  
  @Test
  public void testIf2Exception() throws UnsoundGraphException, CancelException, WalaException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.inter.CallFieldAccess.callIf2Exception()Lcfg/exc/intra/B");

    InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interExplodedCFG = 
        NullPointerAnalysis.computeInterprocAnalysis(cg, new NullProgressMonitor());

    Assert.assertEquals(1, cg.getNodes(mr).size());
    final CGNode callNode = cg.getNodes(mr).iterator().next();

    ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG = interExplodedCFG.getResult(callNode);

    Assert.assertTrue(intraExplodedCFG.hasExceptions());
  }
  
  @Test
  public void testDynamicIf2Exception() throws UnsoundGraphException, CancelException, WalaException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.inter.CallFieldAccess.callDynamicIf2Exception()Lcfg/exc/intra/B");

    Assert.assertEquals(1, cg.getNodes(mr).size());
    final CGNode callNode = cg.getNodes(mr).iterator().next();

    InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interExplodedCFG = 
        NullPointerAnalysis.computeInterprocAnalysis(cg, new NullProgressMonitor());


    ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG = interExplodedCFG.getResult(callNode);

    Assert.assertTrue(intraExplodedCFG.hasExceptions());
  }

  
  @Test
  public void testIf2NoException() throws UnsoundGraphException, CancelException, WalaException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.inter.CallFieldAccess.callIf2NoException()Lcfg/exc/intra/B");

    InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interExplodedCFG = 
        NullPointerAnalysis.computeInterprocAnalysis(cg, new NullProgressMonitor());

    Assert.assertEquals(1, cg.getNodes(mr).size());
    final CGNode callNode = cg.getNodes(mr).iterator().next();

    ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG = interExplodedCFG.getResult(callNode);
    Assert.assertFalse(intraExplodedCFG.hasExceptions());
  }
  
  @Test
  public void testDynamicIf2NoException() throws UnsoundGraphException, CancelException, WalaException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.inter.CallFieldAccess.callDynamicIf2NoException()Lcfg/exc/intra/B");

    InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interExplodedCFG = 
        NullPointerAnalysis.computeInterprocAnalysis(cg, new NullProgressMonitor());

    Assert.assertEquals(1, cg.getNodes(mr).size());
    final CGNode callNode = cg.getNodes(mr).iterator().next();

    ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG = interExplodedCFG.getResult(callNode);
    Assert.assertFalse(intraExplodedCFG.hasExceptions());
  }
  

  @Test
  public void testGetException() throws UnsoundGraphException, CancelException, WalaException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.inter.CallFieldAccess.callGetException()Lcfg/exc/intra/B");

    InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interExplodedCFG = 
        NullPointerAnalysis.computeInterprocAnalysis(cg, new NullProgressMonitor());

    Assert.assertEquals(1, cg.getNodes(mr).size());
    final CGNode callNode = cg.getNodes(mr).iterator().next();

    ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG = interExplodedCFG.getResult(callNode);

    Assert.assertTrue(intraExplodedCFG.hasExceptions());
  }
  
  @Test
  public void testDynamicGetException() throws UnsoundGraphException, CancelException, WalaException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.inter.CallFieldAccess.callDynamicGetException()Lcfg/exc/intra/B");

    Assert.assertEquals(1, cg.getNodes(mr).size());
    final CGNode callNode = cg.getNodes(mr).iterator().next();

    InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interExplodedCFG = 
        NullPointerAnalysis.computeInterprocAnalysis(cg, new NullProgressMonitor());


    ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG = interExplodedCFG.getResult(callNode);

    Assert.assertTrue(intraExplodedCFG.hasExceptions());
  }
}
