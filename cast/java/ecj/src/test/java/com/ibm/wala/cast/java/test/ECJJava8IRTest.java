/*
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.ibm.wala.cast.java.test;

import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ECJJava8IRTest extends ECJIRTests {

  private static final String packageName = "javaeight";

  public ECJJava8IRTest() {
    dump = true;
  }

  @Test
  public void testEmptyLambda() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc(packageName, "EmptyLambda"),
        rtJar,
        simplePkgTestEntryPoint(packageName, "EmptyLambda"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testCondExprLambda() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc(packageName, "CondExprLambda"),
        rtJar,
        simplePkgTestEntryPoint(packageName, "CondExprLambda"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testObjectLambda() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc(packageName, "ObjectLambda"),
        rtJar,
        simplePkgTestEntryPoint(packageName, "ObjectLambda"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testLexicalLambda() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc(packageName, "LexicalLambda"),
        rtJar,
        simplePkgTestEntryPoint(packageName, "LexicalLambda"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testLexicalLocalLambda()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc(packageName, "LexicalLocalLambda"),
        rtJar,
        simplePkgTestEntryPoint(packageName, "LexicalLocalLambda"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testVoidLambda() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc(packageName, "VoidLambda"),
        rtJar,
        simplePkgTestEntryPoint(packageName, "VoidLambda"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testTwoLambdas() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc(packageName, "TwoLambdas"),
        rtJar,
        simplePkgTestEntryPoint(packageName, "TwoLambdas"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testParamsAndCapture() throws IllegalArgumentException, CancelException, IOException {
    dump = true;
    runTest(
        singlePkgTestSrc(packageName, "ParamsAndCapture"),
        rtJar,
        simplePkgTestEntryPoint(packageName, "ParamsAndCapture"),
        emptyList,
        true,
        null);
  }
}
