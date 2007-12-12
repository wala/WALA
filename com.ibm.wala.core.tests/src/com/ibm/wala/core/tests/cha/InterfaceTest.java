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

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

/**
 * Test interface subtype stuff
 */
public class InterfaceTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = InterfaceTest.class.getClassLoader();

  private AnalysisScope scope;
  private ClassHierarchy cha;

  public static void main(String[] args) {
    justThisTest(InterfaceTest.class);
  }

  protected void setUp() throws Exception {
    scope = AnalysisScopeReader.read(TestConstants.WALA_TESTDATA, "J2SEClassHierarchyExclusions.xml", MY_CLASSLOADER);

    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions() );

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


  /**
   * Test for subtype tests with interfaces; bug reported by Bruno Dufour
   */
  public void test1() {
    TypeReference prep_stmt_type = TypeReference.findOrCreate(ClassLoaderReference.Primordial, TypeName
        .string2TypeName("Ljava/sql/PreparedStatement"));
    TypeReference stmt_type = TypeReference.findOrCreate(ClassLoaderReference.Primordial, TypeName
        .string2TypeName("Ljava/sql/Statement"));
    IClass prep_stmt = cha.lookupClass(prep_stmt_type);
    IClass stmt = cha.lookupClass(stmt_type);
    
    assertTrue("did not find PreparedStatement", prep_stmt != null);
    assertTrue("did not find Statement", stmt != null);
    
    assertTrue(cha.implementsInterface(prep_stmt, stmt));
    assertFalse(cha.implementsInterface(stmt, prep_stmt));
    assertTrue(cha.isAssignableFrom(stmt, prep_stmt));
    assertFalse(cha.isAssignableFrom(prep_stmt, stmt));
  }

}
