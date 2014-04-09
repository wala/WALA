/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.ir;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

public class AnnotationTest extends WalaTestCase {

  public static void main(String[] args) {
    justThisTest(AnnotationTest.class);
  }

  private static IClassHierarchy cha;
  
  @BeforeClass
  public static void before() throws IOException, ClassHierarchyException {
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA,
        (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS), AnnotationTest.class.getClassLoader());
    cha = ClassHierarchy.make(scope);    
  }
  
  @AfterClass
  public static void after() {
    cha = null;
    
  }
  @Test
  public void testClassAnnotations1() throws Exception {
    TypeReference typeUnderTest = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/AnnotatedClass1");

    Collection<Annotation> expectedRuntimeInvisibleAnnotations = HashSetFactory.make();
    expectedRuntimeInvisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application,
        "Lannotations/RuntimeInvisableAnnotation")));
    expectedRuntimeInvisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application,
        "Lannotations/DefaultVisableAnnotation")));

    Collection<Annotation> expectedRuntimeVisibleAnnotations = HashSetFactory.make();
    expectedRuntimeVisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application,
        "Lannotations/RuntimeVisableAnnotation")));

    testClassAnnotations(typeUnderTest, expectedRuntimeInvisibleAnnotations, expectedRuntimeVisibleAnnotations);
  }

  @Test
  public void testClassAnnotations2() throws Exception {
    TypeReference typeUnderTest = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/AnnotatedClass2");

    Collection<Annotation> expectedRuntimeInvisibleAnnotations = HashSetFactory.make();
    expectedRuntimeInvisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application,
        "Lannotations/RuntimeInvisableAnnotation")));
    expectedRuntimeInvisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application,
        "Lannotations/RuntimeInvisableAnnotation2")));

    Collection<Annotation> expectedRuntimeVisibleAnnotations = HashSetFactory.make();
    expectedRuntimeVisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application,
        "Lannotations/RuntimeVisableAnnotation")));
    expectedRuntimeVisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application,
        "Lannotations/RuntimeVisableAnnotation2")));

    testClassAnnotations(typeUnderTest, expectedRuntimeInvisibleAnnotations, expectedRuntimeVisibleAnnotations);
  }

  private void testClassAnnotations(TypeReference typeUnderTest, Collection<Annotation> expectedRuntimeInvisibleAnnotations,
      Collection<Annotation> expectedRuntimeVisibleAnnotations) throws IOException, ClassHierarchyException,
      InvalidClassFileException {
    IClass classUnderTest = cha.lookupClass(typeUnderTest);
    Assert.assertNotNull(typeUnderTest.toString() + " not found", classUnderTest);
    Assert.assertTrue(classUnderTest instanceof ShrikeClass);
    ShrikeClass shrikeClassUnderTest = (ShrikeClass) classUnderTest;

    Collection<Annotation> runtimeInvisibleAnnotations = shrikeClassUnderTest.getRuntimeInvisibleAnnotations();
    assertEqualCollections(expectedRuntimeInvisibleAnnotations, runtimeInvisibleAnnotations);

    Collection<Annotation> runtimeVisibleAnnotations = shrikeClassUnderTest.getRuntimeVisibleAnnotations();
    assertEqualCollections(expectedRuntimeVisibleAnnotations, runtimeVisibleAnnotations);
  }

  private <T> void assertEqualCollections(Collection<T> expected, Collection<T> actual) {
    if (expected == null) {
      expected = Collections.emptySet();
    }
    if (actual == null) {
      actual = Collections.emptySet();
    }

    if (expected.size() != actual.size()) {
      Assert.assertTrue("expected=" + expected + " actual=" + actual, false);
    }
    for (T a : expected) {
      Assert.assertTrue("missing " + a.toString(), actual.contains(a));
    }
  }

  @Test
  public void testClassAnnotations3() throws Exception {

    TypeReference typeRef = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/AnnotatedClass3");
    IClass klass = cha.lookupClass(typeRef);
    Assert.assertNotNull(klass);
    ShrikeClass shrikeClass = (ShrikeClass) klass;
    Collection<Annotation> classAnnotations = shrikeClass.getAnnotations(true);
    Assert.assertEquals("[Annotation type <Application,Lannotations/AnnotationWithParams> {strParam=classStrParam}]",
        classAnnotations.toString());

    MethodReference methodRefUnderTest = MethodReference.findOrCreate(typeRef, Selector.make("foo()V"));

    IMethod methodUnderTest = cha.resolveMethod(methodRefUnderTest);
    Assert.assertNotNull(methodRefUnderTest.toString() + " not found", methodUnderTest);
    Assert.assertTrue(methodUnderTest instanceof ShrikeCTMethod);
    ShrikeCTMethod shrikeCTMethodUnderTest = (ShrikeCTMethod) methodUnderTest;

    Collection<Annotation> runtimeInvisibleAnnotations = shrikeCTMethodUnderTest.getAnnotations(true);
    Assert
        .assertEquals(
            "[Annotation type <Application,Lannotations/AnnotationWithParams> {enumParam=EnumElementValue [type=Lannotations/AnnotationEnum;, val=VAL1], strArrParam=ArrayElementValue [vals=[biz, boz]], annotParam=AnnotationElementValue [type=Lannotations/AnnotationWithSingleParam;, elementValues={value=sdfevs}], strParam=sdfsevs, intParam=25, klassParam=Ljava/lang/Integer;}]",
            runtimeInvisibleAnnotations.toString());

  }
  
  @Test
  public void testClassAnnotations4() throws Exception {

    TypeReference typeRef = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/AnnotatedClass4");
    FieldReference fieldRefUnderTest = FieldReference.findOrCreate(typeRef, Atom.findOrCreateUnicodeAtom("foo"), TypeReference.Int); 

    IField fieldUnderTest = cha.resolveField(fieldRefUnderTest);
    Assert.assertNotNull(fieldRefUnderTest.toString() + " not found", fieldUnderTest);

    Collection<Annotation> annots = fieldUnderTest.getAnnotations();
    Assert
        .assertEquals(
            "[Annotation type <Application,Lannotations/RuntimeInvisableAnnotation>, Annotation type <Application,Lannotations/RuntimeVisableAnnotation>]",
            annots.toString());

  }
  
  @Test
  public void testParamAnnotations1() throws Exception {

    TypeReference typeRef = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/ParameterAnnotations1");

    checkParameterAnnots(typeRef, "foo(Ljava/lang/String;)V",
        "[Annotation type <Application,Lannotations/RuntimeVisableAnnotation>]");
    checkParameterAnnots(
        typeRef,
        "bar(Ljava/lang/Integer;)V",
        "[Annotation type <Application,Lannotations/AnnotationWithParams> {enumParam=EnumElementValue [type=Lannotations/AnnotationEnum;, val=VAL1], strArrParam=ArrayElementValue [vals=[biz, boz]], annotParam=AnnotationElementValue [type=Lannotations/AnnotationWithSingleParam;, elementValues={value=sdfevs}], strParam=sdfsevs, intParam=25, klassParam=Ljava/lang/Integer;}]");
    checkParameterAnnots(typeRef, "foo2(Ljava/lang/String;Ljava/lang/Integer;)V",
        "[Annotation type <Application,Lannotations/RuntimeVisableAnnotation>]",
        "[Annotation type <Application,Lannotations/RuntimeInvisableAnnotation>]");
    checkParameterAnnots(typeRef, "foo3(Ljava/lang/String;Ljava/lang/Integer;)V",
        "[Annotation type <Application,Lannotations/RuntimeVisableAnnotation>]",
        "[Annotation type <Application,Lannotations/RuntimeInvisableAnnotation>]");
    checkParameterAnnots(typeRef, "foo4(Ljava/lang/String;Ljava/lang/Integer;)V",
        "[Annotation type <Application,Lannotations/RuntimeInvisableAnnotation>, Annotation type <Application,Lannotations/RuntimeVisableAnnotation>]",
        "[]");

  }

  protected void checkParameterAnnots(TypeReference typeRef, String selector, String... expected) {
    MethodReference methodRefUnderTest = MethodReference.findOrCreate(typeRef, Selector.make(selector));

    IMethod methodUnderTest = cha.resolveMethod(methodRefUnderTest);
    Assert.assertNotNull(methodRefUnderTest.toString() + " not found", methodUnderTest);
    Assert.assertTrue(methodUnderTest instanceof ShrikeCTMethod);
    ShrikeCTMethod shrikeCTMethodUnderTest = (ShrikeCTMethod) methodUnderTest;

    Collection<Annotation>[] parameterAnnotations = shrikeCTMethodUnderTest.getParameterAnnotations();
    Assert.assertEquals(expected.length, parameterAnnotations.length);
    for (int i = 0; i < expected.length; i++) {
      Assert.assertEquals(expected[i], parameterAnnotations[i].toString());
    }
  }

}
