/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.test;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Set;

import junit.framework.Assert;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSZeroOrOneXCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.PropertyNameContextSelector;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction.CorrelatedPairExtractorFactory;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.loader.CAstAbstractLoader;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

/**
 * TODO this class is a mess. rewrite.
 */
public class JSCallGraphBuilderUtil extends com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil {

  public static enum CGBuilderType {
    ZERO_ONE_CFA(false, false, true, true),
    ZERO_ONE_CFA_NO_CALL_APPLY(false, false, false, true),
    ZERO_ONE_CFA_PRECISE_LEXICAL(false, true, true, true),
    ONE_CFA(true, false, true, true),
    ONE_CFA_PRECISE_LEXICAL(true, true, true, true);

    private final boolean useOneCFA;

    private final boolean usePreciseLexical;

    private final boolean handleCallApply;
    
    private final boolean extractCorrelatedPairs;

    private CGBuilderType(boolean useOneCFA, boolean usePreciseLexical, boolean handleCallApply, boolean extractCorrelatedPairs) {
      this.useOneCFA = useOneCFA;
      this.usePreciseLexical = usePreciseLexical;
      this.handleCallApply = handleCallApply;
      this.extractCorrelatedPairs = extractCorrelatedPairs;
    }

    public boolean useOneCFA() {
      return useOneCFA;
    }

    public boolean usePreciseLexical() {
      return usePreciseLexical;
    }
    
    public boolean handleCallApply() {
      return handleCallApply;
    }

    public boolean extractCorrelatedPairs() {
      return extractCorrelatedPairs;
    }
  }

  public static JSCFABuilder makeScriptCGBuilder(String dir, String name, CGBuilderType builderType) throws IOException, WalaException {
    URL script = getURLforFile(dir, name);
    CAstRewriterFactory preprocessor = builderType.extractCorrelatedPairs ? new CorrelatedPairExtractorFactory(translatorFactory, script) : null;
    JavaScriptLoaderFactory loaders = JSCallGraphUtil.makeLoaders(preprocessor);

    AnalysisScope scope = makeScriptScope(script, dir, name, loaders);

    return makeCG(loaders, scope, builderType, AstIRFactory.makeDefaultFactory());
  }

  private static URL getURLforFile(String dir, String name) {
    URL script = JSCallGraphBuilderUtil.class.getClassLoader().getResource(dir + File.separator + name);
    if (script == null) {
      script = JSCallGraphBuilderUtil.class.getClassLoader().getResource(dir + "/" + name);
    }
    assert script != null : "cannot find " + dir + " and " + name;
    return script;
  }
  
  static AnalysisScope makeScriptScope(String dir, String name, JavaScriptLoaderFactory loaders) throws IOException {
    return makeScriptScope(getURLforFile(dir, name), dir, name, loaders);
  }

  static AnalysisScope makeScriptScope(URL script, String dir, String name, JavaScriptLoaderFactory loaders) throws IOException {
    AnalysisScope scope;
    if (script.openConnection() instanceof JarURLConnection) {
      scope = makeScope(new URL[] { script }, loaders, JavaScriptLoader.JS);
    } else {
      scope = makeScope(new SourceFileModule[] { makeSourceModule(script, dir, name) }, loaders, JavaScriptLoader.JS);
    }

    return scope;
  }

  public static JSCFABuilder makeScriptCGBuilder(String dir, String name) throws IOException, WalaException {
    return makeScriptCGBuilder(dir, name, CGBuilderType.ZERO_ONE_CFA);
  }

  public static CallGraph makeScriptCG(String dir, String name) throws IOException, IllegalArgumentException, CancelException, WalaException {
    return makeScriptCG(dir, name, CGBuilderType.ZERO_ONE_CFA);
  }

  public static CallGraph makeScriptCG(String dir, String name, CGBuilderType builderType) throws IOException,
      IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder b = makeScriptCGBuilder(dir, name, builderType);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    // dumpCG(b.getPointerAnalysis(), CG);
    return CG;
  }

  public static CallGraph makeScriptCG(SourceModule[] scripts, CGBuilderType builderType, IRFactory<IMethod> irFactory) throws IOException, IllegalArgumentException,
      CancelException, WalaException {
    CAstRewriterFactory preprocessor = builderType.extractCorrelatedPairs ? new CorrelatedPairExtractorFactory(translatorFactory, scripts) : null;
    PropagationCallGraphBuilder b = makeCGBuilder(makeLoaders(preprocessor), scripts, builderType, irFactory);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    // dumpCG(b.getPointerAnalysis(), CG);
    return CG;
  }

  public static JSCFABuilder makeHTMLCGBuilder(URL url) throws IOException, WalaException {
    return makeHTMLCGBuilder(url, CGBuilderType.ZERO_ONE_CFA);
  }

  public static JSCFABuilder makeHTMLCGBuilder(URL url, CGBuilderType builderType) throws IOException, WalaException {
    JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
    SourceModule[] scripts;
    IRFactory<IMethod> irFactory = AstIRFactory.makeDefaultFactory();
    CAstRewriterFactory preprocessor = builderType.extractCorrelatedPairs ? new CorrelatedPairExtractorFactory(translatorFactory, url) : null;
    JavaScriptLoaderFactory loaders = new WebPageLoaderFactory(translatorFactory, preprocessor);
    try {
      Set<MappedSourceModule> script = WebUtil.extractScriptFromHTML(url).fst;
      scripts = script.toArray(new SourceModule[script.size()]);
    } catch (Error e) {
      SourceModule dummy = new SourceURLModule(url);
      scripts = new SourceModule[]{ dummy };
      ((CAstAbstractLoader)loaders.getTheLoader()).addMessage(dummy, e.warning);
    }
    JSCFABuilder builder = makeCGBuilder(loaders, scripts, builderType, irFactory);
    if(builderType.extractCorrelatedPairs)
      builder.setContextSelector(new PropertyNameContextSelector(builder.getAnalysisCache(), 2, builder.getContextSelector()));
    builder.setBaseURL(url);
    return builder;
  }

  public static CallGraph makeHTMLCG(URL url) throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder b = makeHTMLCGBuilder(url);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    dumpCG(b.getPointerAnalysis(), CG);
    return CG;
  }

  public static CallGraph makeHTMLCG(URL url, CGBuilderType builderType) throws IOException, IllegalArgumentException,
      CancelException, WalaException {
    PropagationCallGraphBuilder b = makeHTMLCGBuilder(url, builderType);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    return CG;
  }

  public static JSCFABuilder makeCGBuilder(JavaScriptLoaderFactory loaders, SourceModule[] scripts, CGBuilderType builderType, IRFactory<IMethod> irFactory) throws IOException, WalaException {
    AnalysisScope scope = makeScope(scripts, loaders, JavaScriptLoader.JS);
    return makeCG(loaders, scope, builderType, irFactory);
  }

  protected static JSCFABuilder makeCG(JavaScriptLoaderFactory loaders, AnalysisScope scope, CGBuilderType builderType, IRFactory<IMethod> irFactory) throws IOException, WalaException {
    try {
      IClassHierarchy cha = makeHierarchy(scope, loaders);
      com.ibm.wala.cast.js.util.Util.checkForFrontEndErrors(cha);
      Iterable<Entrypoint> roots = makeScriptRoots(cha);
      JSAnalysisOptions options = makeOptions(scope, cha, roots);
      options.setHandleCallApply(builderType.handleCallApply());
      options.setUsePreciseLexical(builderType.usePreciseLexical());
      AnalysisCache cache = makeCache(irFactory);
      JSCFABuilder builder = new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, ZeroXInstanceKeys.ALLOCATIONS,
          builderType.useOneCFA());
      if(builderType.extractCorrelatedPairs())
        builder.setContextSelector(new PropertyNameContextSelector(builder.getAnalysisCache(), 2, builder.getContextSelector()));

      return builder;
    } catch (ClassHierarchyException e) {
      Assert.assertTrue("internal error building class hierarchy", false);
      return null;
    }
  }
}
