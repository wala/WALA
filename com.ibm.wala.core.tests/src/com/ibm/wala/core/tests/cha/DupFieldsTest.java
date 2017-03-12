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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

public class DupFieldsTest extends WalaTestCase {

  @Test public void testDupFieldNames() throws IOException, ClassHierarchyException {
    AnalysisScope scope = null;
    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"), DupFieldsTest.class.getClassLoader());
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    TypeReference ref = TypeReference.findOrCreate(ClassLoaderReference.Application, "LDupFieldName");
    IClass klass = cha.lookupClass(ref);
    boolean threwException = false;
    try {
      klass.getField(Atom.findOrCreateUnicodeAtom("a"));
    } catch (IllegalStateException e) {
      threwException = true;
    }
    Assert.assertTrue(threwException);    
    IField f = cha.resolveField(FieldReference.findOrCreate(ref, Atom.findOrCreateUnicodeAtom("a"), TypeReference.Int));
    Assert.assertEquals(f.getFieldTypeReference(), TypeReference.Int);
    f = cha.resolveField(FieldReference.findOrCreate(ref, Atom.findOrCreateUnicodeAtom("a"), TypeReference.Boolean));
    Assert.assertEquals(f.getFieldTypeReference(), TypeReference.Boolean);
  }
}
