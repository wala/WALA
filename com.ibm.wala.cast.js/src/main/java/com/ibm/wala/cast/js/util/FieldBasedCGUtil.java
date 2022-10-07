/*
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.util;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.callgraph.fieldbased.FieldBasedCallGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.FieldBasedCallGraphBuilder.CallGraphResult;
import com.ibm.wala.cast.js.callgraph.fieldbased.OptimisticCallgraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.PessimisticCallGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.WorklistBasedOptimisticCallgraphBuilder;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.WalaException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utility class for building call graphs.
 *
 * @author mschaefer
 */
public class FieldBasedCGUtil {
  public static enum BuilderType {
    PESSIMISTIC {
      @Override
      protected FieldBasedCallGraphBuilder fieldBasedCallGraphBuilderFactory(
          IClassHierarchy cha,
          final JSAnalysisOptions makeOptions,
          IAnalysisCacheView cache,
          boolean supportFullPointerAnalysis) {
        return new PessimisticCallGraphBuilder(cha, makeOptions, cache, supportFullPointerAnalysis);
      }
    },

    OPTIMISTIC {
      @Override
      protected FieldBasedCallGraphBuilder fieldBasedCallGraphBuilderFactory(
          IClassHierarchy cha,
          JSAnalysisOptions makeOptions,
          IAnalysisCacheView cache,
          boolean supportFullPointerAnalysis) {
        return new OptimisticCallgraphBuilder(cha, makeOptions, cache, supportFullPointerAnalysis);
      }
    },

    OPTIMISTIC_WORKLIST {
      @Override
      protected FieldBasedCallGraphBuilder fieldBasedCallGraphBuilderFactory(
          IClassHierarchy cha,
          JSAnalysisOptions makeOptions,
          IAnalysisCacheView cache,
          boolean supportFullPointerAnalysis) {
        return new WorklistBasedOptimisticCallgraphBuilder(
            cha, makeOptions, cache, supportFullPointerAnalysis, -1);
      }
    };

    protected abstract FieldBasedCallGraphBuilder fieldBasedCallGraphBuilderFactory(
        IClassHierarchy cha,
        JSAnalysisOptions makeOptions,
        IAnalysisCacheView cache,
        boolean supportFullPointerAnalysis);
  }

  private final JavaScriptTranslatorFactory translatorFactory;

  public FieldBasedCGUtil(JavaScriptTranslatorFactory translatorFactory) {
    this.translatorFactory = translatorFactory;
  }

  public CallGraphResult buildCG(
      URL url,
      BuilderType builderType,
      boolean supportFullPointerAnalysis,
      Supplier<JSSourceExtractor> fExtractor)
      throws WalaException, CancelException {
    return buildCG(
        url, builderType, new NullProgressMonitor(), supportFullPointerAnalysis, fExtractor);
  }

  public CallGraphResult buildCG(
      URL url,
      BuilderType builderType,
      IProgressMonitor monitor,
      boolean supportFullPointerAnalysis,
      Supplier<JSSourceExtractor> fExtractor)
      throws WalaException, CancelException {
    if (url.getFile().endsWith(".js")) {
      return buildScriptCG(url, builderType, monitor, supportFullPointerAnalysis);
    } else {
      return buildPageCG(url, builderType, monitor, supportFullPointerAnalysis, fExtractor);
    }
  }

  public CallGraphResult buildScriptCG(
      URL url,
      BuilderType builderType,
      IProgressMonitor monitor,
      boolean supportFullPointerAnalysis)
      throws WalaException, CancelException {
    JavaScriptLoaderFactory loaders = new JavaScriptLoaderFactory(translatorFactory);
    Module[] scripts =
        new Module[] {new SourceURLModule(url), JSCallGraphUtil.getPrologueFile("prologue.js")};
    return buildCG(loaders, scripts, builderType, monitor, supportFullPointerAnalysis);
  }

  /**
   * Construct a field-based call graph using all the {@code .js} files appearing in scriptDir or
   * any of its sub-directories
   */
  public CallGraphResult buildScriptDirCG(
      Path scriptDir,
      BuilderType builderType,
      IProgressMonitor monitor,
      boolean supportFullPointerAnalysis)
      throws WalaException, CancelException, IOException {
    JavaScriptLoaderFactory loaders = new JavaScriptLoaderFactory(translatorFactory);
    List<Module> scripts = findScriptsInDir(scriptDir);
    return buildCG(
        loaders, scripts.toArray(new Module[0]), builderType, monitor, supportFullPointerAnalysis);
  }

  /**
   * Construct a bounded field-based call graph using all the {@code .js} files appearing in
   * scriptDir or any of its sub-directories
   */
  public CallGraphResult buildScriptDirBoundedCG(
      Path scriptDir, IProgressMonitor monitor, boolean supportFullPointerAnalysis, Integer bound)
      throws WalaException, CancelException, IOException {
    JavaScriptLoaderFactory loaders = new JavaScriptLoaderFactory(translatorFactory);
    List<Module> scripts = findScriptsInDir(scriptDir);
    return buildBoundedCG(
        loaders, scripts.toArray(new Module[0]), monitor, supportFullPointerAnalysis, bound);
  }

  public List<Module> findScriptsInDir(Path scriptDir) throws IOException {
    List<Path> jsFiles =
        Files.walk(scriptDir)
            .filter(p -> p.toString().toLowerCase().endsWith(".js"))
            .collect(Collectors.toList());
    List<Module> scripts = new ArrayList<>();
    // we can't do this loop as a map() operation on the previous stream because toURL() throws
    // a checked exception
    for (Path p : jsFiles) {
      scripts.add(new SourceURLModule(p.toUri().toURL()));
    }
    scripts.add(JSCallGraphUtil.getPrologueFile("prologue.js"));
    return scripts;
  }

  public CallGraphResult buildTestCG(
      String dir,
      String name,
      BuilderType builderType,
      IProgressMonitor monitor,
      boolean supportFullPointerAnalysis)
      throws IOException, WalaException, CancelException {
    JavaScriptLoaderFactory loaders = new JavaScriptLoaderFactory(translatorFactory);
    Module[] scripts = JSCallGraphBuilderUtil.makeSourceModules(dir, name);
    return buildCG(loaders, scripts, builderType, monitor, supportFullPointerAnalysis);
  }

  public CallGraphResult buildPageCG(
      URL url,
      BuilderType builderType,
      IProgressMonitor monitor,
      boolean supportFullPointerAnalysis,
      Supplier<JSSourceExtractor> fExtractor)
      throws WalaException, CancelException {
    JavaScriptLoaderFactory loaders = new WebPageLoaderFactory(translatorFactory);
    SourceModule[] scripts = JSCallGraphBuilderUtil.makeHtmlScope(url, loaders, fExtractor);
    return buildCG(loaders, scripts, builderType, monitor, supportFullPointerAnalysis);
  }

  public CallGraphResult buildCG(
      JavaScriptLoaderFactory loaders,
      Module[] scripts,
      BuilderType builderType,
      IProgressMonitor monitor,
      boolean supportFullPointerAnalysis)
      throws WalaException, CancelException {
    CAstAnalysisScope scope =
        new CAstAnalysisScope(scripts, loaders, Collections.singleton(JavaScriptLoader.JS));
    IClassHierarchy cha = ClassHierarchyFactory.make(scope, loaders, JavaScriptLoader.JS);
    com.ibm.wala.cast.util.Util.checkForFrontEndErrors(cha);
    Iterable<Entrypoint> roots = JSCallGraphUtil.makeScriptRoots(cha);
    IAnalysisCacheView cache = new AnalysisCacheImpl(AstIRFactory.makeDefaultFactory());
    final FieldBasedCallGraphBuilder builder =
        builderType.fieldBasedCallGraphBuilderFactory(
            cha, JSCallGraphUtil.makeOptions(scope, cha, roots), cache, supportFullPointerAnalysis);
    return builder.buildCallGraph(roots, monitor);
  }

  public CallGraphResult buildBoundedCG(
      JavaScriptLoaderFactory loaders,
      Module[] scripts,
      IProgressMonitor monitor,
      boolean supportFullPointerAnalysis,
      Integer bound)
      throws WalaException, CancelException {
    CAstAnalysisScope scope =
        new CAstAnalysisScope(scripts, loaders, Collections.singleton(JavaScriptLoader.JS));
    IClassHierarchy cha = ClassHierarchyFactory.make(scope, loaders, JavaScriptLoader.JS);
    com.ibm.wala.cast.util.Util.checkForFrontEndErrors(cha);
    Iterable<Entrypoint> roots = JSCallGraphUtil.makeScriptRoots(cha);
    IAnalysisCacheView cache = new AnalysisCacheImpl(AstIRFactory.makeDefaultFactory());
    final FieldBasedCallGraphBuilder builder =
        new WorklistBasedOptimisticCallgraphBuilder(
            cha,
            JSCallGraphUtil.makeOptions(scope, cha, roots),
            cache,
            supportFullPointerAnalysis,
            bound);
    return builder.buildCallGraph(roots, monitor);
  }

  /*
  private JavaScriptLoaderFactory makeLoaderFactory(URL url) {
  	return url.getFile().endsWith(".js") ? new JavaScriptLoaderFactory(translatorFactory) : new WebPageLoaderFactory(translatorFactory);
  }
  */

  @SuppressWarnings("unused")
  private static void compareCGs(Map<String, Set<String>> cg1, Map<String, Set<String>> cg2) {
    boolean diff = false;
    for (Map.Entry<String, Set<String>> entry : cg1.entrySet()) {
      final String key = entry.getKey();
      Set<String> targets1 = entry.getValue(), targets2 = cg2.get(key);
      if (targets2 == null) {
        diff = true;
        System.err.println("CG2 doesn't have call site" + key);
      } else {
        for (String target : targets1)
          if (!targets2.contains(target)) {
            diff = true;
            System.err.println("CG2 doesn't have edge " + key + " -> " + target);
          }
        for (String target : targets2)
          if (!targets1.contains(target)) {
            diff = true;
            System.err.println("CG1 doesn't have edge " + key + " -> " + target);
          }
      }
    }
    for (String key : cg2.keySet()) {
      if (!cg1.containsKey(key)) {
        diff = true;
        System.err.println("CG1 doesn't have call site " + key);
      }
    }
    if (!diff) System.err.println("call graphs are identical");
  }
}
