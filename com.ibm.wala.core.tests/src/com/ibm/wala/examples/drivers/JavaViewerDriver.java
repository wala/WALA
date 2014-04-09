package com.ibm.wala.examples.drivers;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.viewer.WalaViewer;

/**
 * Allows viewing the ClassHeirarcy, CallGraph and Pointer Analysis built from a given classpath.
 * @author yinnonh
 *
 */
public class JavaViewerDriver {
  public static void main(String[] args) throws ClassHierarchyException, IOException, CallGraphBuilderCancelException {
    Properties p = CommandLine.parse(args);
    validateCommandLine(p);
    run(p.getProperty("appClassPath"), p.getProperty("exclusionFile", CallGraphTestUtil.REGRESSION_EXCLUSIONS));
  }
  
  public static void validateCommandLine(Properties p) {
    if (p.get("appClassPath") == null) {
      throw new UnsupportedOperationException("expected command-line to include -appClassPath");
    }
  }

  private static void run(String classPath, String exclusionFilePath) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException{

    File exclusionFile = (new FileProvider()).getFile(exclusionFilePath);
    AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(classPath, exclusionFile != null ? exclusionFile
        : new File(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

    ClassHierarchy cha = ClassHierarchy.make(scope);

    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

    // //
    // build the call graph
    // //
    com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);

    PointerAnalysis pa = builder.getPointerAnalysis();
    new WalaViewer(cg, pa);
    
  }
}
