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
import java.util.Properties;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.util.config.AnalysisScopeReader;

import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.GVUtil;

/**
 * 
 * This simple example WALA application builds a TypeHierarchy and fires off
 * ghostview to viz a DOT representation.
 * 
 * @author sfink
 */
public class GVTypeHierarchy {

  public final static String DOT_FILE = "temp.dt";

  private final static String PS_FILE = "th.ps";

  public static Properties p;

  static {
    try {
      p = WalaProperties.loadProperties();
      p.putAll(WalaExamplesProperties.loadProperties());
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }

  }

  public static void main(String[] args) throws IOException {
    run(args);
  }

  public static Process run(String[] args) throws IOException {
    try {
      SWTTypeHierarchy.validateCommandLine(args);
      String classpath = args[SWTTypeHierarchy.CLASSPATH_INDEX];
      AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(classpath, FileProvider.getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

      // invoke WALA to build a class hierarchy
      ClassHierarchy cha = ClassHierarchy.make(scope);

      Graph<IClass> g = SWTTypeHierarchy.typeHierarchy2Graph(cha);

      g = SWTTypeHierarchy.pruneForAppLoader(g);
      String dotFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + DOT_FILE;
      String psFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PS_FILE;
      String dotExe = p.getProperty(WalaExamplesProperties.DOT_EXE);
      String gvExe = p.getProperty(WalaExamplesProperties.GHOSTVIEW_EXE);
      DotUtil.dotify(g, null, dotFile, psFile, dotExe);
      return GVUtil.launchGV(psFile, gvExe);

    } catch (WalaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  public static <T> Graph<T> pruneGraph(Graph<T> g, Filter<T> f) throws WalaException {
    Collection<T> slice = GraphSlicer.slice(g, f);
    return GraphSlicer.prune(g, new CollectionFilter<T>(slice));
  }
}