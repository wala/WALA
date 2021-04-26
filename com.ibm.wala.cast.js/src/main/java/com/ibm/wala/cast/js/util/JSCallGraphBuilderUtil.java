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
package com.ibm.wala.cast.js.util;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
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
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.function.Supplier;

/** TODO this class is a mess. rewrite. */
public class JSCallGraphBuilderUtil extends com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil {

  public static enum CGBuilderType {
    ZERO_ONE_CFA(false, true, true),
    ZERO_ONE_CFA_WITHOUT_CORRELATION_TRACKING(false, true, false),
    ZERO_ONE_CFA_NO_CALL_APPLY(false, false, true),
    ONE_CFA(true, true, true);

    private final boolean useOneCFA;

    private final boolean handleCallApply;

    private final boolean extractCorrelatedPairs;

    private CGBuilderType(
        boolean useOneCFA, boolean handleCallApply, boolean extractCorrelatedPairs) {
      this.useOneCFA = useOneCFA;
      this.handleCallApply = handleCallApply;
      this.extractCorrelatedPairs = extractCorrelatedPairs;
    }

    public boolean useOneCFA() {
      return useOneCFA;
    }

    public boolean handleCallApply() {
      return handleCallApply;
    }

    public boolean extractCorrelatedPairs() {
      return extractCorrelatedPairs;
    }
  }

  /**
   * create a CG builder for script. Note that the script at dir/name is loaded via the classloader,
   * not from the filesystem.
   */
  public static JSCFABuilder makeScriptCGBuilder(
      String dir, String name, CGBuilderType builderType, ClassLoader loader)
      throws IOException, WalaException {
    URL script = getURLforFile(dir, name, loader);
    CAstRewriterFactory<?, ?> preprocessor =
        builderType.extractCorrelatedPairs
            ? new CorrelatedPairExtractorFactory(translatorFactory, script)
            : null;
    JavaScriptLoaderFactory loaders = JSCallGraphUtil.makeLoaders(preprocessor);

    AnalysisScope scope = makeScriptScope(dir, name, loaders, loader);

    return makeCG(loaders, scope, builderType, AstIRFactory.makeDefaultFactory());
  }

  public static URL getURLforFile(String dir, String name, ClassLoader loader) throws IOException {
    File f = null;
    FileProvider provider = new FileProvider();
    if (dir.startsWith(File.separator)) {
      f = new File(dir + File.separator + name);
    } else {
      try {
        f = provider.getFile(dir + File.separator + name, loader);
      } catch (FileNotFoundException e) {
        // I guess we need to do this on Windows sometimes?  --MS
        // if this fails, we won't catch the exception
      }
    }
    return f.toURI().toURL();
  }

  public static AnalysisScope makeScriptScope(
      String dir, String name, JavaScriptLoaderFactory loaders, ClassLoader loader)
      throws IOException {
    return makeScope(makeSourceModules(dir, name, loader), loaders, JavaScriptLoader.JS);
  }

  public static AnalysisScope makeScriptScope(
      String dir, String name, JavaScriptLoaderFactory loaders) throws IOException {
    return makeScope(
        makeSourceModules(dir, name, JSCallGraphBuilderUtil.class.getClassLoader()),
        loaders,
        JavaScriptLoader.JS);
  }

  public static Module[] makeSourceModules(String dir, String name) throws IOException {
    return makeSourceModules(dir, name, JSCallGraphBuilderUtil.class.getClassLoader());
  }

  public static Module[] makeSourceModules(String dir, String name, ClassLoader loader)
      throws IOException {
    URL script = getURLforFile(dir, name, loader);
    Module[] modules =
        new Module[] {
          (script.openConnection() instanceof JarURLConnection)
              ? new SourceURLModule(script)
              : makeSourceModule(script, dir, name),
          getPrologueFile("prologue.js")
        };
    return modules;
  }

  public static JSCFABuilder makeScriptCGBuilder(String dir, String name, ClassLoader loader)
      throws IOException, WalaException {
    return makeScriptCGBuilder(dir, name, CGBuilderType.ZERO_ONE_CFA, loader);
  }

  public static JSCFABuilder makeScriptCGBuilder(String dir, String name)
      throws IOException, WalaException {
    return makeScriptCGBuilder(
        dir, name, CGBuilderType.ZERO_ONE_CFA, JSCallGraphBuilderUtil.class.getClassLoader());
  }

  public static CallGraph makeScriptCG(String dir, String name)
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    return makeScriptCG(
        dir, name, CGBuilderType.ZERO_ONE_CFA, JSCallGraphBuilderUtil.class.getClassLoader());
  }

  public static CallGraph makeScriptCG(String dir, String name, ClassLoader loader)
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    return makeScriptCG(dir, name, CGBuilderType.ZERO_ONE_CFA, loader);
  }

  public static JSCFABuilder makeScriptCGBuilderWithoutCorrelationTracking(
      String dir, String name, ClassLoader loader) throws IOException, WalaException {
    return makeScriptCGBuilder(
        dir, name, CGBuilderType.ZERO_ONE_CFA_WITHOUT_CORRELATION_TRACKING, loader);
  }

  public static JSCFABuilder makeScriptCGBuilderWithoutCorrelationTracking(String dir, String name)
      throws IOException, WalaException {
    return makeScriptCGBuilder(
        dir,
        name,
        CGBuilderType.ZERO_ONE_CFA_WITHOUT_CORRELATION_TRACKING,
        JSCallGraphBuilderUtil.class.getClassLoader());
  }

  public static CallGraph makeScriptCG(
      String dir, String name, CGBuilderType builderType, ClassLoader loader)
      throws IOException, IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder b = makeScriptCGBuilder(dir, name, builderType, loader);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    // dumpCG(b.getPointerAnalysis(), CG);
    return CG;
  }

  public static CallGraph makeScriptCG(
      SourceModule[] scripts, CGBuilderType builderType, IRFactory<IMethod> irFactory)
      throws IllegalArgumentException, CancelException, WalaException {
    CAstRewriterFactory<?, ?> preprocessor =
        builderType.extractCorrelatedPairs
            ? new CorrelatedPairExtractorFactory(translatorFactory, scripts)
            : null;
    PropagationCallGraphBuilder b =
        makeCGBuilder(makeLoaders(preprocessor), scripts, builderType, irFactory);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    // dumpCG(b.getPointerAnalysis(), CG);
    return CG;
  }

  public static JSCFABuilder makeHTMLCGBuilder(URL url, Supplier<JSSourceExtractor> fExtractor)
      throws WalaException {
    return makeHTMLCGBuilder(url, CGBuilderType.ZERO_ONE_CFA, fExtractor);
  }

  public static JSCFABuilder makeHTMLCGBuilder(
      URL url, Supplier<JSSourceExtractor> fExtractor, Reader r) throws WalaException {
    return makeHTMLCGBuilder(url, CGBuilderType.ZERO_ONE_CFA, fExtractor, r);
  }

  public static JSCFABuilder makeHTMLCGBuilder(
      URL url, CGBuilderType builderType, Supplier<JSSourceExtractor> fExtractor)
      throws WalaException {
    try (Reader r = WebUtil.getStream(url)) {
      return makeHTMLCGBuilder(url, builderType, fExtractor, r);
    } catch (IOException e) {
      throw new WalaException("failed to read " + url, e);
    }
  }

  public static JSCFABuilder makeHTMLCGBuilder(
      URL url, CGBuilderType builderType, Supplier<JSSourceExtractor> fExtractor, Reader r)
      throws WalaException {
    IRFactory<IMethod> irFactory = AstIRFactory.makeDefaultFactory();
    CAstRewriterFactory<?, ?> preprocessor =
        builderType.extractCorrelatedPairs
            ? new CorrelatedPairExtractorFactory(translatorFactory, url)
            : null;
    JavaScriptLoaderFactory loaders = new WebPageLoaderFactory(translatorFactory, preprocessor);
    SourceModule[] scriptsArray = makeHtmlScope(url, loaders, fExtractor, r);

    JSCFABuilder builder = makeCGBuilder(loaders, scriptsArray, builderType, irFactory);
    if (builderType.extractCorrelatedPairs)
      builder.setContextSelector(
          new PropertyNameContextSelector(
              builder.getAnalysisCache(), 2, builder.getContextSelector()));
    builder.setBaseURL(url);
    return builder;
  }

  public static SourceModule[] makeHtmlScope(
      URL url, JavaScriptLoaderFactory loaders, Supplier<JSSourceExtractor> fExtractor) {
    try (Reader r = WebUtil.getStream(url)) {
      return makeHtmlScope(url, loaders, fExtractor, r);
    } catch (IOException e) {
      assert false : e;
      return null;
    }
  }

  public static SourceModule[] makeHtmlScope(
      URL url, JavaScriptLoaderFactory loaders, Supplier<JSSourceExtractor> fExtractor, Reader r) {
    Set<Module> scripts = HashSetFactory.make();

    JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
    scripts.add(getPrologueFile("prologue.js"));
    scripts.add(getPrologueFile("preamble.js"));

    try {
      scripts.addAll(WebUtil.extractScriptFromHTML(url, fExtractor, r).fst);
    } catch (Error e) {
      SourceModule dummy = new SourceURLModule(url);
      scripts.add(dummy);
      ((CAstAbstractLoader) loaders.getTheLoader()).addMessages(dummy, e.warning);
    }

    SourceModule[] scriptsArray = scripts.toArray(new SourceModule[0]);
    return scriptsArray;
  }

  public static CallGraph makeHTMLCG(URL url, Supplier<JSSourceExtractor> fExtractor)
      throws IllegalArgumentException, CancelException, WalaException {
    SSAPropagationCallGraphBuilder b = makeHTMLCGBuilder(url, fExtractor);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    dumpCG(b.getCFAContextInterpreter(), b.getPointerAnalysis(), CG);
    return CG;
  }

  public static CallGraph makeHTMLCG(
      URL url, CGBuilderType builderType, Supplier<JSSourceExtractor> fExtractor)
      throws IllegalArgumentException, CancelException, WalaException {
    PropagationCallGraphBuilder b = makeHTMLCGBuilder(url, builderType, fExtractor);
    CallGraph CG = b.makeCallGraph(b.getOptions());
    return CG;
  }

  public static JSCFABuilder makeCGBuilder(
      JavaScriptLoaderFactory loaders,
      Module[] scripts,
      CGBuilderType builderType,
      IRFactory<IMethod> irFactory)
      throws WalaException {
    AnalysisScope scope = makeScope(scripts, loaders, JavaScriptLoader.JS);
    return makeCG(loaders, scope, builderType, irFactory);
  }

  protected static JSCFABuilder makeCG(
      JavaScriptLoaderFactory loaders,
      AnalysisScope scope,
      CGBuilderType builderType,
      IRFactory<IMethod> irFactory)
      throws WalaException {
    try {
      IClassHierarchy cha = makeHierarchy(scope, loaders);
      com.ibm.wala.cast.util.Util.checkForFrontEndErrors(cha);
      Iterable<Entrypoint> roots = makeScriptRoots(cha);
      JSAnalysisOptions options = makeOptions(scope, cha, roots);
      options.setHandleCallApply(builderType.handleCallApply());
      IAnalysisCacheView cache = makeCache(irFactory);
      JSCFABuilder builder =
          new JSZeroOrOneXCFABuilder(
              cha,
              options,
              cache,
              null,
              null,
              ZeroXInstanceKeys.ALLOCATIONS,
              builderType.useOneCFA());
      if (builderType.extractCorrelatedPairs())
        builder.setContextSelector(
            new PropertyNameContextSelector(
                builder.getAnalysisCache(), 2, builder.getContextSelector()));

      return builder;
    } catch (ClassHierarchyException e) {
      throw new RuntimeException("internal error building class hierarchy", e);
    }
  }

  public static CallGraph makeHTMLCG(URL url, CGBuilderType zeroOneCfaNoCallApply)
      throws IllegalArgumentException, CancelException, WalaException {
    return makeHTMLCG(url, zeroOneCfaNoCallApply, DefaultSourceExtractor.factory);
  }

  public static CallGraph makeHTMLCG(URL url)
      throws IllegalArgumentException, CancelException, WalaException {
    return makeHTMLCG(url, DefaultSourceExtractor.factory);
  }

  public static JSCFABuilder makeHTMLCGBuilder(URL url, CGBuilderType type, Reader r)
      throws WalaException {
    return makeHTMLCGBuilder(url, type, DefaultSourceExtractor.factory, r);
  }

  public static JSCFABuilder makeHTMLCGBuilder(URL url) throws WalaException {
    return makeHTMLCGBuilder(url, DefaultSourceExtractor.factory);
  }
}
