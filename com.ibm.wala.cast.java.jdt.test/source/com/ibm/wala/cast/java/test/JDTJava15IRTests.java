/******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *****************************************************************************/
package com.ibm.wala.cast.java.test;

import org.junit.Test;

public class JDTJava15IRTests extends JDTJavaTest {

  public JDTJava15IRTests() {
    super(JDTJavaIRTests.PROJECT);
  }
  
  @Test
  public void testAnonGeneNullarySimple() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testAnonymousGenerics() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testBasicsGenerics() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testCocovariant() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testCustomGenericsAndFields() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testEnumSwitch() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testExplicitBoxingTest() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testGenericArrays() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testGenericMemberClasses() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testGenericSuperSink() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testMethodGenerics() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testMoreOverriddenGenerics() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testNotSoSimpleEnums() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testOverridesOnePointFour() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testSimpleEnums() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testSimpleEnums2() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testVarargs() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testVarargsCovariant() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testVarargsOverriding() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testWildcards() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

  @Test
  public void testAnnotations() {
    runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
  }

}
