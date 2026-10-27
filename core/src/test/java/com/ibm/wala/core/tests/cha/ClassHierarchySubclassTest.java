package com.ibm.wala.core.tests.cha;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Tests that {@link ClassHierarchy} can be subclassed outside its package. */
public class ClassHierarchySubclassTest extends WalaTestCase {

  /** Treats {@code java.lang.String} as a subclass of {@code java.lang.Thread}. */
  private static class RefinedClassHierarchy extends ClassHierarchy {

    RefinedClassHierarchy(AnalysisScope scope, ClassLoaderFactory factory)
        throws ClassHierarchyException {
      super(scope, factory, null, MissingSuperClassHandling.NONE);
    }

    RefinedClassHierarchy(AnalysisScope scope, ClassLoaderFactory factory, Language language)
        throws ClassHierarchyException {
      super(scope, factory, language, null, MissingSuperClassHandling.NONE);
    }

    RefinedClassHierarchy(
        AnalysisScope scope, ClassLoaderFactory factory, Collection<Language> languages)
        throws ClassHierarchyException {
      super(scope, factory, languages, null, MissingSuperClassHandling.NONE);
    }

    @Override
    public boolean isSubclassOf(IClass c, IClass t) {
      return (c.getReference().equals(TypeReference.JavaLangString)
              && t.getReference().equals(TypeReference.JavaLangThread))
          || super.isSubclassOf(c, t);
    }
  }

  private static AnalysisScope makeScope() throws IOException {
    return AnalysisScopeReader.instance.readJavaScope(
        TestConstants.WALA_TESTDATA,
        new FileProvider().getFile("J2SEClassHierarchyExclusions.txt"),
        ClassHierarchySubclassTest.class.getClassLoader());
  }

  private static void assertRefined(ClassHierarchy cha) {
    IClass string = cha.lookupClass(TypeReference.JavaLangString);
    IClass thread = cha.lookupClass(TypeReference.JavaLangThread);
    IClass object = cha.lookupClass(TypeReference.JavaLangObject);
    assertThat(cha.isSubclassOf(string, thread)).isTrue();
    assertThat(cha.isSubclassOf(string, object)).isTrue();
    assertThat(cha.isSubclassOf(thread, string)).isFalse();
  }

  @Test
  public void testSubclassForScopeLanguages() throws IOException, ClassHierarchyException {
    AnalysisScope scope = makeScope();
    ClassHierarchy cha =
        new RefinedClassHierarchy(scope, new ClassLoaderFactoryImpl(scope.getExclusions()));
    assertThat(cha.getNumberOfClasses()).isPositive();
    assertRefined(cha);
  }

  @Test
  public void testSubclassForLanguage() throws IOException, ClassHierarchyException {
    AnalysisScope scope = makeScope();
    ClassHierarchy cha =
        new RefinedClassHierarchy(
            scope, new ClassLoaderFactoryImpl(scope.getExclusions()), JavaLanguage.get());
    assertThat(cha.getLoader(ClassLoaderReference.Application)).isNotNull();
    assertRefined(cha);
  }

  @Test
  public void testSubclassForLanguages() throws IOException, ClassHierarchyException {
    AnalysisScope scope = makeScope();
    ClassHierarchy cha =
        new RefinedClassHierarchy(
            scope,
            new ClassLoaderFactoryImpl(scope.getExclusions()),
            Collections.singleton(JavaLanguage.get()));
    assertRefined(cha);
  }
}
