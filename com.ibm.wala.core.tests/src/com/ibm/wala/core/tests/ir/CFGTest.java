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
package com.ibm.wala.core.tests.ir;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.StringStuff;

/**
 * Test integrity of CFGs
 */
public class CFGTest extends WalaTestCase {
  
  private final IClassHierarchy cha;

 protected CFGTest(IClassHierarchy cha) {
   this.cha = cha;
 }
 
 public CFGTest() throws ClassHierarchyException, IOException {
   this(WalaTestCase.makeCHA());
 }
 
  public static void main(String[] args) {
    justThisTest(CFGTest.class);
  }

  /**
   * Build an IR, then check integrity on two flavors of CFG
   */
  private void doMethod(String methodSig) {
    try {
      MethodReference mr = StringStuff.makeMethodReference(Language.JAVA, methodSig);

      IMethod m = cha.resolveMethod(mr);
      if (m == null) {
        Assertions.UNREACHABLE("could not resolve " + mr);
      }
      AnalysisOptions options = new AnalysisOptions();
      options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
      IAnalysisCacheView cache = makeAnalysisCache(options.getSSAOptions());
      IR ir = cache.getIR(m, Everywhere.EVERYWHERE);

      ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = ir.getControlFlowGraph();
      try {
        GraphIntegrity.check(cfg);
      } catch (UnsoundGraphException e) {
        e.printStackTrace();
        System.err.println(ir);
        Assert.assertTrue(" failed cfg integrity check for " + methodSig, false);
      }

      try {
        GraphIntegrity.check(cfg);
      } catch (UnsoundGraphException e) {
        e.printStackTrace();
        System.err.println(ir);
        System.err.println(cfg);
        Assert.assertTrue(" failed 2-exit cfg integrity check for " + methodSig, false);
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  /**
   * this method does not exist in 1.5 libraries @Test public void
   * testFDBigInt() {
   * doMethod("java.lang.FDBigInt.class$(Ljava/lang/String;)Ljava/lang/Class;");
   * }
   */

  @Test
  public void testResolveProxyClass() {
    doMethod("java.io.ObjectInputStream.resolveProxyClass([Ljava/lang/String;)Ljava/lang/Class;");
  }

  @Test
  public void testIRCacheIdempotence() {
    MethodReference mr = StringStuff.makeMethodReference("hello.Hello.main([Ljava/lang/String;)V");

    IMethod m = cha.resolveMethod(mr);
    IAnalysisCacheView cache = makeAnalysisCache();
    IR irBefore = cache.getIR(m);
    cache.clear(); 
    IR irAfter = cache.getIR(m);
    for (int i = 0; i < irBefore.getInstructions().length; i++) {
      System.out.println(irBefore.getInstructions()[i]);
      System.out.println(irAfter.getInstructions()[i]);
      Assert.assertEquals(irAfter.getInstructions()[i], irBefore.getInstructions()[i]);
    }
  }

  @Test
  public void testSync1() {
    MethodReference mr = StringStuff.makeMethodReference("cfg.MonitorTest.sync1()V");

    IMethod m = cha.resolveMethod(mr);
    IAnalysisCacheView cache = makeAnalysisCache();
    IR ir = cache.getIR(m);
    System.out.println(ir);
    SSACFG controlFlowGraph = ir.getControlFlowGraph();
    Assert.assertEquals(1, controlFlowGraph.getSuccNodeCount(controlFlowGraph.getBlockForInstruction(21)));
  }

  @Test
  public void testSync2() {
    MethodReference mr = StringStuff.makeMethodReference("cfg.MonitorTest.sync2()V");

    IMethod m = cha.resolveMethod(mr);
    IAnalysisCacheView cache = makeAnalysisCache();
    IR ir = cache.getIR(m);
    System.out.println(ir);
    SSACFG controlFlowGraph = ir.getControlFlowGraph();
    IntSet succs = controlFlowGraph.getSuccNodeNumbers(controlFlowGraph.getBlockForInstruction(13));
    Assert.assertEquals(2, succs.size());
    Assert.assertTrue(succs.contains(6));
    Assert.assertTrue(succs.contains(7));
  }
  
  @Test
  public void testSync3() {
    MethodReference mr = StringStuff.makeMethodReference("cfg.MonitorTest.sync3()V");

    IMethod m = cha.resolveMethod(mr);
    IAnalysisCacheView cache = makeAnalysisCache();
    IR ir = cache.getIR(m);
    SSACFG controlFlowGraph = ir.getControlFlowGraph();
    Assert.assertEquals(1, controlFlowGraph.getSuccNodeCount(controlFlowGraph.getBlockForInstruction(33)));
  }

  public static void testCFG(SSACFG cfg, int[][] assertions) {
  	for(int i = 0; i < assertions.length; i++) {
  		SSACFG.BasicBlock bb= cfg.getNode(i);
  		Assert.assertEquals("basic block " + i, assertions[i].length, cfg.getSuccNodeCount(bb));
  		for(int j = 0; j < assertions[i].length; j++) {
  			Assert.assertTrue(cfg.hasEdge(bb, cfg.getNode(assertions[i][j])));
  		}
  	}
  }
}
