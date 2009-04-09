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

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.StringStuff;

/**
 * Test integrity of CFGs
 */
public class CFGTest extends WalaTestCase {

  public static void main(String[] args) {
    justThisTest(CFGTest.class);
  }

  /**
   * Build an IR, then check integrity on two flavors of CFG
   */
  private void doMethod(String methodSig) {
    try {
      AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(FileProvider.getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

      ClassHierarchy cha = ClassHierarchy.make(scope);

      MethodReference mr = StringStuff.makeMethodReference(methodSig);

      IMethod m = cha.resolveMethod(mr);
      if (m == null) {
        Assertions.UNREACHABLE("could not resolve " + mr);
      }
      AnalysisOptions options = new AnalysisOptions();
      AnalysisCache cache = new AnalysisCache();
      options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
      IR ir = cache.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions());

      ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = ir.getControlFlowGraph();
      try {
        GraphIntegrity.check(cfg);
      } catch (UnsoundGraphException e) {
        e.printStackTrace();
        System.err.println(ir);
        assertTrue(" failed cfg integrity check for " + methodSig, false);
      }

      try {
        GraphIntegrity.check(cfg);
      } catch (UnsoundGraphException e) {
        e.printStackTrace();
        System.err.println(ir);
        System.err.println(cfg);
        assertTrue(" failed 2-exit cfg integrity check for " + methodSig, false);
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  /**
   * this method does not exist in 1.5 libraries public void testFDBigInt() {
   * doMethod("java.lang.FDBigInt.class$(Ljava/lang/String;)Ljava/lang/Class;"); }
   */

  public void testResolveProxyClass() {
    doMethod("java.io.ObjectInputStream.resolveProxyClass([Ljava/lang/String;)Ljava/lang/Class;");
  }
}
