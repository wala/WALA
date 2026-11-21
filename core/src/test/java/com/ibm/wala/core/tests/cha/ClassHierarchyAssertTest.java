package com.ibm.wala.core.tests.cha;

import static com.ibm.wala.ipa.cha.ClassHierarchyAssert.assertThat;
import static com.ibm.wala.ipa.cha.IClassHierarchyConditions.implementsInterface;
import static com.ibm.wala.ipa.cha.IClassHierarchyConditions.subclassOf;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ClassHierarchyAssertTest {

  private static ClassHierarchy cha;

  @BeforeAll
  static void beforeAll() throws Exception {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    cha = ClassHierarchyFactory.make(scope);
  }

  @Test
  void testsLookupsAndSubclasses() {
    IClass stringClass = cha.lookupClass(TypeReference.JavaLangString);
    IClass objectClass = cha.lookupClass(TypeReference.JavaLangObject);
    IClass threadClass = cha.lookupClass(TypeReference.JavaLangThread);

    assertThat(cha)
        .has(subclassOf(stringClass, objectClass))
        .doesNotHave(subclassOf(threadClass, stringClass))
        .has(implementsInterface(stringClass, cha.lookupClass(TypeReference.JavaIoSerializable)));

    assertThat(cha).lookupClass(TypeReference.JavaLangString).isNotNull();
    assertThat(cha).immediateSubclassesOf(objectClass).contains(threadClass);
  }

  @Test
  void rejectsNullClassHierarchy() {
    assertThatThrownBy(() -> assertThat(null).lookupClass(TypeReference.JavaLangObject))
        .isInstanceOf(AssertionError.class);
  }
}
