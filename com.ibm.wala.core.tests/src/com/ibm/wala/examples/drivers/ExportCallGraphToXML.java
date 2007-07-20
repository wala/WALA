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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.ecore.java.ECallSite;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.EJavaMethod;
import com.ibm.wala.ecore.java.callGraph.ECallGraph;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.emf.wrappers.EMFBridge;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.EObjectDictionary;
import com.ibm.wala.emf.wrappers.EObjectGraph;
import com.ibm.wala.emf.wrappers.EObjectGraphImpl;
import com.ibm.wala.emf.wrappers.EUtil;
import com.ibm.wala.emf.wrappers.JavaScopeUtil;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WalaException;

/**
 * This simple example application builds a call graph writes it to an XML file
 * 
 * @author sfink
 */
public class ExportCallGraphToXML {

  /**
   * Usage: ExportCallGraphToXML -appJar [jar file name] The "jar file name"
   * should be something like "c:/temp/testdata/java_cup.jar"
   * 
   * @param args
   */
  public static void main(String[] args) {
    run(args);
  }

  /**
   * Usage: args = "-appJar [jar file name] " The "jar file name" should be
   * something like "c:/temp/testdata/java_cup.jar"
   * 
   * @param args
   */
  public static void run(String[] args) {
    validateCommandLine(args);
    run(args[1]);
  }

  /**
   * @param appJar
   *          something like "c:/temp/testdata/java_cup.jar"
   */
  public static void run(String appJar) {
    try {

      EJavaAnalysisScope escope = JavaScopeUtil.makeAnalysisScope(appJar);

      EMFScopeWrapper scope = EMFScopeWrapper.generateScope(escope);
      System.err.println("Build class hierarchy...");
      ClassHierarchy cha = ClassHierarchy.make(scope);

      Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
      AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

      // //
      // build the call graph
      // //
      System.err.println("Build callgraph...");
      com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(),cha, scope, null, null);
      CallGraph cg = builder.makeCallGraph(options);

      System.err.println("Convert to EMF...");
      com.ibm.wala.emf.wrappers.ECallGraphWrapper ccg = EMFBridge.makeCallGraph(cg);
      ECallGraph ecg = (ECallGraph) ccg.export();

      Properties p = null;
      ;
      try {
        p = WalaProperties.loadProperties();
      } catch (WalaException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      String filename = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + "cg.xml";
      System.err.println("Writing to file: " + filename);
      write(ecg, filename);

      // try and load it too

      List<EObject> l = EUtil.readEObjects(filename, ExportCallGraphToXML.class.getClassLoader());

      // convert it to a default graph for kicks
      ECallGraph output = (ECallGraph) l.get(0);
      System.err.println(output.getClass());
      EObjectGraph graph = EObjectGraphImpl.fromEMF(output);
      System.err.println("read " + graph.getNumberOfNodes() + " nodes");

    } catch (WalaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  private static void write(ECallGraph cg, String filename) throws WalaException {

    // make sure every java method reachable from the call graph
    // is a node is the call graph
    Collection c = Iterator2Collection.toCollection(cg.getNodes().getContents().iterator());
    for (Iterator it = c.iterator(); it.hasNext();) {
      Object node = it.next();
      if (node instanceof ECallSite) {
        ECallSite site = (ECallSite) node;
        EJavaMethod method = site.getDeclaredTarget();
        if (!c.contains(method)) {
          cg.getNodes().getContents().add(method);
        }

        method = site.getJavaMethod();
        if (!c.contains(method)) {
          cg.getNodes().getContents().add(method);
        }
      }
    }

    // make a canonical dictionary of all method nodes
    EObjectDictionary methodNodes = new EObjectDictionary();
    for (Iterator<EJavaMethod> it = cg.getNodes().getContents().iterator(); it.hasNext();) {
      Object node = it.next();
      if (node instanceof EJavaMethod) {
        EJavaMethod method = (EJavaMethod) node;
        methodNodes.findOrAdd(method);
      }
    }

    // Create a collection of EObjects to persist
    Collection persist = new LinkedList();
    // Persist the call graph itself
    persist.add(cg);
    // Persist the nodes of the call graph which are NOT contained in the
    // ECallGraph
    persist.add(cg.getNodes());

    // Persist all Java classes reachable from the call graph,
    // and make sure pointers are canonical
    EObjectDictionary klasses = new EObjectDictionary();
    for (Iterator<EJavaMethod> it = cg.getNodes().getContents().iterator(); it.hasNext();) {
      Object node = it.next();
      if (node instanceof ECallSite) {
        ECallSite site = (ECallSite) node;
        EJavaMethod method = (EJavaMethod) site.getDeclaredTarget();
        method = (EJavaMethod) methodNodes.findOrAdd(method);
        site.setDeclaredTarget(method);
        EJavaClass klass = (EJavaClass) klasses.findOrAdd(method.getJavaClass());
        method.setJavaClass(klass);

        method = site.getJavaMethod();
        method = (EJavaMethod) methodNodes.findOrAdd(method);
        site.setJavaMethod(method);
        klass = (EJavaClass) klasses.findOrAdd(method.getJavaClass());
        method.setJavaClass(klass);
      } else if (node instanceof EJavaMethod) {
        EJavaMethod method = (EJavaMethod) node;
        EJavaClass klass = (EJavaClass) klasses.findOrAdd(method.getJavaClass());
        method.setJavaClass(klass);
      } else {
        Assertions.UNREACHABLE("Unexpected type " + node.getClass());
      }
    }
    persist.add(klasses.export(true));

    // Save everything to a file.
    EUtil.saveToFile(persist, filename);
  }

  /**
   * Validate that the command-line arguments obey the expected usage.
   * 
   * Usage:
   * <ul>
   * <li> args[0] : "-appJar"
   * <li> args[1] : something like "c:/temp/testdata/java_cup.jar" </ul?
   * 
   * @param args
   * @throws UnsupportedOperationException
   *           if command-line is malformed.
   */
  static void validateCommandLine(String[] args) {
    if (args.length != 2) {
      throw new UnsupportedOperationException("must have at exactly 2 command-line arguments");
    }
    if (!args[0].equals("-appJar")) {
      throw new UnsupportedOperationException("invalid command-line, args[0] should be -appJar, but is " + args[0]);
    }
  }
}
