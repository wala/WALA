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

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.JavaScopeUtil;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.HeapStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.NodeDecorator;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.GVUtil;

/**
 * 
 * This simple example WALA application builds an SDG and fires off ghostview to
 * viz a DOT representation.
 * 
 * @author sfink
 */
public class GVSDG {

  private final static String PS_FILE = "sdg.ps";

  /**
   * Usage: GVSDG -appJar [jar file name] -mainclass [main class]
   * 
   * The "jar file name" should be something like
   * "c:/temp/testdata/java_cup.jar"
   * 
   * @param args
   * @throws WalaException
   */
  public static void main(String[] args) throws WalaException {
    run(args);
  }

  /**
   * @throws WalaException
   */
  public static Process run(String[] args) throws WalaException {
    Properties p = CommandLine.parse(args);
    validateCommandLine(p);
    return run(p.getProperty("appJar"), p.getProperty("mainClass"), getDataDependenceOptions(p), getControlDependenceOptions(p));
  }

  public static DataDependenceOptions getDataDependenceOptions(Properties p) {
    String d = p.getProperty("dd", "full");
    for (DataDependenceOptions result : DataDependenceOptions.values()) {
      if (d.equals(result.getName())) {
        return result;
      }
    }
    Assertions.UNREACHABLE("unknown data datapendence option: " + d);
    return null;
  }

  public static ControlDependenceOptions getControlDependenceOptions(Properties p) {
    String d = p.getProperty("cd", "full");
    for (ControlDependenceOptions result : ControlDependenceOptions.values()) {
      if (d.equals(result.getName())) {
        return result;
      }
    }
    Assertions.UNREACHABLE("unknown control datapendence option: " + d);
    return null;
  }

  /**
   * @param appJar
   *            something like "c:/temp/testdata/java_cup.jar"
   */
  public static Process run(String appJar, String mainClass, DataDependenceOptions dOptions, ControlDependenceOptions cOptions) {
    try {
      EJavaAnalysisScope escope = JavaScopeUtil.makeAnalysisScope(appJar);

      // generate a WALA-consumable wrapper around the incoming scope object
      EMFScopeWrapper scope = EMFScopeWrapper.generateScope(escope);
      ClassHierarchy cha = ClassHierarchy.make(scope);
      Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
      AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

      CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
      CallGraph cg = builder.makeCallGraph(options);
      SDG sdg = new SDG(cg, builder.getPointerAnalysis(), dOptions, cOptions);
      try {
        GraphIntegrity.check(sdg);
      } catch (UnsoundGraphException e1) {
        e1.printStackTrace();
        Assertions.UNREACHABLE();
      }
      System.err.println(sdg);

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
      Graph<Statement> g = pruneSDG(sdg);
      DotUtil.dotify(g, makeNodeDecorator(), GVTypeHierarchy.DOT_FILE, psFile, dotExe);

      String gvExe = p.getProperty(WalaExamplesProperties.GHOSTVIEW_EXE);
      return GVUtil.launchGV(psFile, gvExe);

    } catch (WalaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  private static Graph<Statement> pruneSDG(final SDG sdg) {
    Filter f = new Filter() {
      public boolean accepts(Object o) {
        Statement s = (Statement) o;
        if (s.getNode().equals(sdg.getCallGraph().getFakeRootNode())) {
          return false;
        } else {
          return true;
        }
      }
    };
    return GraphSlicer.prune(sdg, f);
  }

  private static NodeDecorator makeNodeDecorator() {
    return new NodeDecorator() {
      public String getLabel(Object o) throws WalaException {
        Statement s = (Statement) o;
        switch (s.getKind()) {
        case HEAP_PARAM_CALLEE:
        case HEAP_PARAM_CALLER:
        case HEAP_RET_CALLEE:
        case HEAP_RET_CALLER:
          HeapStatement h = (HeapStatement) s;
          return s.getKind() + "\\n" + h.getNode() + "\\n" + h.getLocation();
        case EXC_RET_CALLEE:
        case EXC_RET_CALLER:
        case NORMAL:
        case NORMAL_RET_CALLEE:
        case NORMAL_RET_CALLER:
        case PARAM_CALLEE:
        case PARAM_CALLER:
        case PHI:
        default:
          return s.toString();
        }
      }

    };
  }

  /**
   * Validate that the command-line arguments obey the expected usage.
   * 
   * Usage:
   * <ul>
   * <li> args[0] : "-appJar"
   * <li> args[1] : something like "c:/temp/testdata/java_cup.jar"
   * <li> args[2] : "-mainClass"
   * <li> args[3] : something like "Lslice/TestRecursion"
   * 
   * @throws UnsupportedOperationException
   *             if command-line is malformed.
   */
  static void validateCommandLine(Properties p) {
    if (p.get("appJar") == null) {
      throw new UnsupportedOperationException("expected command-line to include -appJar");
    }
    if (p.get("mainClass") == null) {
      throw new UnsupportedOperationException("expected command-line to include -appJar");
    }
  }
}