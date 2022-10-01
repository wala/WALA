/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.ir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ibm.wala.classLoader.BytecodeClass;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public abstract class AnnotationTest extends WalaTestCase {

  private final IClassHierarchy cha;

  /**
   * Should we only check for annotations whose retention is {@link
   * java.lang.annotation.RetentionPolicy#RUNTIME}? Useful for d8 on Android, which strips
   * everything but runtime retention annotations
   */
  private final boolean checkRuntimeRetentionOnly;

  protected AnnotationTest(IClassHierarchy cha, boolean checkRuntimeRetentionOnly) {
    this.cha = cha;
    this.checkRuntimeRetentionOnly = checkRuntimeRetentionOnly;
  }

  public AnnotationTest(boolean checkRuntimeRetentionOnly)
      throws ClassHierarchyException, IOException {
    this(makeCHA(), checkRuntimeRetentionOnly);
  }

  public AnnotationTest() throws ClassHierarchyException, IOException {
    this(false);
  }

  public static IClassHierarchy makeCHA() throws IOException, ClassHierarchyException {
    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA,
            new FileProvider().getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS),
            AnnotationTest.class.getClassLoader());
    return ClassHierarchyFactory.make(scope);
  }

  public static <T> void assertEqualCollections(Collection<T> expected, Collection<T> actual) {
    if (expected == null) {
      expected = Collections.emptySet();
    }
    if (actual == null) {
      actual = Collections.emptySet();
    }

    if (expected.size() != actual.size()) {
      fail("expected=" + expected + " actual=" + actual);
    }
    for (T a : expected) {
      assertTrue("missing " + a.toString(), actual.contains(a));
    }
  }

  @Test
  public void testClassAnnotations1() throws Exception {
    TypeReference typeUnderTest =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application, "Lannotations/AnnotatedClass1");

    Collection<Annotation> expectedRuntimeInvisibleAnnotations = HashSetFactory.make();
    expectedRuntimeInvisibleAnnotations.add(
        Annotation.make(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Lannotations/RuntimeInvisableAnnotation")));
    expectedRuntimeInvisibleAnnotations.add(
        Annotation.make(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Lannotations/DefaultVisableAnnotation")));

    Collection<Annotation> expectedRuntimeVisibleAnnotations = HashSetFactory.make();
    expectedRuntimeVisibleAnnotations.add(
        Annotation.make(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Lannotations/RuntimeVisableAnnotation")));

    testClassAnnotations(
        typeUnderTest, expectedRuntimeInvisibleAnnotations, expectedRuntimeVisibleAnnotations);
  }

  @Test
  public void testClassAnnotations2() throws Exception {
    TypeReference typeUnderTest =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application, "Lannotations/AnnotatedClass2");

    Collection<Annotation> expectedRuntimeInvisibleAnnotations = HashSetFactory.make();
    expectedRuntimeInvisibleAnnotations.add(
        Annotation.make(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Lannotations/RuntimeInvisableAnnotation")));
    expectedRuntimeInvisibleAnnotations.add(
        Annotation.make(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Lannotations/RuntimeInvisableAnnotation2")));

    Collection<Annotation> expectedRuntimeVisibleAnnotations = HashSetFactory.make();
    expectedRuntimeVisibleAnnotations.add(
        Annotation.make(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Lannotations/RuntimeVisableAnnotation")));
    expectedRuntimeVisibleAnnotations.add(
        Annotation.make(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Lannotations/RuntimeVisableAnnotation2")));

    testClassAnnotations(
        typeUnderTest, expectedRuntimeInvisibleAnnotations, expectedRuntimeVisibleAnnotations);
  }

  private void testClassAnnotations(
      TypeReference typeUnderTest,
      Collection<Annotation> expectedRuntimeInvisibleAnnotations,
      Collection<Annotation> expectedRuntimeVisibleAnnotations)
      throws InvalidClassFileException {
    IClass classUnderTest = cha.lookupClass(typeUnderTest);
    assertNotNull(typeUnderTest.toString() + " not found", classUnderTest);
    assertTrue(classUnderTest + " must be BytecodeClass", classUnderTest instanceof BytecodeClass);
    BytecodeClass<?> bcClassUnderTest = (BytecodeClass<?>) classUnderTest;

    Collection<Annotation> runtimeInvisibleAnnotations = bcClassUnderTest.getAnnotations(true);
    Collection<Annotation> runtimeVisibleAnnotations = bcClassUnderTest.getAnnotations(false);
    if (!checkRuntimeRetentionOnly) {
      assertEqualCollections(expectedRuntimeInvisibleAnnotations, runtimeInvisibleAnnotations);
    }
    assertEqualCollections(expectedRuntimeVisibleAnnotations, runtimeVisibleAnnotations);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testClassAnnotations3() throws Exception {

    TypeReference typeRef =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application, "Lannotations/AnnotatedClass3");
    IClass klass = cha.lookupClass(typeRef);
    assertNotNull(typeRef + " must exist", klass);
    BytecodeClass<?> shrikeClass = (BytecodeClass<?>) klass;
    Collection<Annotation> classAnnotations = shrikeClass.getAnnotations(false);
    assertEquals(
        "[Annotation type <Application,Lannotations/AnnotationWithParams> {strParam=classStrParam}]",
        classAnnotations.toString());

    MethodReference methodRefUnderTest =
        MethodReference.findOrCreate(typeRef, Selector.make("foo()V"));

    IMethod methodUnderTest = cha.resolveMethod(methodRefUnderTest);
    assertNotNull(methodRefUnderTest + " not found", methodUnderTest);
    assertTrue(
        methodUnderTest + " must be IBytecodeMethod", methodUnderTest instanceof IBytecodeMethod);
    IBytecodeMethod<IInstruction> bcMethodUnderTest =
        (IBytecodeMethod<IInstruction>) methodUnderTest;

    Collection<Annotation> runtimeVisibleAnnotations = bcMethodUnderTest.getAnnotations(false);
    assertEquals(1, runtimeVisibleAnnotations.size());

    Annotation x = runtimeVisibleAnnotations.iterator().next();
    assertEquals(
        TypeReference.findOrCreate(
            ClassLoaderReference.Application, "Lannotations/AnnotationWithParams"),
        x.getType());
    for (Pair<String, String> n :
        new Pair[] {
          Pair.make("enumParam", "EnumElementValue [type=Lannotations/AnnotationEnum;, val=VAL1]"),
          Pair.make("strArrParam", "ArrayElementValue [vals=[biz, boz]]"),
          Pair.make(
              "annotParam",
              "AnnotationElementValue [type=Lannotations/AnnotationWithSingleParam;, elementValues={value=sdfevs}]"),
          Pair.make("strParam", "sdfsevs"),
          Pair.make("intParam", "25"),
          Pair.make("klassParam", "Ljava/lang/Integer;")
        }) {
      assertEquals(n.snd, x.getNamedArguments().get(n.fst).toString());
    }
  }

  @Test
  public void testClassAnnotations4() throws Exception {

    TypeReference typeRef =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application, "Lannotations/AnnotatedClass4");
    FieldReference fieldRefUnderTest =
        FieldReference.findOrCreate(
            typeRef, Atom.findOrCreateUnicodeAtom("foo"), TypeReference.Int);

    IField fieldUnderTest = cha.resolveField(fieldRefUnderTest);
    assertNotNull(fieldRefUnderTest + " not found", fieldUnderTest);

    Collection<Annotation> annots = fieldUnderTest.getAnnotations();
    Collection<Annotation> expectedAnnotations = HashSetFactory.make();
    expectedAnnotations.add(
        Annotation.make(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Lannotations/RuntimeVisableAnnotation")));
    if (!checkRuntimeRetentionOnly) {
      expectedAnnotations.add(
          Annotation.make(
              TypeReference.findOrCreate(
                  ClassLoaderReference.Application, "Lannotations/RuntimeInvisableAnnotation")));
    }
    assertEqualCollections(expectedAnnotations, annots);
  }

  @Test
  public void testParamAnnotations1() throws Exception {

    TypeReference typeRef =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application, "Lannotations/ParameterAnnotations1");

    checkParameterAnnots(
        typeRef,
        "foo(Ljava/lang/String;)V",
        new String[] {"Annotation type <Application,Lannotations/RuntimeVisableAnnotation>"});
    checkParameterAnnots(
        typeRef,
        "bar(Ljava/lang/Integer;)V",
        new String[] {
          "Annotation type <Application,Lannotations/AnnotationWithParams> {annotParam=AnnotationElementValue [type=Lannotations/AnnotationWithSingleParam;, elementValues={value=sdfevs}], enumParam=EnumElementValue [type=Lannotations/AnnotationEnum;, val=VAL1], intParam=25, klassParam=Ljava/lang/Integer;, strArrParam=ArrayElementValue [vals=[biz, boz]], strParam=sdfsevs}"
        });
    checkParameterAnnots(
        typeRef,
        "foo2(Ljava/lang/String;Ljava/lang/Integer;)V",
        new String[] {"Annotation type <Application,Lannotations/RuntimeVisableAnnotation>"},
        checkRuntimeRetentionOnly
            ? new String[] {}
            : new String[] {
              "Annotation type <Application,Lannotations/RuntimeInvisableAnnotation>"
            });
    checkParameterAnnots(
        typeRef,
        "foo3(Ljava/lang/String;Ljava/lang/Integer;)V",
        new String[] {"Annotation type <Application,Lannotations/RuntimeVisableAnnotation>"},
        checkRuntimeRetentionOnly
            ? new String[] {}
            : new String[] {
              "Annotation type <Application,Lannotations/RuntimeInvisableAnnotation>"
            });
    checkParameterAnnots(
        typeRef,
        "foo4(Ljava/lang/String;Ljava/lang/Integer;)V",
        checkRuntimeRetentionOnly
            ? new String[] {"Annotation type <Application,Lannotations/RuntimeVisableAnnotation>"}
            : new String[] {
              "Annotation type <Application,Lannotations/RuntimeInvisableAnnotation>",
              "Annotation type <Application,Lannotations/RuntimeVisableAnnotation>"
            },
        new String[0]);
  }

  protected void checkParameterAnnots(
      TypeReference typeRef, String selector, String[]... expected) {
    MethodReference methodRefUnderTest =
        MethodReference.findOrCreate(typeRef, Selector.make(selector));

    IMethod methodUnderTest = cha.resolveMethod(methodRefUnderTest);
    assertNotNull(methodRefUnderTest + " not found", methodUnderTest);
    assertTrue(
        methodUnderTest + " must be bytecode method", methodUnderTest instanceof IBytecodeMethod);
    IBytecodeMethod<?> IBytecodeMethodUnderTest = (IBytecodeMethod<?>) methodUnderTest;

    Collection<Annotation>[] parameterAnnotations =
        IBytecodeMethodUnderTest.getParameterAnnotations();
    assertEquals(expected.length, parameterAnnotations.length);
    for (int i = 0; i < expected.length; i++) {
      Set<String> e = HashSetFactory.make();
      e.addAll(Arrays.asList(expected[i]));

      Set<String> a = HashSetFactory.make();
      if (parameterAnnotations[i] != null) {
        for (Annotation x : parameterAnnotations[i]) {
          a.add(x.toString());
        }
      }

      assertEquals(e + " must be " + a, e, a);
    }
  }
}
