/******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *****************************************************************************/
package com.ibm.wala.cast.java.test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.cast.java.client.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.test.ide.IDEIRTestUtil;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.plugin.CoreTestsPlugin;
import com.ibm.wala.ide.tests.util.EclipseTestUtil;
import com.ibm.wala.ide.util.EclipseFileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class JDTJava15IRTests extends IRTests {

  public JDTJava15IRTests() {
    super(JDTJavaIRTests.PROJECT_NAME);
  }

  @Override
  protected void populateScope(JavaSourceAnalysisEngine engine, Collection<String> sources, List<String> libs) throws IOException {
    IDEIRTestUtil.populateScope(projectName, (JDTJavaSourceAnalysisEngine)engine, sources, libs);
  }

  @BeforeClass
  public static void beforeClass() {
    EclipseTestUtil.importZippedProject(TestPlugin.getDefault(), JDTJavaIRTests.PROJECT_NAME, JDTJavaIRTests.PROJECT_ZIP, new NullProgressMonitor());
    System.err.println("finish importing project");
  }

  @AfterClass
  public static void afterClass() {
    EclipseTestUtil.destroyProject(JDTJavaIRTests.PROJECT_NAME);
  }

  @Override
  protected JavaSourceAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors) {
    JavaSourceAnalysisEngine engine = new JDTJavaSourceAnalysisEngine(JDTJavaIRTests.PROJECT_NAME) {
      @Override
      protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
        return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE, cha, mainClassDescriptors);
      }
    };

    try {
      engine.setExclusionsFile((new EclipseFileProvider())
          .getFileFromPlugin(CoreTestsPlugin.getDefault(), CallGraphTestUtil.REGRESSION_EXCLUSIONS).getAbsolutePath());
    } catch (IOException e) {
      Assert.assertFalse("Cannot find exclusions file", true);
    }

    return engine;
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
