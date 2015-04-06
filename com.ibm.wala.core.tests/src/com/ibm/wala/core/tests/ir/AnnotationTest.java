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
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.BytecodeClass;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.WalaTestCase;
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
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.strings.Atom;

public abstract class AnnotationTest extends WalaTestCase {

  private final IClassHierarchy cha;
  
  protected AnnotationTest(IClassHierarchy cha) {
    this.cha = cha;
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
    Assert.assertTrue(classUnderTest instanceof BytecodeClass);
    BytecodeClass<?> bcClassUnderTest = (BytecodeClass<?>) classUnderTest;

    Collection<Annotation> runtimeInvisibleAnnotations = bcClassUnderTest.getAnnotations(true);
    assertEqualCollections(expectedRuntimeInvisibleAnnotations, runtimeInvisibleAnnotations);

    Collection<Annotation> runtimeVisibleAnnotations = bcClassUnderTest.getAnnotations(false);
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

  @SuppressWarnings("unchecked")
  @Test
  public void testClassAnnotations3() throws Exception {

    TypeReference typeRef = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/AnnotatedClass3");
    IClass klass = cha.lookupClass(typeRef);
    Assert.assertNotNull(klass);
    BytecodeClass<?> shrikeClass = (BytecodeClass<?>) klass;
    Collection<Annotation> classAnnotations = shrikeClass.getAnnotations(true);
    Assert.assertEquals("[Annotation type <Application,Lannotations/AnnotationWithParams> {strParam=classStrParam}]",
        classAnnotations.toString());

    MethodReference methodRefUnderTest = MethodReference.findOrCreate(typeRef, Selector.make("foo()V"));

    IMethod methodUnderTest = cha.resolveMethod(methodRefUnderTest);
    Assert.assertNotNull(methodRefUnderTest.toString() + " not found", methodUnderTest);
    Assert.assertTrue(methodUnderTest instanceof IBytecodeMethod);
    IBytecodeMethod bcMethodUnderTest = (IBytecodeMethod) methodUnderTest;

    Collection<Annotation> runtimeInvisibleAnnotations = bcMethodUnderTest.getAnnotations(true);
    Assert.assertEquals(1, runtimeInvisibleAnnotations.size());
    
    Annotation x = runtimeInvisibleAnnotations.iterator().next();
    Assert.assertEquals(TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/AnnotationWithParams"), x.getType());
    for(Pair<String,String> n : new Pair[]{Pair.make("enumParam", "EnumElementValue [type=Lannotations/AnnotationEnum;, val=VAL1]"),
                                           Pair.make("strArrParam", "ArrayElementValue [vals=[biz, boz]]"), 
                                           Pair.make("annotParam", "AnnotationElementValue [type=Lannotations/AnnotationWithSingleParam;, elementValues={value=sdfevs}]"),
                                           Pair.make("strParam", "sdfsevs"), 
                                           Pair.make("intParam", "25"), 
                                           Pair.make("klassParam", "Ljava/lang/Integer;")}) {
      Assert.assertEquals(n.snd, x.getNamedArguments().get(n.fst).toString());
    }
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
        new String[]{"Annotation type <Application,Lannotations/RuntimeVisableAnnotation>"});
    checkParameterAnnots(
        typeRef,
        "bar(Ljava/lang/Integer;)V",
        new String[]{"Annotation type <Application,Lannotations/AnnotationWithParams> {annotParam=AnnotationElementValue [type=Lannotations/AnnotationWithSingleParam;, elementValues={value=sdfevs}], enumParam=EnumElementValue [type=Lannotations/AnnotationEnum;, val=VAL1], intParam=25, klassParam=Ljava/lang/Integer;, strArrParam=ArrayElementValue [vals=[biz, boz]], strParam=sdfsevs}"});
    checkParameterAnnots(typeRef, "foo2(Ljava/lang/String;Ljava/lang/Integer;)V",
        new String[]{"Annotation type <Application,Lannotations/RuntimeVisableAnnotation>"},
        new String[]{"Annotation type <Application,Lannotations/RuntimeInvisableAnnotation>"});
    checkParameterAnnots(typeRef, "foo3(Ljava/lang/String;Ljava/lang/Integer;)V",
        new String[]{"Annotation type <Application,Lannotations/RuntimeVisableAnnotation>"},
        new String[]{"Annotation type <Application,Lannotations/RuntimeInvisableAnnotation>"});
    checkParameterAnnots(typeRef, "foo4(Ljava/lang/String;Ljava/lang/Integer;)V",
        new String[]{"Annotation type <Application,Lannotations/RuntimeInvisableAnnotation>", "Annotation type <Application,Lannotations/RuntimeVisableAnnotation>"},
        new String[0]);

  }

  protected void checkParameterAnnots(TypeReference typeRef, String selector, String[]... expected) {
    MethodReference methodRefUnderTest = MethodReference.findOrCreate(typeRef, Selector.make(selector));

    IMethod methodUnderTest = cha.resolveMethod(methodRefUnderTest);
    Assert.assertNotNull(methodRefUnderTest.toString() + " not found", methodUnderTest);
    Assert.assertTrue(methodUnderTest instanceof IBytecodeMethod);
    IBytecodeMethod IBytecodeMethodUnderTest = (IBytecodeMethod) methodUnderTest;

    Collection<Annotation>[] parameterAnnotations = IBytecodeMethodUnderTest.getParameterAnnotations();
    Assert.assertEquals(expected.length, parameterAnnotations.length);
    for (int i = 0; i < expected.length; i++) {
      Set<String> e = HashSetFactory.make();
      for(String s : expected[i]) {
        e.add(s);
      }
      
      Set<String> a = HashSetFactory.make();
      if (parameterAnnotations[i] != null) {
        for(Annotation x : parameterAnnotations[i]) {
          a.add(x.toString());
        }
      }
      
      Assert.assertEquals(e, a);
    }
  }

}
