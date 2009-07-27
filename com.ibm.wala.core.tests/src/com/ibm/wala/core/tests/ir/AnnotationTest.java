package com.ibm.wala.core.tests.ir;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

public class AnnotationTest extends WalaTestCase {

  public static void main(String[] args) {
    justThisTest(AnnotationTest.class);
  }

  @Test public void testClassAnnotations1() throws Exception {
    TypeReference typeUnderTest = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/AnnotatedClass1");

    Collection<Annotation> expectedRuntimeInvisibleAnnotations = HashSetFactory.make();
    expectedRuntimeInvisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/RuntimeInvisableAnnotation")));
    expectedRuntimeInvisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/DefaultVisableAnnotation")));

    Collection<Annotation> expectedRuntimeVisibleAnnotations = HashSetFactory.make();
    expectedRuntimeVisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/RuntimeVisableAnnotation")));
    
    testClassAnnontations(typeUnderTest, expectedRuntimeInvisibleAnnotations, expectedRuntimeVisibleAnnotations);
  }

  @Test public void testClassAnnotations2() throws Exception {
    TypeReference typeUnderTest = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/AnnotatedClass2");

    Collection<Annotation> expectedRuntimeInvisibleAnnotations = HashSetFactory.make();
    expectedRuntimeInvisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/RuntimeInvisableAnnotation")));
    expectedRuntimeInvisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/RuntimeInvisableAnnotation2")));

    Collection<Annotation> expectedRuntimeVisibleAnnotations = HashSetFactory.make();
    expectedRuntimeVisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/RuntimeVisableAnnotation")));
    expectedRuntimeVisibleAnnotations.add(Annotation.make(TypeReference.findOrCreate(ClassLoaderReference.Application, "Lannotations/RuntimeVisableAnnotation2")));
    
    testClassAnnontations(typeUnderTest, expectedRuntimeInvisibleAnnotations, expectedRuntimeVisibleAnnotations);
  }

  private void testClassAnnontations(TypeReference typeUnderTest, Collection<Annotation> expectedRuntimeInvisibleAnnotations,
      Collection<Annotation> expectedRuntimeVisibleAnnotations) throws IOException, ClassHierarchyException,
      InvalidClassFileException, com.ibm.wala.shrikeCT.AnnotationsReader.UnimplementedException {
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, FileProvider.getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS), getClass().getClassLoader());
    ClassHierarchy cha = ClassHierarchy.make(scope);

    IClass classUnderTest = cha.lookupClass(typeUnderTest);
    Assert.assertNotNull(typeUnderTest.toString() + " not found", classUnderTest);
    Assert.assertTrue(classUnderTest instanceof ShrikeClass);
    ShrikeClass shrikeClassUnderTest = (ShrikeClass) classUnderTest;

    Collection<Annotation> runtimeInvisibleAnnotations = shrikeClassUnderTest.getRuntimeInvisibleAnnotations();
    assertEqualCollections(expectedRuntimeInvisibleAnnotations, runtimeInvisibleAnnotations);

    Collection<Annotation> runtimeVisibleAnnotations = shrikeClassUnderTest.getRuntimeVisibleAnnotations();
    assertEqualCollections(expectedRuntimeVisibleAnnotations, runtimeVisibleAnnotations);
  }

  private <T> void assertEqualCollections(Collection<T> expected,
      Collection<T> actual) {
    if (expected == null){
      expected = Collections.emptySet();
    }
    if (actual == null){
      actual = Collections.emptySet();
    }
    
    if (expected.size() != actual.size()){
      Assert.assertTrue("expected=" + expected + " actual=" + actual, false);
    }
    for (T a : expected){
      Assert.assertTrue ("missing " + a.toString(), actual.contains(a));
    }
  }
//  String methodSig = "annotations.AnnotatedClass1.m1()V;";
//  MethodReference mr = StringStuff.makeMethodReference(methodSig );
//
}
