/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.client;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.callgraph.fieldbased.FieldBasedCallGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.FieldBasedCallGraphBuilder.CallGraphResult;
import com.ibm.wala.cast.js.callgraph.fieldbased.OptimisticCallgraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.PessimisticCallGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.ObjectVertex;
import com.ibm.wala.cast.js.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSZeroOrOneXCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptEntryPoints;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.cha.SeqClassHierarchyFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import java.util.Collections;
import java.util.Set;
import java.util.jar.JarFile;

public abstract class JavaScriptAnalysisEngine<I extends InstanceKey>
    extends AbstractAnalysisEngine<I, CallGraphBuilder<I>, Void> {
  protected JavaScriptLoaderFactory loaderFactory;

  protected JavaScriptTranslatorFactory translatorFactory;

  @Override
  public void buildAnalysisScope() {
    loaderFactory = new JavaScriptLoaderFactory(translatorFactory);

    SourceModule[] files = moduleFiles.toArray(new SourceModule[0]);

    scope = new CAstAnalysisScope(files, loaderFactory, Collections.singleton(JavaScriptLoader.JS));
  }

  @Override
  public IClassHierarchy buildClassHierarchy() {
    try {
      return setClassHierarchy(
          SeqClassHierarchyFactory.make(getScope(), loaderFactory, JavaScriptLoader.JS));
    } catch (ClassHierarchyException e) {
      Assertions.UNREACHABLE(e.toString());
      return null;
    }
  }

  public void setTranslatorFactory(JavaScriptTranslatorFactory factory) {
    this.translatorFactory = factory;
  }

  @Override
  public void setJ2SELibraries(JarFile[] libs) {
    Assertions.UNREACHABLE("Illegal to call setJ2SELibraries");
  }

  @Override
  public void setJ2SELibraries(Module[] libs) {
    Assertions.UNREACHABLE("Illegal to call setJ2SELibraries");
  }

  @Override
  protected Iterable<Entrypoint> makeDefaultEntrypoints(IClassHierarchy cha) {
    return new JavaScriptEntryPoints(cha, cha.getLoader(JavaScriptTypes.jsLoader));
  }

  @Override
  public IAnalysisCacheView makeDefaultCache() {
    return new AnalysisCacheImpl(AstIRFactory.makeDefaultFactory());
  }

  @Override
  public JSAnalysisOptions getDefaultOptions(Iterable<Entrypoint> roots) {
    final JSAnalysisOptions options = new JSAnalysisOptions(scope, roots);

    options.setUseConstantSpecificKeys(true);

    options.setUseStacksForLexicalScoping(true);

    return options;
  }

  public static class FieldBasedJavaScriptAnalysisEngine
      extends JavaScriptAnalysisEngine<ObjectVertex> {
    public enum BuilderType {
      PESSIMISTIC,
      OPTIMISTIC,
      REFLECTIVE
    }

    private BuilderType builderType = BuilderType.OPTIMISTIC;

    /** @return the builderType */
    public BuilderType getBuilderType() {
      return builderType;
    }

    /** @param builderType the builderType to set */
    public void setBuilderType(BuilderType builderType) {
      this.builderType = builderType;
    }

    @Override
    public JSAnalysisOptions getDefaultOptions(Iterable<Entrypoint> roots) {
      return JSCallGraphUtil.makeOptions(scope, getClassHierarchy(), roots);
    }

    @Override
    protected CallGraphBuilder<ObjectVertex> getCallGraphBuilder(
        final IClassHierarchy cha, AnalysisOptions options, final IAnalysisCacheView cache) {
      Set<Entrypoint> roots = HashSetFactory.make();
      for (Entrypoint e : options.getEntrypoints()) {
        roots.add(e);
      }

      if (builderType.equals(BuilderType.OPTIMISTIC)) {
        ((JSAnalysisOptions) options).setHandleCallApply(false);
      }

      final FieldBasedCallGraphBuilder builder =
          builderType.equals(BuilderType.PESSIMISTIC)
              ? new PessimisticCallGraphBuilder(
                  getClassHierarchy(), options, makeDefaultCache(), true)
              : new OptimisticCallgraphBuilder(
                  getClassHierarchy(), options, makeDefaultCache(), true);

      return new CallGraphBuilder<ObjectVertex>() {
        private PointerAnalysis<ObjectVertex> ptr;

        @Override
        public CallGraph makeCallGraph(AnalysisOptions options, IProgressMonitor monitor)
            throws IllegalArgumentException, CallGraphBuilderCancelException {
          CallGraphResult result;
          try {
            result = builder.buildCallGraph(options.getEntrypoints(), monitor);
          } catch (CancelException e) {
            throw CallGraphBuilderCancelException.createCallGraphBuilderCancelException(
                e, null, null);
          }
          ptr = result.getPointerAnalysis();
          return result.getCallGraph();
        }

        @Override
        public PointerAnalysis<ObjectVertex> getPointerAnalysis() {
          return ptr;
        }

        @Override
        public IAnalysisCacheView getAnalysisCache() {
          return cache;
        }

        @Override
        public IClassHierarchy getClassHierarchy() {
          return cha;
        }
      };
    }
  }

  public static class PropagationJavaScriptAnalysisEngine
      extends JavaScriptAnalysisEngine<InstanceKey> {

    @Override
    protected JSZeroOrOneXCFABuilder getCallGraphBuilder(
        IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
      return new ZeroCFABuilderFactory().make((JSAnalysisOptions) options, cache, cha);
    }
  }
}
