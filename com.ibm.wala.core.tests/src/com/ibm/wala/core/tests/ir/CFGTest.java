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
import com.ibm.wala.cfg.TwoExitCFG;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.JavaScopeUtil;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.StringStuff;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.warnings.WarningSet;

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
      EJavaAnalysisScope escope = JavaScopeUtil.makePrimordialScope();

      // generate a DOMO-consumable wrapper around the incoming scope object
      EMFScopeWrapper scope = EMFScopeWrapper.generateScope(escope);
      
      // invoke DOMO to build a DOMO class hierarchy object
      WarningSet warnings = new WarningSet();
      ClassHierarchy cha = ClassHierarchy.make(scope, warnings);

      MethodReference mr = StringStuff.makeMethodReference(methodSig);

      IMethod m = cha.resolveMethod(mr);
      if (m == null) {
        Assertions.UNREACHABLE("could not resolve " + mr);
      }
      AnalysisOptions options = new AnalysisOptions();
      options.getSSAOptions().setUsePiNodes(true);
      IR ir = options.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, cha, options.getSSAOptions(), new WarningSet());

      ControlFlowGraph cfg = ir.getControlFlowGraph();
      try {
        GraphIntegrity.check(cfg);
      } catch (UnsoundGraphException e) {
        e.printStackTrace();
        Trace.println(ir);
        assertTrue(" failed cfg integrity check for " + methodSig, false);
      }

      cfg = new TwoExitCFG(cfg);
      try {
        GraphIntegrity.check(cfg);
      } catch (UnsoundGraphException e) {
        e.printStackTrace();
        Trace.println(ir);
        Trace.println(cfg);
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
