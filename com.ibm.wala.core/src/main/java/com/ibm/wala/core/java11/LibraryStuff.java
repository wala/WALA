package com.ibm.wala.core.java11;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.util.NullProgressMonitor;

public class LibraryStuff {

  public static void main(String[] args) throws Exception {
    try {
      AnalysisScopeReader r = new Java9AnalysisScopeReader();
      AnalysisScope scope =
          r.readJavaScope(
              (args.length > 0 ? args[0] : "wala.testdata.txt"),
              null,
              LibraryStuff.class.getClassLoader());
      System.err.println(scope);

      ClassLoaderFactory factory = new ClassLoaderFactoryImpl(null);
      IClassHierarchy cha = ClassHierarchyFactory.make(scope, factory);

      System.err.println(cha);

      Iterable<Entrypoint> entrypoints =
          com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha);

      AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
      options.setReflectionOptions(ReflectionOptions.NONE);

      IAnalysisCacheView cache = new AnalysisCacheImpl();

      SSAPropagationCallGraphBuilder builder =
          Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha);
      CallGraph cg = builder.makeCallGraph(options, new NullProgressMonitor());

      System.err.println(cg);

      SDG<InstanceKey> G =
          new SDG<>(
              cg,
              builder.getPointerAnalysis(),
              DataDependenceOptions.FULL,
              ControlDependenceOptions.FULL);

      System.err.println(G);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}
