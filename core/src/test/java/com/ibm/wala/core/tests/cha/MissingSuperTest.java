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

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.PhantomClass;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import java.io.IOException;
import java.util.Collection;
import org.junit.jupiter.api.Test;

public class MissingSuperTest extends WalaTestCase {

  /**
   * Test handling of an invalid class where a non-abstract method has no code. We want to throw an
   * exception rather than crash.
   */
  @Test
  public void testMissingSuper() throws IOException, ClassHierarchyException {
    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA,
            new FileProvider().getFile("J2SEClassHierarchyExclusions.txt"),
            MissingSuperTest.class.getClassLoader());

    TypeReference ref =
        TypeReference.findOrCreate(ClassLoaderReference.Application, "Lmissingsuper/MissingSuper");

    // without phantom classes, won't be able to resolve
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    assertThat(cha.lookupClass(ref)).isNull();

    // with makeWithRoot lookup should succeed and
    // unresolvable super class "Super" should be replaced by hierarchy root
    cha = ClassHierarchyFactory.makeWithRoot(scope);
    IClass klass = cha.lookupClass(ref);
    assertThat(klass).isNotNull().extracting(IClass::getSuperclass).isEqualTo(cha.getRootClass());

    // with phantom classes, lookup and IR construction should work
    cha = ClassHierarchyFactory.makeWithPhantom(scope);
    klass = cha.lookupClass(ref);
    assertThat(klass).isNotNull();
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    Collection<? extends IMethod> declaredMethods = klass.getDeclaredMethods();
    assertThat(declaredMethods).as(declaredMethods::toString).hasSize(2);
    for (IMethod m : declaredMethods) {
      // should succeed
      cache.getIR(m);
    }
    // there should be one PhantomClass in the Application class loader
    boolean found = false;
    for (IClass klass2 : cha) {
      if (klass2 instanceof PhantomClass
          && klass2.getReference().getClassLoader().equals(ClassLoaderReference.Application)) {
        assertThat(klass2.getReference().getName()).hasToString("Lmissingsuper/Super");
        found = true;
      }
    }
    assertThat(found).isTrue();
  }
}
