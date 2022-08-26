/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.core.tests.callGraph;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assume.assumeThat;

import com.ibm.wala.analysis.reflection.java7.MethodHandles;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ResourceJarFileModule;
import com.ibm.wala.core.tests.shrike.DynamicCallGraphTestBase;
import com.ibm.wala.core.util.CancelRuntimeException;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.tests.util.SlowTests;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import java.io.IOException;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTests.class)
public class KawaCallGraphTest extends DynamicCallGraphTestBase {

  @Test
  public void testKawaChess()
      throws ClassHierarchyException, IllegalArgumentException, IOException, SecurityException {
    assumeThat("not running on Travis CI", System.getenv("TRAVIS"), nullValue());

    CallGraph CG =
        testKawa(
            new ResourceJarFileModule(getClass().getClassLoader().getResource("kawachess.jar")),
            "main");

    Set<CGNode> status =
        getNodes(
            CG,
            "Lchess",
            "startingStatus",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    assert !status.isEmpty();

    Set<CGNode> color =
        getNodes(
            CG,
            "Lchess",
            "startingColor",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    assert !color.isEmpty();

    Set<CGNode> loadImage =
        getNodes(
            CG, "Limg", "loadImage", "(Ljava/lang/CharSequence;)Ljava/awt/image/BufferedImage;");
    assert !loadImage.isEmpty();

    Set<CGNode> append$v =
        getNodes(CG, "Lkawa/lang/Quote", "append$V", "([Ljava/lang/Object;)Ljava/lang/Object;");
    assert !append$v.isEmpty();

    Set<CGNode> clinit = getNodes(CG, "Lkawa/lib/kawa/base", "<clinit>", "()V");
    assert !clinit.isEmpty();
  }

  @Test
  public void testKawaTest()
      throws ClassHierarchyException, IllegalArgumentException, IOException, SecurityException {
    assumeThat("not running on Travis CI", System.getenv("TRAVIS"), nullValue());

    CallGraph CG =
        testKawa(
            new ResourceJarFileModule(getClass().getClassLoader().getResource("kawatest.jar")),
            "test");

    Set<CGNode> nodes = getNodes(CG, "Ltest", "plusish$V", "(Lgnu/lists/LList;)Ljava/lang/Object;");
    assert !nodes.isEmpty();
  }

  private static Set<CGNode> getNodes(CallGraph CG, String cls, String method, String descr) {
    Set<CGNode> nodes =
        CG.getNodes(
            MethodReference.findOrCreate(
                TypeReference.find(ClassLoaderReference.Application, cls),
                Atom.findOrCreateUnicodeAtom(method),
                Descriptor.findOrCreateUTF8(descr)));
    return nodes;
  }

  /** Maximum number of outer fixed point iterations to use when building the Kawa call graph. */
  private static final int MAX_ITERATIONS = 6;

  /**
   * Builds a call graph for a Kawa module. Call graph construction is terminated after {@link
   * #MAX_ITERATIONS} runs of the outer fixed point loop of call graph construction.
   *
   * @param code the module
   * @param main entrypoint method for the call graph
   * @return the call graph
   */
  private CallGraph testKawa(Module code, String main)
      throws ClassHierarchyException, IllegalArgumentException, IOException, SecurityException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            "base.txt", CallGraphTestUtil.REGRESSION_EXCLUSIONS_FOR_GUI);
    scope.addToScope(
        ClassLoaderReference.Application,
        new ResourceJarFileModule(getClass().getClassLoader().getResource("kawa.jar")));
    scope.addToScope(ClassLoaderReference.Application, code);

    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, 'L' + main);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    IAnalysisCacheView cache = new AnalysisCacheImpl();

    options.setReflectionOptions(ReflectionOptions.STRING_ONLY);
    options.setTraceStringConstants(true);
    options.setUseConstantSpecificKeys(true);

    SSAPropagationCallGraphBuilder builder =
        Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha);

    MethodHandles.analyzeMethodHandles(options, builder);

    CallGraph cg;
    try {
      cg =
          builder.makeCallGraph(
              options,
              new IProgressMonitor() {
                private long time = System.currentTimeMillis();

                private int iterations = 0;

                @Override
                public void beginTask(String task, int totalWork) {
                  noteElapsedTime();
                }

                private void noteElapsedTime() {
                  long now = System.currentTimeMillis();
                  if (now - time >= 10000) {
                    System.out.println("worked " + (now - time));
                    System.out.flush();
                    time = now;
                  }
                }

                @Override
                public void subTask(String subTask) {
                  noteElapsedTime();
                }

                @Override
                public void cancel() {
                  assert false;
                }

                @Override
                public boolean isCanceled() {
                  return false;
                }

                @Override
                public void done() {
                  noteElapsedTime();
                }

                @Override
                public void worked(int units) {
                  noteElapsedTime();
                  iterations++;
                  if (iterations >= MAX_ITERATIONS) {
                    throw CancelRuntimeException.make("should have run long enough");
                  }
                }

                @Override
                public String getCancelMessage() {
                  assert false : "should not cancel";
                  return null;
                }
              });
    } catch (CallGraphBuilderCancelException cgbe) {
      cg = cgbe.getPartialCallGraph();
    }

    return cg;
  }
}
