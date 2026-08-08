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
package com.ibm.wala.core.tests.cha;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.classLoader.BytecodeClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.TypeReference;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;

/** A test of support for source file mapping */
public class SourceMapTest extends WalaTestCase {
  private static final ClassLoader MY_CLASSLOADER = SourceMapTest.class.getClassLoader();

  private static final String CLASS_IN_PRIMORDIAL_JAR = "Lcom/ibm/wala/model/SyntheticFactory";

  @Test
  public void testHello() throws ClassHierarchyException, IOException {

    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(TestConstants.HELLO, null, MY_CLASSLOADER);
    // TODO: it's annoying to have to build a class hierarchy here.
    // see feature 38676
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    TypeReference t =
        TypeReference.findOrCreate(scope.getApplicationLoader(), TestConstants.HELLO_MAIN);
    IClass klass = cha.lookupClass(t);
    assertThat(klass).isNotNull();
    String sourceFile = klass.getSourceFileName();
    System.err.println("Source file: " + sourceFile);
    assertThat(sourceFile).isNotNull();
  }

  @Test
  public void testHelloFromJar() throws ClassHierarchyException, IOException {

    // Copy the `hello` test subject into a temporary JAR, so that the scope file's `classFile` and
    // `sourceFile` entries must resolve `hello/Hello.class` and `hello/Hello.java` as entries of
    // that JAR rather than as unpacked files on the filesystem.
    File jar = createJarContaining("hello/Hello.class", "hello/Hello.java");
    try (URLClassLoader jarLoader =
        new URLClassLoader(new URL[] {jar.toURI().toURL()}, MY_CLASSLOADER)) {
      AnalysisScope scope =
          AnalysisScopeReader.instance.readJavaScope(TestConstants.HELLO, null, jarLoader);
      ClassHierarchy cha = ClassHierarchyFactory.make(scope);
      TypeReference t =
          TypeReference.findOrCreate(scope.getApplicationLoader(), TestConstants.HELLO_MAIN);
      IClass klass = cha.lookupClass(t);
      assertThat(klass).isNotNull();
      String sourceFile = klass.getSourceFileName();
      System.err.println("Source file: " + sourceFile);
      assertThat(sourceFile).isNotNull();
    }
  }

  private static File createJarContaining(String... entries) throws IOException {
    File jar = File.createTempFile("wala-source-map-test", ".jar");
    jar.deleteOnExit();
    try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
      for (String entry : entries) {
        try (InputStream in = MY_CLASSLOADER.getResourceAsStream(entry)) {
          assertThat(in).as("classpath resource %s", entry).isNotNull();
          out.putNextEntry(new JarEntry(entry));
          in.transferTo(out);
          out.closeEntry();
        }
      }
    }
    return jar;
  }

  @Test
  public void testFromJar() throws ClassHierarchyException, IOException {

    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(TestConstants.HELLO, null, MY_CLASSLOADER);
    // TODO: it's annoying to have to build a class hierarchy here.
    // open a feature to fix this
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    TypeReference t =
        TypeReference.findOrCreate(scope.getPrimordialLoader(), CLASS_IN_PRIMORDIAL_JAR);
    IClass klass = cha.lookupClass(t);
    assertThat(klass).isNotNull();
    String sourceFile = klass.getSourceFileName();
    assertThat(sourceFile).isNotNull();
    System.err.println("Source file: " + sourceFile);
    Module container = ((BytecodeClass<?>) klass).getContainer();
    assertThat(container).isNotNull();
    System.err.println("container: " + container);
  }
}
