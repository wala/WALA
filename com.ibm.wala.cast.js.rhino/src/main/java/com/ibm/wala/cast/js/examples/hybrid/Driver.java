package com.ibm.wala.cast.js.examples.hybrid;

import com.ibm.wala.cast.ipa.callgraph.CrossLanguageCallGraph;
import com.ibm.wala.cast.ipa.callgraph.CrossLanguageMethodTargetSelector;
import com.ibm.wala.cast.ipa.callgraph.StandardFunctionTargetSelector;
import com.ibm.wala.cast.ipa.cha.CrossLanguageClassHierarchy;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructTargetSelector;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptEntryPoints;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ComposedEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.FakeRootClass;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class Driver {

  public static void addDefaultDispatchLogic(AnalysisOptions options, IClassHierarchy cha) {
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);

    Map<Atom, MethodTargetSelector> methodTargetSelectors = HashMapFactory.make();
    methodTargetSelectors.put(
        JavaScriptLoader.JS.getName(),
        new JavaScriptConstructTargetSelector(
            cha, new StandardFunctionTargetSelector(cha, options.getMethodTargetSelector())));
    methodTargetSelectors.put(Language.JAVA.getName(), options.getMethodTargetSelector());

    options.setSelector(new CrossLanguageMethodTargetSelector(methodTargetSelectors));
  }

  public static void main(String[] args)
      throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
    JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());

    HybridClassLoaderFactory loaders = new HybridClassLoaderFactory();

    HybridAnalysisScope scope = new HybridAnalysisScope();
    FileProvider files = new FileProvider();
    AnalysisScopeReader.instance.read(
        scope,
        args[0],
        files.getFile("Java60RegressionExclusions.txt"),
        Driver.class.getClassLoader());

    scope.addToScope(scope.getJavaScriptLoader(), JSCallGraphUtil.getPrologueFile("prologue.js"));
    for (int i = 1; i < args.length; i++) {
      URL script = Driver.class.getClassLoader().getResource(args[i]);
      scope.addToScope(scope.getJavaScriptLoader(), new SourceURLModule(script));
    }

    System.err.println(scope);

    IClassHierarchy cha = CrossLanguageClassHierarchy.make(scope, loaders);

    Iterable<Entrypoint> jsRoots =
        new JavaScriptEntryPoints(cha, cha.getLoader(scope.getJavaScriptLoader()));

    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha);

    ComposedEntrypoints roots = new ComposedEntrypoints(jsRoots, entrypoints);

    AnalysisOptions options = new AnalysisOptions(scope, roots);

    IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory();

    IAnalysisCacheView cache = new AnalysisCacheImpl(factory);

    addDefaultDispatchLogic(options, cha);

    JavaJavaScriptHybridCallGraphBuilder b =
        new JavaJavaScriptHybridCallGraphBuilder(
            new FakeRootMethod(
                new FakeRootClass(CrossLanguageCallGraph.crossCoreLoader, cha), options, cache),
            options,
            cache);

    System.err.println(b.makeCallGraph(options));
  }
}
