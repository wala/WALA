/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.cha;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

/**
 * Test interface subtype stuff
 */
public class InterfaceTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = InterfaceTest.class.getClassLoader();

  private static AnalysisScope scope;
  private static ClassHierarchy cha;

  public static void main(String[] args) {
    justThisTest(InterfaceTest.class);
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"), MY_CLASSLOADER);

    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions() );

    try {
      cha = ClassHierarchyFactory.make(scope, factory);
    } catch (ClassHierarchyException e) {
      throw new Exception(e);
    }
  }

  @AfterClass
  public static void afterClass() throws Exception {
    scope = null;
    cha = null;
  }


  /**
   * Test for subtype tests with interfaces; bug reported by Bruno Dufour
   */
  @Test public void test1() {
    TypeReference prep_stmt_type = TypeReference.findOrCreate(ClassLoaderReference.Primordial, TypeName
        .string2TypeName("Ljava/sql/PreparedStatement"));
    TypeReference stmt_type = TypeReference.findOrCreate(ClassLoaderReference.Primordial, TypeName
        .string2TypeName("Ljava/sql/Statement"));
    IClass prep_stmt = cha.lookupClass(prep_stmt_type);
    IClass stmt = cha.lookupClass(stmt_type);
    
    Assert.assertTrue("did not find PreparedStatement", prep_stmt != null);
    Assert.assertTrue("did not find Statement", stmt != null);
    
    Assert.assertTrue(cha.implementsInterface(prep_stmt, stmt));
    Assert.assertFalse(cha.implementsInterface(stmt, prep_stmt));
    Assert.assertTrue(cha.isAssignableFrom(stmt, prep_stmt));
    Assert.assertFalse(cha.isAssignableFrom(prep_stmt, stmt));
  }
  
  /**
   * check that arrays implement Cloneable and Serializable
   */
  @Test public void test2() {
    IClass objArrayClass = cha.lookupClass(TypeReference.JavaLangObject.getArrayTypeForElementType());
    IClass stringArrayClass = cha.lookupClass(TypeReference.JavaLangString.getArrayTypeForElementType());
    IClass cloneableClass = cha.lookupClass(TypeReference.JavaLangCloneable);
    IClass serializableClass = cha.lookupClass(TypeReference.JavaIoSerializable);
    
    Assert.assertTrue(cha.implementsInterface(objArrayClass, cloneableClass));
    Assert.assertTrue(cha.implementsInterface(objArrayClass, serializableClass));
    Assert.assertTrue(cha.implementsInterface(stringArrayClass, cloneableClass));
    Assert.assertTrue(cha.implementsInterface(stringArrayClass, serializableClass));
  }

}
