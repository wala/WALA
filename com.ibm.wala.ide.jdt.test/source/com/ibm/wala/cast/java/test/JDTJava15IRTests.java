/*
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.ibm.wala.cast.java.test;

import com.ibm.wala.util.CancelException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.junit.Test;

public class JDTJava15IRTests extends JDTJavaTest {

  public JDTJava15IRTests() {
    super(JDTJavaIRTests.PROJECT);
  }

  @Test
  public void testAnonGeneNullarySimple()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testAnonymousGenerics()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testBasicsGenerics() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testCocovariant() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testCustomGenericsAndFields()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testEnumSwitch() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testExplicitBoxingTest()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testGenericArrays() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testGenericMemberClasses()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testGenericSuperSink() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testMethodGenerics() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testMoreOverriddenGenerics()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testNotSoSimpleEnums() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testOverridesOnePointFour()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testSimpleEnums() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testSimpleEnums2() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testVarargs() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testVarargsCovariant() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testVarargsOverriding()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testWildcards() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testAnnotations() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testTypeInferencePrimAndStringOp()
      throws IllegalArgumentException, CancelException, IOException {
    String pkgName = "javaonepointfive";
    runTest(
        singlePkgTestSrc(pkgName),
        rtJar,
        simplePkgTestEntryPoint(pkgName),
        Collections.singletonList(
            new TypeInferenceAssertion(pkgName + File.separator + singleInputForTest())),
        false,
        null);
  }
}
