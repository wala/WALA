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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Set;

import org.junit.Assert;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
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
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.io.FileProvider;

/**
 * TODO this class is a mess. rewrite.
 */
public class JSCallGraphBuilderUtil extends com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil {

  public static enum CGBuilderType {
    ZERO_ONE_CFA(false, true, true),
    ZERO_ONE_CFA_NO_CALL_APPLY(false, false, true),
    ONE_CFA(true, true, true);
    
    private final boolean useOneCFA;

    private final boolean handleCallApply;
    
    private final boolean extractCorrelatedPairs;

    private CGBuilderType(boolean useOneCFA, boolean handleCallApply, boolean extractCorrelatedPairs) {
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
   * create a CG builder for script.  Note that the script at dir/name is loaded via the classloader, not from the filesystem.
   */
  public static JSCFABuilder makeScriptCGBuilder(String dir, String name, CGBuilderType builderType) throws IOException, WalaException {
    URL script = getURLforFile(dir, name);
    CAstRewriterFactory preprocessor = builderType.extractCorrelatedPairs ? new CorrelatedPairExtractorFactory(translatorFactory, script) : null;
    JavaScriptLoaderFactory loaders = JSCallGraphUtil.makeLoaders(preprocessor);

    AnalysisScope scope = makeScriptScope(script, dir, name, loaders);

    return makeCG(loaders, scope, builderType, AstIRFactory.makeDefaultFactory());
  }

  public static URL getURLforFile(String dir, String name) throws IOException {
    File f = null;
    FileProvider provider = new FileProvider();
    try {
      f = provider.getFile(dir + File.separator + name, JSCallGraphBuilderUtil.class.getClassLoader());
    } catch (FileNotFoundException e) {
      // I guess we need to do this on Windows sometimes?  --MS
      // if this fails, we won't catch the exception
      f = provider.getFile(dir + "/" + name, JSCallGraphBuilderUtil.class.getClassLoader());
    }
    return f.toURI().toURL();
  }
  
  static AnalysisScope makeScriptScope(String dir, String name, JavaScriptLoaderFactory loaders) throws IOException {
    return makeScriptScope(getURLforFile(dir, name), dir, name, loaders);
  }

  public static SourceModule getPrologueFile(final String name) {
    return new SourceURLModule(JSCallGraphBuilderUtil.class.getClassLoader().getResource(name)) {
      @Override
      public String getName() {
        return name;
      }      
    };
  }
  
  static AnalysisScope makeScriptScope(URL script, String dir, String name, JavaScriptLoaderFactory loaders) throws IOException {
    return makeScope(
        new SourceModule[] { 
            (script.openConnection() instanceof JarURLConnection)? new SourceURLModule(script): makeSourceModule(script, dir, name), 
            getPrologueFile("prologue.js")
        }, loaders, JavaScriptLoader.JS);
    
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
    IRFactory<IMethod> irFactory = AstIRFactory.makeDefaultFactory();
    CAstRewriterFactory preprocessor = builderType.extractCorrelatedPairs ? new CorrelatedPairExtractorFactory(translatorFactory, url) : null;
    JavaScriptLoaderFactory loaders = new WebPageLoaderFactory(translatorFactory, preprocessor);
    SourceModule[] scriptsArray = makeHtmlScope(url, loaders);
    
    JSCFABuilder builder = makeCGBuilder(loaders, scriptsArray, builderType, irFactory);
    if(builderType.extractCorrelatedPairs)
      builder.setContextSelector(new PropertyNameContextSelector(builder.getAnalysisCache(), 2, builder.getContextSelector()));
    builder.setBaseURL(url);
    return builder;
  }

  public static SourceModule[] makeHtmlScope(URL url, JavaScriptLoaderFactory loaders) {
    Set<SourceModule> scripts = HashSetFactory.make();
    
    JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
    scripts.add(getPrologueFile("prologue.js"));
    scripts.add(getPrologueFile("preamble.js"));

    try {
      scripts.addAll(WebUtil.extractScriptFromHTML(url, true).fst);
    } catch (Error e) {
      SourceModule dummy = new SourceURLModule(url);
      scripts.add(dummy);
      ((CAstAbstractLoader)loaders.getTheLoader()).addMessage(dummy, e.warning);
    }
        
    SourceModule[] scriptsArray = scripts.toArray(new SourceModule[ scripts.size() ]);
    return scriptsArray;
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
