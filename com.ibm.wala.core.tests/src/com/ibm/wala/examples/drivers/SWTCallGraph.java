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

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.io.FileUtil;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.viz.SWTTreeViewer;
import com.ibm.wala.viz.ViewIRAction;

/**
 * 
 * This application is a WALA client: it invokes an SWT TreeViewer to visualize
 * a Call Graph
 * 
 * @author sfink
 */
public class SWTCallGraph {

  private final static boolean CHECK_GRAPH = false;

  /**
   * Usage: SWTCallGraph -appJar [jar file name]
   * 
   * The "jar file name" should be something like
   * "c:/temp/testdata/java_cup.jar"
   * 
   * If it's a directory, then we'll try to find all jar files under that
   * directory.
   * 
   * @param args
   * @throws WalaException
   */
  public static void main(String[] args) throws WalaException {
    Properties p = CommandLine.parse(args);
    GVCallGraph.validateCommandLine(p);
    run(p);
  }

  /**
   * @param p
   *            should contain at least the following properties:
   *            <ul>
   *            <li>appJar should be something like
   *            "c:/temp/testdata/java_cup.jar"
   *            <li>algorithm (optional) can be one of:
   *            <ul>
   *            <li> "ZERO_CFA" (default value)
   *            <li> "RTA"
   *            </ul>
   *            </ul>
   * 
   * @throws WalaException
   */
  public static ApplicationWindow run(Properties p) throws WalaException {

    try {
      String appJar = p.getProperty("appJar");
      if (isDirectory(appJar)) {
        appJar = SWTCallGraph.findJarFiles(new String[] { appJar });
      }

      String exclusionFile = p.getProperty("exclusions");

      AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, exclusionFile != null ? new File(exclusionFile)
          : FileProvider.getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

      ClassHierarchy cha = ClassHierarchy.make(scope);

      Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
      AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

      // //
      // build the call graph
      // //
      com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope, null,
          null);
      CallGraph cg = builder.makeCallGraph(options,null);

      System.out.println(CallGraphStats.getStats(cg));

      if (CHECK_GRAPH) {
        GraphIntegrity.check(cg);
      }

      Properties wp = null;
      try {
        wp = WalaProperties.loadProperties();
        wp.putAll(WalaExamplesProperties.loadProperties());
      } catch (WalaException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + GVWalaIR.PS_FILE;
      String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;
      String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
      String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);

      // create and run the viewer
      final SWTTreeViewer v = new SWTTreeViewer();
      v.setGraphInput(cg);
      v.setRootsInput(InferGraphRoots.inferRoots(cg));
      v.getPopUpActions().add(new ViewIRAction(v, cg, psFile, dotFile, dotExe, gvExe));
      v.run();
      return v.getApplicationWindow();

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  // private static CallGraphConstructionAlgorithm chooseAlgorithm(Properties p)
  // throws CapaException {
  // String alg = p.getProperty("algorithm", "ZERO_CFA");
  // if (alg.equals("ZERO_CFA")) {
  // return CallGraphConstructionAlgorithm.ZERO_CFA_LITERAL;
  // } else if (alg.equals("RTA")) {
  // return CallGraphConstructionAlgorithm.RTA_LITERAL;
  // } else if (alg.equals("ZERO_ONE_CFA")) {
  // return CallGraphConstructionAlgorithm.VANILLA_ZERO_ONE_CFA_LITERAL;
  // } else {
  // throw new CapaException("Unsupported algorithm: " + alg);
  // }
  // }

  static boolean isDirectory(String appJar) {
    return (new File(appJar).isDirectory());
  }

  public static String findJarFiles(String[] directories) throws WalaException {
    Collection<String> result = HashSetFactory.make();
    for (int i = 0; i < directories.length; i++) {
      for (Iterator<File> it = FileUtil.listFiles(directories[i], ".*\\.jar", true).iterator(); it.hasNext();) {
        File f = (File) it.next();
        result.add(f.getAbsolutePath());
      }
    }
    return composeString(result);
  }

  /**
   * @param s
   *            Collection<String>
   */
  private static String composeString(Collection<String> s) {
    StringBuffer result = new StringBuffer();
    Iterator<String> it = s.iterator();
    for (int i = 0; i < s.size() - 1; i++) {
      result.append(it.next());
      result.append(';');
    }
    if (it.hasNext()) {
      result.append(it.next());
    }
    return result.toString();
  }
}
