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
package com.ibm.wala.core.tests.ir;

import static com.ibm.wala.util.graph.EdgeManagerConditions.edge;
import static com.ibm.wala.util.intset.IntSetAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatObject;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.strings.StringStuff;
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
import com.ibm.wala.util.intset.IntSet;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Test integrity of CFGs */
public abstract class CFGTest extends WalaTestCase {

  private final IClassHierarchy cha;

  protected CFGTest(IClassHierarchy cha) {
    this.cha = cha;
  }

  public CFGTest() throws ClassHierarchyException, IOException {
    this(AnnotationTest.makeCHA());
  }

  public static void main(String[] args) {
    justThisTest(CFGTest.class);
  }

  /** Build an IR, then check integrity on two flavors of CFG */
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
      assertThatCode(() -> GraphIntegrity.check(cfg))
          .describedAs("cfg integrity check for %s", methodSig)
          .doesNotThrowAnyException();
    } catch (Exception e) {
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  /**
   * this method does not exist in 1.5 libraries @Test public void testFDBigInt() {
   * doMethod("java.lang.FDBigInt.class$(Ljava/lang/String;)Ljava/lang/Class;"); }
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
    }
    assertThat(irAfter.getInstructions()).containsExactly(irBefore.getInstructions());
  }

  @Test
  public void testSync1() {
    MethodReference mr = StringStuff.makeMethodReference("cfg.MonitorTest.sync1()V");

    IMethod m = cha.resolveMethod(mr);
    IAnalysisCacheView cache = makeAnalysisCache();
    IR ir = cache.getIR(m);
    System.out.println(ir);
    SSACFG controlFlowGraph = ir.getControlFlowGraph();
    assertThat(controlFlowGraph.getSuccNodeCount(controlFlowGraph.getBlockForInstruction(21)))
        .isEqualTo(1);
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
    assertThat(succs).toCollection().containsExactly(6, 7);
  }

  @Test
  public void testSync3() {
    MethodReference mr = StringStuff.makeMethodReference("cfg.MonitorTest.sync3()V");

    IMethod m = cha.resolveMethod(mr);
    IAnalysisCacheView cache = makeAnalysisCache();
    IR ir = cache.getIR(m);
    SSACFG controlFlowGraph = ir.getControlFlowGraph();
    assertThat(controlFlowGraph.getSuccNodeCount(controlFlowGraph.getBlockForInstruction(33)))
        .isEqualTo(1);
  }

  public static void testCFG(SSACFG cfg, int[][] assertions) {
    for (int i = 0; i < assertions.length; i++) {
      SSACFG.BasicBlock bb = cfg.getNode(i);
      assertThat(cfg.getSuccNodeCount(bb)).isEqualTo(assertions[i].length);
      for (int j = 0; j < assertions[i].length; j++) {
        assertThatObject(cfg).has(edge(bb, cfg.getNode(assertions[i][j])));
      }
    }
  }
}
