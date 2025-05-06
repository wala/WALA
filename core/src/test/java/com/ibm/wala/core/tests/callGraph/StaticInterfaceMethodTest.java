package com.ibm.wala.core.tests.callGraph;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class StaticInterfaceMethodTest {

  @Test
  public void staticInterfaceMethodAsEntrypoint()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    MethodReference staticInterfaceMethodRef =
        MethodReference.findOrCreate(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application,
                "LstaticInterfaceMethod/InterfaceWithStaticMethod"),
            "test",
            "()V");
    Iterable<Entrypoint> entrypoints =
        List.of(new DefaultEntrypoint(staticInterfaceMethodRef, cha));
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);

    assertThat(cg.getNodes(staticInterfaceMethodRef)).hasSize(1);
  }
}
