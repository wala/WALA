/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
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
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

public class StaticInnerTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = StaticInnerTest.class.getClassLoader();

  private AnalysisScope scope;
  private ClassHierarchy cha;

  public static void main(String[] args) {
    justThisTest(StaticInnerTest.class);
  }

  protected void setUp() throws Exception {

    scope = AnalysisScopeReader.read(TestConstants.WALA_TESTDATA, FileProvider.getFile("J2SEClassHierarchyExclusions.txt"), MY_CLASSLOADER);

    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions() );

    try {
      cha = ClassHierarchy.make(scope, factory);
    } catch (ClassHierarchyException e) {
      throw new Exception();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    scope = null;
    cha = null;
    super.tearDown();
  }

  public void testStaticInner() {
    IClass klass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, "Linner/TestStaticInner$A"));
    //assertTrue(klass.isStatic());
  }

}
