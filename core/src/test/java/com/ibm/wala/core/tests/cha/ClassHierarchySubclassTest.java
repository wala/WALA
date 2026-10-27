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

    /**
     * Builds a refined hierarchy for all languages in the given scope.
     *
     * @param scope the analysis scope whose classes populate the hierarchy
     * @param factory the factory that creates the class loaders for {@code scope}
     * @throws ClassHierarchyException if the hierarchy cannot be built
     */
    RefinedClassHierarchy(AnalysisScope scope, ClassLoaderFactory factory)
        throws ClassHierarchyException {
      super(scope, factory, null, MissingSuperClassHandling.NONE);
    }

    /**
     * Builds a refined hierarchy for the given language.
     *
     * @param scope the analysis scope whose classes populate the hierarchy
     * @param factory the factory that creates the class loaders for {@code scope}
     * @param language the language whose classes the hierarchy contains
     * @throws ClassHierarchyException if the hierarchy cannot be built
     */
    RefinedClassHierarchy(AnalysisScope scope, ClassLoaderFactory factory, Language language)
        throws ClassHierarchyException {
      super(scope, factory, language, null, MissingSuperClassHandling.NONE);
    }

    /**
     * Builds a refined hierarchy for the given languages.
     *
     * @param scope the analysis scope whose classes populate the hierarchy
     * @param factory the factory that creates the class loaders for {@code scope}
     * @param languages the languages whose classes the hierarchy contains
     * @throws ClassHierarchyException if the hierarchy cannot be built
     */
    RefinedClassHierarchy(
        AnalysisScope scope, ClassLoaderFactory factory, Collection<Language> languages)
        throws ClassHierarchyException {
      super(scope, factory, languages, null, MissingSuperClassHandling.NONE);
    }

    /**
     * Also reports {@code java.lang.String} as a subclass of {@code java.lang.Thread}, and
     * otherwise defers to {@link ClassHierarchy#isSubclassOf(IClass, IClass)}.
     */
    @Override
    public boolean isSubclassOf(IClass c, IClass t) {
      return (c.getReference().equals(TypeReference.JavaLangString)
              && t.getReference().equals(TypeReference.JavaLangThread))
          || super.isSubclassOf(c, t);
    }
  }

  /**
   * Reads the Java analysis scope for the WALA test data.
   *
   * @return the analysis scope
   * @throws IOException if the scope or its exclusions file cannot be read
   */
  private static AnalysisScope makeScope() throws IOException {
    return AnalysisScopeReader.instance.readJavaScope(
        TestConstants.WALA_TESTDATA,
        new FileProvider().getFile("J2SEClassHierarchyExclusions.txt"),
        ClassHierarchySubclassTest.class.getClassLoader());
  }

  /**
   * Asserts that {@code cha} applies both the refined and the inherited subtype relations.
   *
   * @param cha the class hierarchy to check
   */
  private static void assertRefined(ClassHierarchy cha) {
    IClass string = cha.lookupClass(TypeReference.JavaLangString);
    IClass thread = cha.lookupClass(TypeReference.JavaLangThread);
    IClass object = cha.lookupClass(TypeReference.JavaLangObject);
    assertThat(cha.isSubclassOf(string, thread)).isTrue();
    assertThat(cha.isSubclassOf(string, object)).isTrue();
    assertThat(cha.isSubclassOf(thread, string)).isFalse();
  }

  /** Tests the protected constructor that builds a hierarchy for all languages in the scope. */
  @Test
  public void testSubclassForScopeLanguages() throws IOException, ClassHierarchyException {
    AnalysisScope scope = makeScope();
    ClassHierarchy cha =
        new RefinedClassHierarchy(scope, new ClassLoaderFactoryImpl(scope.getExclusions()));
    assertThat(cha.getNumberOfClasses()).isPositive();
    assertRefined(cha);
  }

  /** Tests the protected constructor that builds a hierarchy for a single language. */
  @Test
  public void testSubclassForLanguage() throws IOException, ClassHierarchyException {
    AnalysisScope scope = makeScope();
    ClassHierarchy cha =
        new RefinedClassHierarchy(
            scope, new ClassLoaderFactoryImpl(scope.getExclusions()), JavaLanguage.get());
    assertThat(cha.getLoader(ClassLoaderReference.Application)).isNotNull();
    assertRefined(cha);
  }

  /** Tests the protected constructor that builds a hierarchy for a collection of languages. */
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
