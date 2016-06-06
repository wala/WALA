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
package com.ibm.wala.core.tests.cfg.exc.intra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.exc.ExceptionPruningAnalysis;
import com.ibm.wala.cfg.exc.NullPointerAnalysis;
import com.ibm.wala.cfg.exc.intra.IntraprocNullPointerAnalysis;
import com.ibm.wala.cfg.exc.intra.NullPointerState;
import com.ibm.wala.cfg.exc.intra.NullPointerState.State;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.util.warnings.Warnings;

/**
 * Test validity and precision of intra-procedurel NullpointerException-Analysis {@link IntraprocNullPointerAnalysis}
 * 
 */
public class NullPointerExceptionIntraTest extends WalaTestCase {

  private static AnalysisScope scope;

  private static ClassHierarchy cha;

  @BeforeClass
  public static void beforeClass() throws Exception {

    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA,
        (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"), NullPointerExceptionIntraTest.class.getClassLoader());
    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions());

    try {
      cha = ClassHierarchy.make(scope, factory);
    } catch (ClassHierarchyException e) {
      throw new Exception();
    }
  }

  @AfterClass
  public static void afterClass() throws Exception {
    Warnings.clear();
    scope = null;
    cha = null;
  }

  public static void main(String[] args) {
    justThisTest(NullPointerExceptionIntraTest.class);
  }

  @Test
  public void testIf() throws UnsoundGraphException, CancelException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.intra.FieldAccess.testIf(ZLcfg/exc/intra/B;Lcfg/exc/intra/B;)Lcfg/exc/intra/B");

    IMethod m = cha.resolveMethod(mr);
    AnalysisCache cache = new AnalysisCache();
    IR ir = cache.getIR(m);
    final ISSABasicBlock returnNode = returnNode(ir.getControlFlowGraph());
    final int returnVal = returnVal(returnNode);

    {
      ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG =
          NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis(ir);
      intraExplodedCFG.compute(new NullProgressMonitor());
        
      final IExplodedBasicBlock returnNodeExploded = returnNodeExploded(returnNode, intraExplodedCFG.getCFG());
      final NullPointerState returnState = intraExplodedCFG.getState(returnNodeExploded);

      Assert.assertEquals(State.NOT_NULL, returnState.getState(returnVal));
    }
    {
      ExceptionPruningAnalysis<SSAInstruction, ISSABasicBlock> intraSSACFG =
          NullPointerAnalysis.createIntraproceduralSSACFGAnalyis(ir);
      intraSSACFG.compute(new NullProgressMonitor());
      
      Assert.assertEquals(ir.getControlFlowGraph().exit(), intraSSACFG.getCFG().exit());
      Assert.assertEquals(returnNode,           returnNode(intraSSACFG.getCFG()));
        
      final NullPointerState returnState = intraSSACFG.getState(returnNode);

      Assert.assertEquals(State.NOT_NULL, returnState.getState(returnVal));
    }    
  }
  
  @Test
  public void testIf2() throws UnsoundGraphException, CancelException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.intra.FieldAccess.testIf2(ZLcfg/exc/intra/B;Lcfg/exc/intra/B;)Lcfg/exc/intra/B");

    IMethod m = cha.resolveMethod(mr);
    AnalysisCache cache = new AnalysisCache();
    IR ir = cache.getIR(m);
    final ISSABasicBlock returnNode = returnNode(ir.getControlFlowGraph());
    final int returnVal = returnVal(returnNode);

    {
      ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG =
          NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis(ir);
      intraExplodedCFG.compute(new NullProgressMonitor());
        
      final IExplodedBasicBlock returnNodeExploded = returnNodeExploded(returnNode, intraExplodedCFG.getCFG());
      final NullPointerState returnState = intraExplodedCFG.getState(returnNodeExploded);

      Assert.assertNotEquals(State.NOT_NULL, returnState.getState(returnVal));
      Assert.assertNotEquals(State.NULL, returnState.getState(returnVal));
    }
    {
      ExceptionPruningAnalysis<SSAInstruction, ISSABasicBlock> intraSSACFG =
          NullPointerAnalysis.createIntraproceduralSSACFGAnalyis(ir);
      intraSSACFG.compute(new NullProgressMonitor());
      
      Assert.assertEquals(ir.getControlFlowGraph().exit(), intraSSACFG.getCFG().exit());
      Assert.assertEquals(returnNode,           returnNode(intraSSACFG.getCFG()));
        
      final NullPointerState returnState = intraSSACFG.getState(returnNode);

      Assert.assertNotEquals(State.NOT_NULL, returnState.getState(returnVal));
      Assert.assertNotEquals(State.NULL, returnState.getState(returnVal));
    }    
  }

  @Test
  public void testIf3() throws UnsoundGraphException, CancelException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.intra.FieldAccess.testIf3(ZLcfg/exc/intra/B;)Lcfg/exc/intra/B");

    IMethod m = cha.resolveMethod(mr);
    AnalysisCache cache = new AnalysisCache();
    IR ir = cache.getIR(m);
    final ISSABasicBlock returnNode = returnNode(ir.getControlFlowGraph());
    final int returnVal = returnVal(returnNode);

    {
      ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG =
          NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis(ir);
      intraExplodedCFG.compute(new NullProgressMonitor());
        
      final IExplodedBasicBlock returnNodeExploded = returnNodeExploded(returnNode, intraExplodedCFG.getCFG());
      final NullPointerState returnState = intraExplodedCFG.getState(returnNodeExploded);

      Assert.assertNotEquals(State.NOT_NULL, returnState.getState(returnVal));
      Assert.assertNotEquals(State.NULL, returnState.getState(returnVal));
    }
    {
      ExceptionPruningAnalysis<SSAInstruction, ISSABasicBlock> intraSSACFG =
          NullPointerAnalysis.createIntraproceduralSSACFGAnalyis(ir);
      intraSSACFG.compute(new NullProgressMonitor());
      
      Assert.assertEquals(ir.getControlFlowGraph().exit(), intraSSACFG.getCFG().exit());
      Assert.assertEquals(returnNode,           returnNode(intraSSACFG.getCFG()));
        
      final NullPointerState returnState = intraSSACFG.getState(returnNode);

      Assert.assertNotEquals(State.NOT_NULL, returnState.getState(returnVal));
      Assert.assertNotEquals(State.NULL, returnState.getState(returnVal));
    }    
  }

  
  
  @Test
  public void testWhile() throws UnsoundGraphException, CancelException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.intra.FieldAccess.testWhile(ZLcfg/exc/intra/B;)Lcfg/exc/intra/B");

    IMethod m = cha.resolveMethod(mr);
    AnalysisCache cache = new AnalysisCache();
    IR ir = cache.getIR(m);
    final ISSABasicBlock returnNode = returnNode(ir.getControlFlowGraph());
    final int returnVal = returnVal(returnNode);

    {
      ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG =
          NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis(ir);
      intraExplodedCFG.compute(new NullProgressMonitor());
        
      final IExplodedBasicBlock returnNodeExploded = returnNodeExploded(returnNode, intraExplodedCFG.getCFG());
      final NullPointerState returnState = intraExplodedCFG.getState(returnNodeExploded);

      Assert.assertNotEquals(State.NOT_NULL, returnState.getState(returnVal));
      Assert.assertNotEquals(State.NULL, returnState.getState(returnVal));
    }
    {
      ExceptionPruningAnalysis<SSAInstruction, ISSABasicBlock> intraSSACFG =
          NullPointerAnalysis.createIntraproceduralSSACFGAnalyis(ir);
      intraSSACFG.compute(new NullProgressMonitor());
      
      Assert.assertEquals(ir.getControlFlowGraph().exit(), intraSSACFG.getCFG().exit());
      Assert.assertEquals(returnNode,           returnNode(intraSSACFG.getCFG()));
        
      final NullPointerState returnState = intraSSACFG.getState(returnNode);

      Assert.assertNotEquals(State.NOT_NULL, returnState.getState(returnVal));
      Assert.assertNotEquals(State.NULL, returnState.getState(returnVal));
    }    
  }
  
  @Test
  public void testWhile2() throws UnsoundGraphException, CancelException {
    MethodReference mr = StringStuff.makeMethodReference("cfg.exc.intra.FieldAccess.testWhile2(ZLcfg/exc/intra/B;)Lcfg/exc/intra/B");

    IMethod m = cha.resolveMethod(mr);
    AnalysisCache cache = new AnalysisCache();
    IR ir = cache.getIR(m);
    final ISSABasicBlock returnNode = returnNode(ir.getControlFlowGraph());
    final int returnVal = returnVal(returnNode);

    {
      ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intraExplodedCFG =
          NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis(ir);
      intraExplodedCFG.compute(new NullProgressMonitor());
        
      final IExplodedBasicBlock returnNodeExploded = returnNodeExploded(returnNode, intraExplodedCFG.getCFG());
      final NullPointerState returnState = intraExplodedCFG.getState(returnNodeExploded);

      Assert.assertEquals(State.NOT_NULL, returnState.getState(returnVal));
    }
    {
      ExceptionPruningAnalysis<SSAInstruction, ISSABasicBlock> intraSSACFG =
          NullPointerAnalysis.createIntraproceduralSSACFGAnalyis(ir);
      intraSSACFG.compute(new NullProgressMonitor());
      
      Assert.assertEquals(ir.getControlFlowGraph().exit(), intraSSACFG.getCFG().exit());
      Assert.assertEquals(returnNode,           returnNode(intraSSACFG.getCFG()));
        
      final NullPointerState returnState = intraSSACFG.getState(returnNode);

      Assert.assertEquals(State.NOT_NULL, returnState.getState(returnVal));
    }    
  }

  private static ISSABasicBlock returnNode(ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg) {
      Collection<ISSABasicBlock> returnNodes = cfg.getNormalPredecessors(cfg.exit());
      Assert.assertEquals(1, returnNodes.size());
      return (ISSABasicBlock) returnNodes.toArray()[0];
  }
  
  private static int returnVal(ISSABasicBlock returnNode) {
    final SSAReturnInstruction returnInst = (SSAReturnInstruction) returnNode.getLastInstruction();
    Assert.assertEquals(1, returnInst.getNumberOfUses());
    return returnInst.getUse(0);
  }
  
  private static IExplodedBasicBlock returnNodeExploded(ISSABasicBlock returnNode, ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> explodedCfg) {
    final IExplodedBasicBlock exit = explodedCfg.exit();
    for (Iterator<IExplodedBasicBlock> it = explodedCfg.getPredNodes(exit); it.hasNext();) {
      IExplodedBasicBlock candidate  = it.next();
      if (candidate.getInstruction() == returnNode.getLastInstruction()) {
        return candidate;
      }
    }
    Assert.assertTrue(false);
    return null;
  }
}
