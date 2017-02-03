package com.ibm.wala.cast.js.examples.hybrid;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import com.ibm.wala.cast.ipa.callgraph.CrossLanguageMethodTargetSelector;
import com.ibm.wala.cast.ipa.callgraph.StandardFunctionTargetSelector;
import com.ibm.wala.cast.ipa.cha.CrossLanguageClassHierarchy;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructTargetSelector;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptEntryPoints;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ComposedEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

public class Driver {

  public static void addDefaultDispatchLogic(AnalysisOptions options, AnalysisScope scope, IClassHierarchy cha, AnalysisCache cache) {
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);

    Map<Atom,MethodTargetSelector> methodTargetSelectors = HashMapFactory.make();
    methodTargetSelectors.put(JavaScriptLoader.JS.getName(), new JavaScriptConstructTargetSelector(cha,
        new StandardFunctionTargetSelector(cha, options.getMethodTargetSelector())));
    methodTargetSelectors.put(Language.JAVA.getName(), options.getMethodTargetSelector());

    options.setSelector(new CrossLanguageMethodTargetSelector(methodTargetSelectors));
  }

  public static void main(String[] args) throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
    JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());

    HybridClassLoaderFactory loaders = new HybridClassLoaderFactory();

    HybridAnalysisScope scope = new HybridAnalysisScope();
    FileProvider files = new FileProvider();
    AnalysisScopeReader.read(scope, args[0], files.getFile("Java60RegressionExclusions.txt"), Driver.class.getClassLoader(), files);

    scope.addToScope(
        scope.getJavaScriptLoader(),
        JSCallGraphBuilderUtil.getPrologueFile("prologue.js"));
    for(int i = 1; i < args.length; i++) {
      URL script = Driver.class.getClassLoader().getResource(args[i]);
      scope.addToScope(
          scope.getJavaScriptLoader(),
          new SourceURLModule(script));
    }
    
    System.err.println(scope);
    
    IClassHierarchy cha = CrossLanguageClassHierarchy.make(scope, loaders);
    
    Iterable<Entrypoint> jsRoots =
        new JavaScriptEntryPoints(cha, cha.getLoader(scope.getJavaScriptLoader()));

    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
    
    ComposedEntrypoints roots = new ComposedEntrypoints(jsRoots, entrypoints);

    AnalysisOptions options = new AnalysisOptions(scope, roots);
    
    IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory();

    AnalysisCache cache = new AnalysisCacheImpl(factory);

    addDefaultDispatchLogic(options, scope, cha, cache);

    JavaJavaScriptHybridCallGraphBuilder b = new JavaJavaScriptHybridCallGraphBuilder(cha, options, cache);
    
    System.err.println(b.makeCallGraph(options));
  }
}
