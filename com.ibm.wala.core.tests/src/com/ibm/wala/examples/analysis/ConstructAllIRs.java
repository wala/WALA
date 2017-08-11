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
package com.ibm.wala.examples.analysis;

import java.io.IOException;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.perf.Stopwatch;
import com.ibm.wala.util.ref.ReferenceCleanser;

/**
 * An analysis skeleton that simply constructs IRs for all methods in a class hierarchy. Illustrates the use of
 * {@link ReferenceCleanser} to improve running time / reduce memory usage.
 */
public class ConstructAllIRs {

  /**
   * Should we periodically clear out soft reference caches in an attempt to help the GC?
   */
  private final static boolean PERIODIC_WIPE_SOFT_CACHES = true;

  /**
   * Interval which defines the period to clear soft reference caches
   */
  private final static int WIPE_SOFT_CACHE_INTERVAL = 2500;

  /**
   * Counter for wiping soft caches
   */
  private static int wipeCount = 0;

  /**
   * First command-line argument should be location of scope file for application to analyze
   * 
   * @throws IOException
   * @throws ClassHierarchyException
   */
  public static void main(String[] args) throws IOException, ClassHierarchyException {
    String scopeFile = args[0];

    // measure running time
    Stopwatch s = new Stopwatch();
    s.start();
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, null, ConstructAllIRs.class.getClassLoader());

    // build a type hierarchy
    System.out.print("building class hierarchy...");
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    System.out.println("done");

    // register class hierarchy and AnalysisCache with the reference cleanser, so that their soft references are appropriately wiped
    ReferenceCleanser.registerClassHierarchy(cha);
    AnalysisOptions options = new AnalysisOptions();
    IAnalysisCacheView cache = new AnalysisCacheImpl(options.getSSAOptions());
    ReferenceCleanser.registerCache(cache);

    System.out.print("building IRs...");
    for (IClass klass : cha) {
      for (IMethod method : klass.getDeclaredMethods()) {
        wipeSoftCaches();
        // construct an IR; it will be cached
        cache.getIR(method, Everywhere.EVERYWHERE);
      }
    }
    System.out.println("done");
    s.stop();
    System.out.println("RUNNING TIME: " + s.getElapsedMillis());

  }

  private static void wipeSoftCaches() {
    if (PERIODIC_WIPE_SOFT_CACHES) {
      wipeCount++;
      if (wipeCount >= WIPE_SOFT_CACHE_INTERVAL) {
        wipeCount = 0;
        ReferenceCleanser.clearSoftCaches();
      }
    }
  }

}
