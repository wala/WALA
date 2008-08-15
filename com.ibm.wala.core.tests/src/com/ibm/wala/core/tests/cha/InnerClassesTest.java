/*******************************************************************************
 * Copyright (c) 20078 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.cha;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

/**
 * Test stuff with inner classes
 */
public class InnerClassesTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = InnerClassesTest.class.getClassLoader();

  private AnalysisScope scope;

  private ClassHierarchy cha;

  public static void main(String[] args) {
    justThisTest(InnerClassesTest.class);
  }

  protected void setUp() throws Exception {
    scope = AnalysisScopeReader.read(TestConstants.WALA_TESTDATA, FileProvider.getFile("J2SEClassHierarchyExclusions.txt"),
        MY_CLASSLOADER);

    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions());

    try {
      cha = ClassHierarchy.make(scope, factory);
    } catch (ClassHierarchyException e) {
      throw new Exception();
    }
  }

  protected void tearDown() throws Exception {
    scope = null;
    cha = null;
    super.tearDown();
  }

  public void test1() throws InvalidClassFileException {
    TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName
        .string2TypeName("Linner/TestStaticInner"));
    IClass klass = cha.lookupClass(t);
    assert klass != null;
    ShrikeClass s = (ShrikeClass)klass;
    assertFalse(s.isInnerClass());
  }
  
  public void test2() throws InvalidClassFileException {
    TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName
        .string2TypeName("Linner/TestStaticInner$A"));
    IClass klass = cha.lookupClass(t);
    assert klass != null;
    ShrikeClass s = (ShrikeClass)klass;
    assertTrue(s.isInnerClass());
    assertTrue(s.isStaticInnerClass());
  }
  
  public void test3() throws InvalidClassFileException {
    TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName
        .string2TypeName("Linner/TestInner$A"));
    IClass klass = cha.lookupClass(t);
    assert klass != null;
    ShrikeClass s = (ShrikeClass)klass;
    assertTrue(s.isInnerClass());
    assertFalse(s.isStaticInnerClass());
  }

}
