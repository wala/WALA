package com.ibm.wala.cast.java.ecj.util;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.java.client.impl.ZeroOneContainerCFABuilderFactory;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.io.CommandLine;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * Driver that constructs a call graph for an application specified as a directory of source code.
 * Example of using the JDT front-end based on ECJ. Useful for getting some code to copy-paste.
 */
public class SourceDirCallGraph {

  @FunctionalInterface
  public interface Processor {
    public void process(CallGraph CG, CallGraphBuilder<?> builder, long time);
  }

  /**
   * Usage: SourceDirCallGraph -sourceDir file_path -mainClass class_name
   *
   * <p>If given -mainClass, uses main() method of class_name as entrypoint. Class name should start
   * with an 'L'.
   *
   * <p>Example args: -sourceDir /tmp/srcTest -mainClass LFoo
   */
  public static void main(String[] args)
      throws ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException,
          IOException {
    new SourceDirCallGraph()
        .doit(
            args,
            (cg, builder, time) -> {
              System.out.println("done");
              System.out.println("took " + time + "ms");
              System.out.println(CallGraphStats.getStats(cg));
            });
  }

  protected ClassLoaderFactory getLoaderFactory(AnalysisScope scope) {
    return new ECJClassLoaderFactory(scope.getExclusions());
  }

  public void doit(String[] args, Processor processor)
      throws ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException,
          IOException {
    long start = System.currentTimeMillis();
    Properties p = CommandLine.parse(args);
    String sourceDir = p.getProperty("sourceDir");
    String mainClass = p.getProperty("mainClass");
    AnalysisScope scope = new JavaSourceAnalysisScope();
    // add standard libraries to scope
    String[] stdlibs = WalaProperties.getJ2SEJarFiles();
    for (String stdlib : stdlibs) {
      scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stdlib));
    }
    // add the source directory
    File root = new File(sourceDir);
    if (root.isDirectory()) {
      scope.addToScope(JavaSourceAnalysisScope.SOURCE, new SourceDirectoryTreeModule(root));
    } else {
      String srcFileName = sourceDir.substring(sourceDir.lastIndexOf(File.separator) + 1);
      assert root.exists() : "couldn't find " + sourceDir;
      scope.addToScope(
          JavaSourceAnalysisScope.SOURCE, new SourceFileModule(root, srcFileName, null));
    }

    // build the class hierarchy
    IClassHierarchy cha = ClassHierarchyFactory.make(scope, getLoaderFactory(scope));
    System.out.println(cha.getNumberOfClasses() + " classes");
    System.out.println(Warnings.asString());
    Warnings.clear();
    AnalysisOptions options = new AnalysisOptions();
    Iterable<Entrypoint> entrypoints = getEntrypoints(mainClass, cha);
    options.setEntrypoints(entrypoints);
    options.getSSAOptions().setDefaultValues(SymbolTable::getDefaultValue);
    // you can dial down reflection handling if you like
    options.setReflectionOptions(ReflectionOptions.NONE);
    IAnalysisCacheView cache =
        new AnalysisCacheImpl(AstIRFactory.makeDefaultFactory(), options.getSSAOptions());
    // CallGraphBuilder builder = new ZeroCFABuilderFactory().make(options, cache,
    // cha, scope,
    // false);
    CallGraphBuilder<?> builder = new ZeroOneContainerCFABuilderFactory().make(options, cache, cha);
    System.out.println("building call graph...");
    CallGraph cg = builder.makeCallGraph(options, null);
    long end = System.currentTimeMillis();

    processor.process(cg, builder, end - start);
  }

  protected Iterable<Entrypoint> getEntrypoints(String mainClass, IClassHierarchy cha) {
    Iterable<Entrypoint> entrypoints =
        Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE, cha, new String[] {mainClass});
    return entrypoints;
  }
}
