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
package com.ibm.wala.examples.drivers;

import java.io.IOException;
import java.util.Properties;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.examples.analysis.dataflow.ContextSensitiveReachingDefs;
import com.ibm.wala.ide.ui.IFDSExplorer;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.io.CommandLine;

/**
 * An example of using {@link IFDSExplorer}. We visualize the result of running {@link ContextSensitiveReachingDefs} on a simple
 * test example.
 */
public class IFDSExplorerExample {

  /**
   * Usage: IFDSExplorerExample -dotExe <path_to_dot_exe> -viewerExe <path_to_viewer_exe>
   * 
   * @param args
   * @throws IOException
   * @throws CallGraphBuilderCancelException
   * @throws IllegalArgumentException
   * @throws WalaException
   */
  public static void main(String[] args) throws IOException, IllegalArgumentException, CallGraphBuilderCancelException,
      WalaException {
    Properties p = CommandLine.parse(args);
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, "Java60RegressionExclusions.txt");
    IClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "Ldataflow/StaticDataflow");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    AnalysisCache cache = new AnalysisCacheImpl();
    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(options, cache, cha, scope);
    System.out.println("building CG");
    CallGraph cg = builder.makeCallGraph(options, null);
    System.out.println("done with CG");
    System.out.println("computing reaching defs");
    ContextSensitiveReachingDefs reachingDefs = new ContextSensitiveReachingDefs(cg);
    TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> result = reachingDefs.analyze();
    System.out.println("done with reaching defs");
    IFDSExplorer.setDotExe(p.getProperty("dotExe"));
    IFDSExplorer.setGvExe(p.getProperty("viewerExe"));
    IFDSExplorer.viewIFDS(result);
  }

}
