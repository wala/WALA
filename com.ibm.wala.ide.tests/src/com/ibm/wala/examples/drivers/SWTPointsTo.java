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
package com.ibm.wala.examples.drivers;

import java.io.IOException;
import java.util.Properties;

import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.analysis.pointers.BasicHeapGraph;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ide.ui.SWTTreeViewer;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;

/**
 * 
 * This application is a WALA client: it invokes an SWT TreeViewer to visualize
 * a Points-To solution
 * 
 * @author sfink
 */
public class SWTPointsTo {

  /**
   * Usage: SWTPointsTo -appJar [jar file name] The "jar file name" should be
   * something like "c:/temp/testdata/java_cup.jar"
   * 
   * @param args
   * @throws WalaException
   */
  public static void main(String[] args) throws WalaException {
    Properties p = CommandLine.parse(args);
    PDFCallGraph.validateCommandLine(p);
    run(p.getProperty("appJar"));
  }

  /**
   * @param appJar
   *            should be something like "c:/temp/testdata/java_cup.jar"
   */
  public static ApplicationWindow run(String appJar) {

    try {
      Graph<Object> g = buildPointsTo(appJar);

      // create and run the viewer
      final SWTTreeViewer v = new SWTTreeViewer();
      v.setGraphInput(g);
      v.setRootsInput(InferGraphRoots.inferRoots(g));
      v.run();
      return v.getApplicationWindow();

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static Graph<Object> buildPointsTo(String appJar) throws WalaException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

    
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);

    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

    // //
    // build the call graph
    // //
    com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(options, new AnalysisCacheImpl(),cha, scope, null, null);
    CallGraph cg = builder.makeCallGraph(options,null);
    PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
    
    System.err.println(pointerAnalysis);
    
    return new BasicHeapGraph<>(pointerAnalysis, cg);
  }
}
