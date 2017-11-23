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

import java.util.Collection;
import java.util.function.Predicate;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ide.ui.SWTTreeViewer;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.io.FileProvider;

/**
 * This is a simple example WALA application. It's neither efficient nor concise, but is intended to demonstrate some basic
 * framework concepts.
 * 
 * This application builds a type hierarchy visualizes it with an SWT {@link TreeViewer}.
 */
public class SWTTypeHierarchy {
  // This example takes one command-line argument, so args[1] should be the "-classpath" parameter
  final static int CLASSPATH_INDEX = 1;

  /**
   * Usage: SWTTypeHierarchy -classpath [classpath]
   */
  public static void main(String[] args) {
    // check that the command-line is kosher
    validateCommandLine(args);
    run(args[CLASSPATH_INDEX]);
  }

  public static ApplicationWindow run(String classpath) {

    try {
      AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(classpath, (new FileProvider())
          .getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

      // invoke WALA to build a class hierarchy
      ClassHierarchy cha = ClassHierarchyFactory.make(scope);

      Graph<IClass> g = typeHierarchy2Graph(cha);
      g = pruneForAppLoader(g);

      // create and run the viewer
      final SWTTreeViewer v = new SWTTreeViewer();
      v.setGraphInput(g);
      Collection<IClass> roots = InferGraphRoots.inferRoots(g);
      if (roots.size() < 1) {
        System.err.println("PANIC: roots.size()=" + roots.size());
        System.exit(-1);
      }
      v.setRootsInput(roots);
      v.run();
      return v.getApplicationWindow();

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Return a view of an {@link IClassHierarchy} as a {@link Graph}, with edges from classes to immediate subtypes
   */
  public static Graph<IClass> typeHierarchy2Graph(IClassHierarchy cha) {
    Graph<IClass> result = SlowSparseNumberedGraph.make();
    for (IClass c : cha) {
      result.addNode(c);
    }
    for (IClass c : cha) {
      for (IClass x : cha.getImmediateSubclasses(c)) {
        result.addEdge(c, x);
      }
      if (c.isInterface()) {
        for (IClass x : cha.getImplementors(c.getReference())) {
          result.addEdge(c, x);
        }
      }
    }
    return result;
  }

  /**
   * Restrict g to nodes from the Application loader
   */
  static Graph<IClass> pruneForAppLoader(Graph<IClass> g) {
    Predicate<IClass> f = c -> (c.getClassLoader().getReference().equals(ClassLoaderReference.Application));
    return pruneGraph(g, f);
  }

  /**
   * Remove from a graph g any nodes that are not accepted by a {@link Predicate}
   */
  public static <T> Graph<T> pruneGraph(Graph<T> g, Predicate<T> f) {
    Collection<T> slice = GraphSlicer.slice(g, f);
    return GraphSlicer.prune(g, new CollectionFilter<>(slice));
  }

  /**
   * Validate that the command-line arguments obey the expected usage.
   * 
   * Usage: args[0] : "-classpath" args[1] : String, a ";"-delimited class path
   * 
   * @throws UnsupportedOperationException if command-line is malformed.
   */
  static void validateCommandLine(String[] args) {
    if (args.length < 2) {
      throw new UnsupportedOperationException("must have at least 2 command-line arguments");
    }
    if (!args[0].equals("-classpath")) {
      throw new UnsupportedOperationException("invalid command-line, args[0] should be -classpath, but is " + args[0]);
    }
  }
}
