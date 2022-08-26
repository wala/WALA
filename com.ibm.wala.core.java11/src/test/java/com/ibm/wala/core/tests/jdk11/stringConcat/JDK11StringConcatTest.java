package com.ibm.wala.core.tests.jdk11.stringConcat;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/** Tests string concatenation on JDK 11+, which uses invokedynamic at the bytecode level */
public class JDK11StringConcatTest extends WalaTestCase {
  @Test
  public void testStringConcat()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            "wala.testdata.txt", CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, "LstringConcat/StringConcat");

    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);

    // Find node corresponding to main
    TypeReference tm =
        TypeReference.findOrCreate(ClassLoaderReference.Application, "LstringConcat/StringConcat");
    MethodReference mm = MethodReference.findOrCreate(tm, "main", "([Ljava/lang/String;)V");
    Assert.assertTrue("expect main node", cg.getNodes(mm).iterator().hasNext());
    CGNode mnode = cg.getNodes(mm).iterator().next();

    // should be from main to testConcat()
    TypeReference t1s =
        TypeReference.findOrCreate(ClassLoaderReference.Application, "LstringConcat/StringConcat");
    MethodReference t1m = MethodReference.findOrCreate(t1s, "testConcat", "()Ljava/lang/String;");
    Assert.assertTrue("expect testConcat node", cg.getNodes(t1m).iterator().hasNext());
    CGNode t1node = cg.getNodes(t1m).iterator().next();

    // Check call from main to testConcat()
    Assert.assertTrue(
        "should have call site from main to StringConcat.testConcat()",
        cg.getPossibleSites(mnode, t1node).hasNext());

    // For now, we will see no call edges from the testConcat method, as we have not added
    // support for invokedynamic-based string concatenation yet
    // TODO add support and change this assertion
    Assert.assertFalse(
        "did not expect call nodes from testConcat", cg.getSuccNodes(t1node).hasNext());
  }
}
