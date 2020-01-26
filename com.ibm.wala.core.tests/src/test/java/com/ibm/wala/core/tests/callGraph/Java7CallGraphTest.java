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

import static org.junit.Assume.assumeFalse;

import com.ibm.wala.analysis.reflection.java7.MethodHandles;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.shrike.DynamicCallGraphTestBase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.io.TemporaryFile;
import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import org.junit.Test;

public class Java7CallGraphTest extends DynamicCallGraphTestBase {

  @Test
  public void testOcamlHelloHash()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException,
          ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException,
          InterruptedException {
    // Known to be broken on Windows, but not intentionally so.  Please fix if you know how!
    // <https://github.com/wala/WALA/issues/608>
    assumeFalse(PlatformUtil.onWindows());

    if (!"True".equals(System.getenv("APPVEYOR"))) {
      testOCamlJar("hello_hash.jar");
    }
  }

  private void testOCamlJar(String jarFile, String... args)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException,
          ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException,
          InterruptedException {
    File F =
        TemporaryFile.urlToFile(
            jarFile.replace('.', '_') + ".jar", getClass().getClassLoader().getResource(jarFile));
    F.deleteOnExit();

    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            "base.txt", CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    scope.addToScope(ClassLoaderReference.Application, new JarFile(F, false));

    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, "Lpack/ocamljavaMain");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    options.setUseConstantSpecificKeys(true);
    IAnalysisCacheView cache = new AnalysisCacheImpl();

    SSAPropagationCallGraphBuilder builder =
        Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);

    MethodHandles.analyzeMethodHandles(options, builder);

    CallGraph cg = builder.makeCallGraph(options, null);

    System.err.println(cg);

    instrument(F.getAbsolutePath());
    run("pack.ocamljavaMain", null, args);

    checkNodes(
        cg,
        t -> {
          String s = t.toString();
          return s.contains("Lpack/") || s.contains("Locaml/stdlib/");
        });
  }
}
