package com.ibm.wala.core.tests.cha;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.scope.JUnitEntryPoints;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JunitEntrypointSupportTest {

  @Test
  public void basic() throws ClassHierarchyException, IllegalArgumentException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    List<Entrypoint> entrypoints =
        Iterator2Collection.toList(JUnitEntryPoints.make(cha).iterator());
    assertThat(entrypoints)
        .extracting(Object::toString)
        .containsExactlyInAnyOrder(
            "< Application, Ljunit/JunitTests$A, test2()V >([<Application,Ljunit/JunitTests$A>])",
            "< Application, Ljunit/JunitTests$A, <init>()V >([<Application,Ljunit/JunitTests$A>])",
            "< Application, Ljunit/JunitTests, test1()V >([<Application,Ljunit/JunitTests>])",
            "< Application, Ljunit/JunitTests, <init>()V >([<Application,Ljunit/JunitTests>])");
  }
}
