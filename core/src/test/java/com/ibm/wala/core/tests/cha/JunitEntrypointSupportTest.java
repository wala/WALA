package com.ibm.wala.core.tests.cha;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.scope.JUnitEntryPoints;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Iterator2Collection;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class JunitEntrypointSupportTest {

  @Test
  public void basic()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Set<Entrypoint> entrypoints = Iterator2Collection.toSet(JUnitEntryPoints.make(cha).iterator());
    System.err.println(entrypoints);
  }
}
