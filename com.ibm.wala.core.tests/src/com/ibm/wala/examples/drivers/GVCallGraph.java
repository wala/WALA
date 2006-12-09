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

import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.JavaScopeUtil;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.WarningSet;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.GVUtil;

/**
 * 
 * This simple example WALA application builds a call graph and fires off
 * ghostview to viz a DOT representation.
 * 
 * @author sfink
 */
public class GVCallGraph {

  private final static String PS_FILE = "cg.ps";

  /**
   * Usage: GVCallGraph -appJar [jar file name] The "jar file name" should
   * be something like "c:/temp/testdata/java_cup.jar"
   * 
   * @param args
   * @throws WalaException 
   */
  public static void main(String[] args) throws WalaException {
    run(args);
  }

  /**
   * Usage: args = "-appJar [jar file name] " The "jar file name" should be
   * something like "c:/temp/testdata/java_cup.jar"
   * 
   * @param args
   * @throws WalaException 
   */
  public static Process run(String[] args) throws WalaException {
    Properties p = CommandLine.parse(args);
    validateCommandLine(p);
    return run(p.getProperty("appJar"));
  }

  /**
   * @param appJar
   *          something like "c:/temp/testdata/java_cup.jar"
   */
  public static Process run(String appJar) {
    try {

      Graph<CGNode> g = buildPrunedCallGraph(appJar);

      Properties p = null;
      try {
        p = WalaExamplesProperties.loadProperties();
        p.putAll(WalaProperties.loadProperties());
      } catch (WalaException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      String psFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PS_FILE;

      String dotExe = p.getProperty(WalaExamplesProperties.DOT_EXE);
      DotUtil.dotify(g, null, GVTypeHierarchy.DOT_FILE, psFile, dotExe);

      String gvExe = p.getProperty(WalaExamplesProperties.GHOSTVIEW_EXE);
      return GVUtil.launchGV(psFile, gvExe);

    } catch (WalaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  /**
   * @param appJar
   *          something like "c:/temp/testdata/java_cup.jar"
   * @return a call graph
   * @throws WalaException
   */
  public static Graph<CGNode> buildPrunedCallGraph(String appJar) throws WalaException {
    EJavaAnalysisScope escope = JavaScopeUtil.makeAnalysisScope(appJar);

    // generate a DOMO-consumable wrapper around the incoming scope object
    EMFScopeWrapper scope = EMFScopeWrapper.generateScope(escope);
    

    // TODO: return the warning set (need a CAPA type)
    // invoke DOMO to build a DOMO class hierarchy object
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);

    Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

    // //
    // build the call graph
    // //
    com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, cha, scope, warnings, null, null);
    CallGraph cg = builder.makeCallGraph(options);

    Graph<CGNode> g = pruneForAppLoader(cg);
    return g;
  }

  static Graph<CGNode> pruneForAppLoader(CallGraph g) throws WalaException {
    return GVTypeHierarchy.pruneGraph(g, new ApplicationLoaderFilter());
  }

  /**
   * Validate that the command-line arguments obey the expected usage.
   * 
   * Usage:
   * <ul>
   * <li> args[0] : "-appJar"
   * <li> args[1] : something like "c:/temp/testdata/java_cup.jar" </ul?
   * 
   * @throws UnsupportedOperationException
   *           if command-line is malformed.
   */
  static void validateCommandLine(Properties p) {
    if (p.get("appJar") == null) {
      throw new UnsupportedOperationException("expected command-line to include -appJar");
    }
  }

  /**
   * @author sfink
   * 
   * A filter that accepts domo objects that "belong" to the application loader.
   * 
   * Currently supported DOMO types include
   * <ul>
   * <li> CGNode
   * <li> LocalPointerKey
   * </ul>
   */
  private static class ApplicationLoaderFilter implements Filter {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.capa.util.collections.Filter#accepts(java.lang.Object)
     */
    public boolean accepts(Object o) {
      if (o instanceof CGNode) {
        CGNode n = (CGNode) o;
        return n.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
      } else if (o instanceof LocalPointerKey) {
        LocalPointerKey l = (LocalPointerKey) o;
        return accepts(l.getNode());
      } else {
        return false;
      }
    }

  }
}