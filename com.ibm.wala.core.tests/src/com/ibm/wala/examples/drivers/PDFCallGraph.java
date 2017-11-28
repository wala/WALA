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
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.function.Predicate;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.io.FileUtil;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.PDFViewUtil;

/**
 * This simple example WALA application builds a call graph and fires off ghostview to visualize a DOT representation.
 */
public class PDFCallGraph {
  public static boolean isDirectory(String appJar) {
    return (new File(appJar).isDirectory());
  }

  public static String findJarFiles(String[] directories) {
    Collection<String> result = HashSetFactory.make();
    for (String directorie : directories) {
      for (File f : FileUtil.listFiles(directorie, ".*\\.jar", true)) {
        result.add(f.getAbsolutePath());
      }
    }
    return composeString(result);
  }
  
  private static String composeString(Collection<String> s) {
    StringBuffer result = new StringBuffer();
    Iterator<String> it = s.iterator();
    for (int i = 0; i < s.size() - 1; i++) {
      result.append(it.next());
      result.append(File.pathSeparator);
    }
    if (it.hasNext()) {
      result.append(it.next());
    }
    return result.toString();
  }  
  
  private final static String PDF_FILE = "cg.pdf";

  /**
   * Usage: args = "-appJar [jar file name] {-exclusionFile [exclusionFileName]}" The "jar file name" should be something like
   * "c:/temp/testdata/java_cup.jar"
   * 
   * @throws CancelException
   * @throws IllegalArgumentException
   */
  public static void main(String[] args) throws IllegalArgumentException, CancelException {
    run(args);
  }

  /**
   * Usage: args = "-appJar [jar file name] {-exclusionFile [exclusionFileName]}" The "jar file name" should be something like
   * "c:/temp/testdata/java_cup.jar"
   * 
   * @throws CancelException
   * @throws IllegalArgumentException
   */
  public static Process run(String[] args) throws IllegalArgumentException, CancelException {
    Properties p = CommandLine.parse(args);
    validateCommandLine(p);
    return run(p.getProperty("appJar"), p.getProperty("exclusionFile", CallGraphTestUtil.REGRESSION_EXCLUSIONS));
  }

  /**
   * @param appJar something like "c:/temp/testdata/java_cup.jar"
   * @throws CancelException
   * @throws IllegalArgumentException
   */
  public static Process run(String appJar, String exclusionFile) throws IllegalArgumentException, CancelException {
    try {
      Graph<CGNode> g = buildPrunedCallGraph(appJar, (new FileProvider()).getFile(exclusionFile));

      Properties p = null;
      try {
        p = WalaExamplesProperties.loadProperties();
        p.putAll(WalaProperties.loadProperties());
      } catch (WalaException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      String pdfFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDF_FILE;

      String dotExe = p.getProperty(WalaExamplesProperties.DOT_EXE);
      DotUtil.dotify(g, null, PDFTypeHierarchy.DOT_FILE, pdfFile, dotExe);

      String gvExe = p.getProperty(WalaExamplesProperties.PDFVIEW_EXE);
      return PDFViewUtil.launchPDFView(pdfFile, gvExe);

    } catch (WalaException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * @param appJar something like "c:/temp/testdata/java_cup.jar"
   * @return a call graph
   * @throws CancelException
   * @throws IllegalArgumentException
   * @throws IOException 
   */
  public static Graph<CGNode> buildPrunedCallGraph(String appJar, File exclusionFile) throws WalaException,
      IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, exclusionFile != null ? exclusionFile : new File(
        CallGraphTestUtil.REGRESSION_EXCLUSIONS));

    ClassHierarchy cha = ClassHierarchyFactory.make(scope);

    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

    // //
    // build the call graph
    // //
    com.ibm.wala.ipa.callgraph.CallGraphBuilder<InstanceKey> builder = Util.makeZeroCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    System.err.println(CallGraphStats.getStats(cg));

    Graph<CGNode> g = pruneForAppLoader(cg);

    return g;
  }

  public static Graph<CGNode> pruneForAppLoader(CallGraph g) {
    return PDFTypeHierarchy.pruneGraph(g, new ApplicationLoaderFilter());
  }

  /**
   * Validate that the command-line arguments obey the expected usage.
   * 
   * Usage:
   * <ul>
   * <li>args[0] : "-appJar"
   * <li> args[1] : something like "c:/temp/testdata/java_cup.jar" </ul?
   * 
   * @throws UnsupportedOperationException if command-line is malformed.
   */
  public static void validateCommandLine(Properties p) {
    if (p.get("appJar") == null) {
      throw new UnsupportedOperationException("expected command-line to include -appJar");
    }
  }

  /**
   * A filter that accepts WALA objects that "belong" to the application loader.
   * 
   * Currently supported WALA types include
   * <ul>
   * <li> {@link CGNode}
   * <li> {@link LocalPointerKey}
   * </ul>
   */
  private static class ApplicationLoaderFilter implements Predicate<CGNode> {

    @Override public boolean test(CGNode o) {
      if (o == null)
        return false;
      return o.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }
  }
}
