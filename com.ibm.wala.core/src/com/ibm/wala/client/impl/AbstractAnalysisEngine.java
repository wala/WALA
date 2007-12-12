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
package com.ibm.wala.client.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.jar.JarFile;

import com.ibm.wala.analysis.pointers.BasicHeapGraph;
import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.client.AnalysisEngine;
import com.ibm.wala.client.CallGraphBuilderFactory;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerFlowGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerFlowGraphFactory;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.*;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * Abstract base class for analysis engine implementations
 * 
 * Some clients choose to build on this, but many don't. I usually don't in new
 * code; I usually don't find the re-use enabled by this class compelling. I
 * would probably nuke this except for some legacy code that uses it.
 * 
 * @author sfink
 */
public abstract class AbstractAnalysisEngine implements AnalysisEngine {

  public interface EntrypointBuilder {
    Iterable<Entrypoint> createEntrypoints(AnalysisScope scope, IClassHierarchy cha);
  }

  public final static String BASIC_FILE = "SyntheticJ2SEModel.xml";

  /**
   * DEBUG_LEVEL:
   * <ul>
   * <li>0 No output
   * <li>1 Print some simple stats and warning information
   * <li>2 Detailed debugging
   * </ul>
   */
  protected static final int DEBUG_LEVEL = 1;

  /**
   * Name of the file which holds the class hierarchy exclusions directives for
   * this analysis.
   */
  private String exclusionsFile = "J2SEClassHierarchyExclusions.xml";

  /**
   * The modules to analyze
   */
  protected Collection moduleFiles;

  /**
   * A representation of the analysis scope
   */
  protected AnalysisScope scope;

  /**
   * A representation of the analysis options
   */
  private AnalysisOptions options;
  
  /**
   * A cache of IRs and stuff
   */
  private AnalysisCache cache = makeDefaultCache();

  /**
   * The standard J2SE libraries to analyze
   */
  protected Module[] j2seLibs;

  /**
   * Whether to perform closed-world analysis of an application
   */
  private boolean closedWorld = false;

  /**
   * An object which produces call graph builders specialized for J2EE
   */
  private CallGraphBuilderFactory callGraphBuilderFactory;

  /**
   * Governing class hierarchy
   */
  private IClassHierarchy cha;

  /**
   * Governing call graph
   */
  protected CallGraph cg;

  /**
   * Results of pointer analysis
   */
  protected PointerAnalysis pointerAnalysis;

  /**
   * Factory to create graph view of flow of pointers in the heap.
   */
  private PointerFlowGraphFactory pointerFlowGraphFactory;

  /**
   * Graph view of flow of pointers between heap abstracts
   */
  private PointerFlowGraph pointerFlowGraph;

  /**
   * Graph view of flow of pointers between heap abstractions
   */
  private HeapGraph heapGraph;

  private EntrypointBuilder entrypointBuilder = new EntrypointBuilder() {
    public Iterable<Entrypoint> createEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
      return makeDefaultEntrypoints(scope, cha);
    }
  };

  protected CallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
    return getCallGraphBuilderFactory().make(options, cache, cha, getScope(),  false);
  }

  protected CallGraphBuilder buildCallGraph(IClassHierarchy cha, AnalysisOptions options, boolean savePointerAnalysis) throws IllegalArgumentException, CancelException {
    Assertions.productionAssertion(getCallGraphBuilderFactory() != null, "must initialize callGraphBuilderFactory!");

    CallGraphBuilder builder = getCallGraphBuilder(cha, options, cache);

    cg = builder.makeCallGraph(options);

    if (savePointerAnalysis) {
      pointerFlowGraphFactory = builder.getPointerFlowGraphFactory();
      pointerAnalysis = builder.getPointerAnalysis();
    }

    return builder;
  }

  public void setModuleFiles(Collection moduleFiles) {
    this.moduleFiles = moduleFiles;
  }

  /**
   * Set up the AnalysisScope object
   */
  protected void buildAnalysisScope() {
    if (j2seLibs == null) {
      Assertions.UNREACHABLE("no j2selibs specified. You probably did not call AppAnalysisEngine.setJ2SELibrary.");
    }

    scope = AnalysisScopeReader.read(BASIC_FILE, getExclusionsFile(), getClass().getClassLoader());

    // add standard libraries
    for (int i = 0; i < j2seLibs.length; i++) {
      scope.addToScope(scope.getPrimordialLoader(), j2seLibs[i]);
    }

    // add user stuff
    addApplicationModulesToScope();
  }

  /**
   * @return a IClassHierarchy object for this analysis scope
   */
  public IClassHierarchy buildClassHierarchy() {
    IClassHierarchy cha = null;
    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(getScope().getExclusions() );
    try {
      cha = ClassHierarchy.make(getScope(), factory);
    } catch (ClassHierarchyException e) {
      System.err.println("Class Hierarchy construction failed");
      System.err.println(e.toString());
      e.printStackTrace();
    }
    return cha;
  }

  public IClassHierarchy getClassHierarchy() {
    return cha;
  }

  protected void setClassHierarchy(IClassHierarchy cha) {
    this.cha = cha;
  }

  /**
   * @return Returns the call graph
   */
  protected CallGraph getCallGraph() {
    return cg;
  }

  /**
   * Add the application modules to the analysis scope.
   */
  protected void addApplicationModulesToScope() {
    ClassLoaderReference app = scope.getApplicationLoader();
    for (Iterator it = moduleFiles.iterator(); it.hasNext();) {
      Object o = it.next();
      if (Assertions.verifyAssertions) {
        if (!(o instanceof Module)) {
          Assertions.UNREACHABLE("Unexpected type: " + o.getClass());
        }
      }
      Module M = (Module) o;
      scope.addToScope(app, M);
    }
  }

  public void setJ2SELibraries(JarFile[] libs) {
    if (libs == null) {
      throw new IllegalArgumentException("libs is null");
    }
    this.j2seLibs = new Module[libs.length];
    for (int i = 0; i < libs.length; i++) {
      j2seLibs[i] = new JarFileModule(libs[i]);
    }
  }

  public void setJ2SELibraries(Module[] libs) {
    if (libs == null) {
      throw new IllegalArgumentException("libs is null");
    }
    this.j2seLibs = new Module[libs.length];
    for (int i = 0; i < libs.length; i++) {
      j2seLibs[i] = libs[i];
    }
  }

  public void setClosedWorld(boolean b) {
    this.closedWorld = b;
  }

  public boolean isClosedWorld() {
    return closedWorld;
  }

  protected AnalysisScope getScope() {
    return scope;
  }

  protected CallGraphBuilderFactory getCallGraphBuilderFactory() {
    return callGraphBuilderFactory;
  }

  protected void setCallGraphBuilderFactory(CallGraphBuilderFactory callGraphBuilderFactory) {
    this.callGraphBuilderFactory = callGraphBuilderFactory;
  }

  public PointerAnalysis getPointerAnalysis() {
    return pointerAnalysis;
  }

  public PointerFlowGraph getPointerFlowGraph() {
    if (pointerFlowGraph == null) {
      pointerFlowGraph = pointerFlowGraphFactory.make(pointerAnalysis, cg);
    }
    return pointerFlowGraph;
  }

  public HeapGraph getHeapGraph() {
    if (heapGraph == null) {
      heapGraph = new BasicHeapGraph(getPointerAnalysis(), cg);
    }
    return heapGraph;
  }

  public String getExclusionsFile() {
    return exclusionsFile;
  }

  public void setExclusionsFile(String exclusionsFile) {
    this.exclusionsFile = exclusionsFile;
  }

  public AnalysisOptions getDefaultOptions(Iterable<Entrypoint> entrypoints) {
    return new AnalysisOptions(getScope(), entrypoints);
  }
  
  public AnalysisCache makeDefaultCache() {
    return new AnalysisCache(new DefaultIRFactory());
  }

  protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
    return Util.makeMainEntrypoints(scope, cha);
  }

  public void setEntrypointBuilder(EntrypointBuilder builder) {
    entrypointBuilder = builder;
  }

  /**
   * Builds the call graph for the analysis scope in effect, using all of the
   * given entry points.
   * @throws CancelException 
   * @throws IllegalArgumentException 
   */
  public CallGraphBuilder defaultCallGraphBuilder() throws IllegalArgumentException, CancelException {
    buildAnalysisScope();
    IClassHierarchy cha = buildClassHierarchy();
    setClassHierarchy(cha);
    Iterable<Entrypoint> eps = entrypointBuilder.createEntrypoints(scope, cha);
    options = getDefaultOptions(eps);
    cache = makeDefaultCache();
    return buildCallGraph(cha, options, true);
  }

  public CallGraph buildDefaultCallGraph() throws IllegalArgumentException, CancelException {
    return defaultCallGraphBuilder().makeCallGraph(options);
  }

  public AnalysisCache getCache() {
    return cache;
  }

  public AnalysisOptions getOptions() {
    return options;
  }

}
