/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.cha;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.BytecodeClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;

/**
 * A test of support for source file mapping
 */
public class SourceMapTest extends WalaTestCase {
  private static final ClassLoader MY_CLASSLOADER = SourceMapTest.class.getClassLoader();

  private final static String CLASS_IN_PRIMORDIAL_JAR = "Lcom/ibm/wala/model/SyntheticFactory";

  @Test public void testHello() throws ClassHierarchyException, IOException {
    if (analyzingJar()) return;
    AnalysisScope scope = null;
    scope = AnalysisScopeReader.readJavaScope(TestConstants.HELLO, null, MY_CLASSLOADER);
    // TODO: it's annoying to have to build a class hierarchy here.
    // see feature 38676
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    TypeReference t = TypeReference.findOrCreate(scope.getApplicationLoader(), TestConstants.HELLO_MAIN);
    IClass klass = cha.lookupClass(t);
    Assert.assertTrue("failed to load " + t, klass != null);
    String sourceFile = klass.getSourceFileName();
    System.err.println("Source file: " + sourceFile);
    Assert.assertTrue(sourceFile != null);
  }

  @Test public void testFromJar() throws ClassHierarchyException, IOException {
    if (analyzingJar()) return;
    AnalysisScope scope = null;
    scope = AnalysisScopeReader.readJavaScope(TestConstants.HELLO, null, MY_CLASSLOADER);
    // TODO: it's annoying to have to build a class hierarchy here.
    // open a feature to fix this
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    TypeReference t = TypeReference.findOrCreate(scope.getPrimordialLoader(), CLASS_IN_PRIMORDIAL_JAR);
    IClass klass = cha.lookupClass(t);
    Assert.assertTrue(klass != null);
    String sourceFile = klass.getSourceFileName();
    Assert.assertTrue(sourceFile != null);
    System.err.println("Source file: " + sourceFile);
    Module container = ((BytecodeClass<?>)klass).getContainer();
    Assert.assertTrue(container != null);
    System.err.println("container: " + container);
  }
}
