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
import java.io.InputStream;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.cfg.CFGSanitizer;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.io.FileProvider;

/**
 * Test integrity of CFGs
 */
public class CFGSanitizerTest extends WalaTestCase {

  /**
   * check that for all synthetic methods coming from the native specifications, the exit block is not disconnected from the rest of
   * the sanitized graph
   * 
   * @throws IOException
   * @throws IllegalArgumentException
   * @throws WalaException
   */
  @Test
  public void testSyntheticEdgeToExit() throws IOException, IllegalArgumentException, WalaException {
    AnalysisScope scope = AnalysisScopeReader.makePrimordialScope((new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    ClassLoader cl = CFGSanitizerTest.class.getClassLoader();
    XMLMethodSummaryReader summary;
    try (final InputStream s = cl.getResourceAsStream("natives.xml")) {
      summary = new XMLMethodSummaryReader(s, scope);
    }
    AnalysisOptions options = new AnalysisOptions(scope, null);
    Map<MethodReference, MethodSummary> summaries = summary.getSummaries();
    for (MethodReference mr : summaries.keySet()) {
      IMethod m = cha.resolveMethod(mr);
      if (m == null) {
        continue;
      }
      System.out.println(m.getSignature());
      MethodSummary methodSummary = summaries.get(mr);
      SummarizedMethod summMethod = new SummarizedMethod(mr, methodSummary, m.getDeclaringClass());
      IR ir = summMethod.makeIR(Everywhere.EVERYWHERE, options.getSSAOptions());
      System.out.println(ir);
      Graph<ISSABasicBlock> graph = CFGSanitizer.sanitize(ir, cha);
      System.out.println(graph);
      BasicBlock exit = ir.getControlFlowGraph().exit();
      if (!exit.equals(ir.getControlFlowGraph().entry())) {
        Assert.assertTrue(graph.getPredNodeCount(exit) > 0);
      }
    }
  }

}
