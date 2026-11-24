package com.ibm.wala.ipa.callgraph.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MainEntrypointIteratorTest {
  private static ClassHierarchy cha;

  @BeforeAll
  static void makeHierarchy() throws Exception {
    cha =
        ClassHierarchyFactory.make(
            CallGraphTestUtil.makeJ2SEAnalysisScope(
                TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS));
  }

  @Test
  void iteratorThrowsAfterLastEntrypoint() {
    Iterator<Entrypoint> iterator =
        Util.makeMainEntrypoints(
                ClassLoaderReference.Application, cha, new String[] {"Lhello/Hello"})
            .iterator();

    assertThat(iterator.next().getMethod().getReference())
        .isEqualTo(StringStuff.makeMethodReference("hello.Hello.main([Ljava/lang/String;)V"));
    assertExhausted(iterator);
  }

  @Test
  void iteratorReturnsEveryRequestedEntrypointBeforeExhaustion() {
    Iterator<Entrypoint> iterator =
        Util.makeMainEntrypoints(
                ClassLoaderReference.Application,
                cha,
                new String[] {"Lhello/Hello", "Llambda/MethodRefs"})
            .iterator();

    assertThat(iterator.next().getMethod().getReference())
        .isEqualTo(StringStuff.makeMethodReference("hello.Hello.main([Ljava/lang/String;)V"));
    assertThat(iterator.next().getMethod().getReference())
        .isEqualTo(StringStuff.makeMethodReference("lambda.MethodRefs.main([Ljava/lang/String;)V"));
    assertExhausted(iterator);
  }

  private static void assertExhausted(Iterator<?> iterator) {
    assertSoftly(softly -> {
      softly.assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
      softly.assertThat(iterator).isExhausted();
    });
  }
}
