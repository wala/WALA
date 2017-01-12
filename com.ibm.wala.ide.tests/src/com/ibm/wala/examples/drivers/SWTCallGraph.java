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
import java.util.Properties;
import java.util.jar.JarFile;

import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ide.ui.SWTTreeViewer;
import com.ibm.wala.ide.ui.ViewIRAction;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;

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
    PDFCallGraph.validateCommandLine(p);
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
      if (PDFCallGraph.isDirectory(appJar)) {
        appJar = PDFCallGraph.findJarFiles(new String[] { appJar });
      }

      String exclusionFile = p.getProperty("exclusions");

      AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, exclusionFile != null ? new File(exclusionFile)
          : (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

      ClassHierarchy cha = ClassHierarchyFactory.make(scope);

      Iterable<Entrypoint> entrypoints = null;
      JarFile jar = new JarFile(appJar);
      if (jar.getManifest() != null) {
        String mainClass = jar.getManifest().getMainAttributes().getValue("Main-Class");
        if (mainClass != null) {
          entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, "L" + mainClass.replace('.', '/'));
        }
      }
      if (entrypoints == null) {
        entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
      }
      
      AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
      options.setReflectionOptions(ReflectionOptions.ONE_FLOW_TO_CASTS_NO_METHOD_INVOKE);
      
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
      String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFWalaIR.PDF_FILE;
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
}
