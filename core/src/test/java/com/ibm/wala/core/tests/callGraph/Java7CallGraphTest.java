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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.ibm.wala.analysis.reflection.java7.MethodHandles;
import com.ibm.wala.classLoader.JavaLanguage;
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
import com.ibm.wala.shrike.cg.Runtime;
import com.ibm.wala.shrike.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.io.TemporaryFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

public class Java7CallGraphTest extends DynamicCallGraphTestBase {

  @TempDir private Path temporaryDirectory;

  public static class ConstructorReentryProbe {
    private static class FailsOnce {
      FailsOnce(boolean fail) {
        Runtime.execution(FailsOnce.class.getName(), "<init>(Z)V", Runtime.NULL_TAG);
        if (fail) {
          throw new IllegalStateException();
        }
        Runtime.termination(FailsOnce.class.getName(), "<init>(Z)V", Runtime.NULL_TAG, false);
      }
    }

    public static void main(String[] args) {
      Runtime.execution(
          ConstructorReentryProbe.class.getName(), "main([Ljava/lang/String;)V", Runtime.NULL_TAG);
      try {
        new FailsOnce(true);
      } catch (IllegalStateException expected) {
        // Reenter the same constructor from the same stack depth.
      }
      new FailsOnce(false);
      Runtime.termination(
          ConstructorReentryProbe.class.getName(),
          "main([Ljava/lang/String;)V",
          Runtime.NULL_TAG,
          false);
    }
  }

  @Override
  protected Path getTemporaryDirectory() {
    return temporaryDirectory;
  }

  @Test
  public void testConstructorReentryAfterException() throws IOException, InterruptedException {
    Process process =
        new ProcessBuilder(
                Path.of(
                        System.getProperty("java.home"),
                        "bin",
                        PlatformUtil.onWindows() ? "java.exe" : "java")
                    .toString(),
                "-Xverify:all",
                "-DdynamicCGFile=" + getDynamicCGLocation(),
                "-cp",
                System.getProperty("java.class.path"),
                ConstructorReentryProbe.class.getName())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertThat(process.waitFor()).as(output).isZero();

    String caller = ConstructorReentryProbe.class.getName().replace('.', '/');
    String callee = ConstructorReentryProbe.FailsOnce.class.getName().replace('.', '/');
    String edge = caller + "\tmain([Ljava/lang/String;)V\t" + callee + "\t<init>(Z)V";
    assertThat(traceLines()).filteredOn(edge::equals).hasSize(2);
  }

  private List<String> traceLines() throws IOException {
    try (GZIPInputStream trace =
        new GZIPInputStream(Files.newInputStream(getDynamicCGLocation()))) {
      return new String(trace.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
    }
  }

  @Test
  public void testConstructorExceptionalExits()
      throws IOException,
          ClassNotFoundException,
          InvalidClassFileException,
          FailureException,
          InterruptedException {
    String classFile = "dynamicCG/ConstructorExceptionTrace";
    Path subjectJar = temporaryDirectory.resolve("constructor-exception-trace.jar");
    try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(subjectJar))) {
      for (String suffix : List.of("", "$FailingBase", "$FailBefore", "$FailAfter")) {
        String entry = classFile + suffix + ".class";
        jar.putNextEntry(new JarEntry(entry));
        try (InputStream in =
            Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(entry))) {
          in.transferTo(jar);
        }
        jar.closeEntry();
      }
    }

    instrument(subjectJar.toString());
    run("dynamicCG.ConstructorExceptionTrace", null);

    assertThat(traceLines())
        .contains(
            "dynamicCG/ConstructorExceptionTrace$FailBefore\t<init>()V\t"
                + "dynamicCG/ConstructorExceptionTrace$FailingBase\t<init>()V",
            "dynamicCG/ConstructorExceptionTrace\tmain([Ljava/lang/String;)V\t"
                + "dynamicCG/ConstructorExceptionTrace\tafterFailBefore()V",
            "dynamicCG/ConstructorExceptionTrace\tmain([Ljava/lang/String;)V\t"
                + "dynamicCG/ConstructorExceptionTrace\tafterFailAfter()V",
            "dynamicCG/ConstructorExceptionTrace\tmain([Ljava/lang/String;)V\t"
                + "dynamicCG/ConstructorExceptionTrace\tafterCaughtReturn()V");
  }

  @Test
  @DisabledOnOs(
      value = OS.WINDOWS,
      disabledReason = "haven't been able to get `ocamljava.bat` to work on Windows yet")
  public void testOcamlHelloHash()
      throws ClassHierarchyException,
          IllegalArgumentException,
          CancelException,
          IOException,
          ClassNotFoundException,
          InvalidClassFileException,
          FailureException,
          SecurityException,
          InterruptedException {
    // Known to be broken on Windows, but not intentionally so.  Please fix if you know how!
    // <https://github.com/wala/WALA/issues/608>
    assumeFalse(PlatformUtil.onWindows());

    if (!"True".equals(System.getenv("APPVEYOR"))) {
      testOCamlJar("hello_hash.jar");
    }
  }

  private void testOCamlJar(String jarFile, String... args)
      throws ClassHierarchyException,
          IllegalArgumentException,
          CancelException,
          IOException,
          ClassNotFoundException,
          InvalidClassFileException,
          FailureException,
          SecurityException,
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
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "Lpack/ocamljavaMain");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    options.setUseConstantSpecificKeys(true);
    IAnalysisCacheView cache = new AnalysisCacheImpl();

    SSAPropagationCallGraphBuilder builder =
        Util.makeZeroCFABuilder(JavaLanguage.get(), options, cache, cha);

    MethodHandles.analyzeMethodHandles(options, builder);

    CallGraph cg = builder.makeCallGraph(options, null);

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
