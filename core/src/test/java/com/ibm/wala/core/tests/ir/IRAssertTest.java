package com.ibm.wala.core.tests.ir;

import static com.ibm.wala.ssa.IRAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IRAssertTest {

  private static ClassHierarchy cha;

  @BeforeAll
  static void beforeAll() throws Exception {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    cha = ClassHierarchyFactory.make(scope);
  }

  @Test
  void testsInstructionsAndCFG() {
    MethodReference mr =
        MethodReference.findOrCreate(
            TypeReference.findOrCreate(ClassLoaderReference.Application, "Llambda/MethodRefs$C1"),
            Atom.findOrCreateUnicodeAtom("target"),
            Descriptor.findOrCreateUTF8("()V"));
    IMethod m = cha.resolveMethod(mr);
    AnalysisCacheImpl cache = new AnalysisCacheImpl();
    IR ir = cache.getIR(m);

    assertThat(ir).instructions().isNotEmpty();
    assertThat(ir).instruction(0).isNotNull();
    assertThat(ir).cfg().nodes().isNotEmpty();
    assertThat(ir).cfg().entry().isNotNull();
    assertThat(ir).cfg().exit().isNotNull();
  }

  @Test
  void rejectsNullIR() {
    assertThatThrownBy(() -> assertThat(null).instructions()).isInstanceOf(AssertionError.class);
  }
}
