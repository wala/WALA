/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.cha;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaRuntimeException;
import org.junit.jupiter.api.Test;

public class CodeDeletedTest extends WalaTestCase {

  /**
   * Test handling of an invalid class where a non-abstract method has no code. We want to throw an
   * exception rather than crash.
   */
  @Test
  public void testDeletedCode() {
    assertThrows(
        WalaRuntimeException.class,
        () -> {
          AnalysisScope scope =
              AnalysisScopeReader.instance.readJavaScope(
                  TestConstants.WALA_TESTDATA,
                  new FileProvider().getFile("J2SEClassHierarchyExclusions.txt"),
                  DupFieldsTest.class.getClassLoader());
          ClassHierarchy cha = ClassHierarchyFactory.make(scope);
          TypeReference ref =
              TypeReference.findOrCreate(ClassLoaderReference.Application, "LCodeDeleted");
          IClass klass = cha.lookupClass(ref);
          IAnalysisCacheView cache = new AnalysisCacheImpl();
          for (IMethod m : klass.getDeclaredMethods()) {
            if (m.toString().contains("foo")) {
              // should throw WalaRuntimeException
              cache.getIR(m);
            }
          }
        });
  }
}
