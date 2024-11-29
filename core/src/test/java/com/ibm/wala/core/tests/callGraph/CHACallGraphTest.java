/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.callGraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.util.CallGraphSearchUtil;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.intset.IntSet;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class CHACallGraphTest {

  @Test
  public void testJava_cup()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testCHA(
        TestConstants.JAVA_CUP,
        TestConstants.JAVA_CUP_MAIN,
        CallGraphTestUtil.REGRESSION_EXCLUSIONS);
  }

  @Test
  public void testLambdaAndAnonymous()
      throws ClassHierarchyException, CancelException, IOException {
    CallGraph cg =
        testCHA(
            TestConstants.WALA_TESTDATA,
            "Llambda/LambdaAndAnonymous",
            CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    CGNode mainMethod = CallGraphSearchUtil.findMainMethod(cg);
    for (CallSiteReference site : Iterator2Iterable.make(mainMethod.iterateCallSites())) {
      if (site.isInterface() && site.getDeclaredTarget().getName().toString().equals("target")) {
        assertEquals(2, cg.getNumberOfTargets(mainMethod, site));
      }
    }
  }

  @Test
  public void testLambdaParamsAndCapture()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph cg =
        testCHA(
            TestConstants.WALA_TESTDATA,
            "Llambda/ParamsAndCapture",
            CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    System.err.println(cg);
    Function<String, MethodReference> getTargetRef =
        (klass) ->
            MethodReference.findOrCreate(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Llambda/ParamsAndCapture$" + klass),
                Atom.findOrCreateUnicodeAtom("target"),
                Descriptor.findOrCreateUTF8("()V"));

    Consumer<String> checkCalledFromOneSite =
        (klassName) -> {
          Set<CGNode> nodes = cg.getNodes(getTargetRef.apply(klassName));
          assertEquals(1, nodes.size(), "expected " + klassName + ".target() to be reachable");
          CGNode node = nodes.iterator().next();
          List<CGNode> predNodes = Iterator2Collection.toList(cg.getPredNodes(node));
          assertEquals(
              1,
              predNodes.size(),
              "expected " + klassName + ".target() to be invoked from one calling method");
          CGNode pred = predNodes.get(0);
          List<CallSiteReference> sites =
              Iterator2Collection.toList(cg.getPossibleSites(pred, node));
          assertEquals(
              5,
              sites.size(),
              "expected " + klassName + ".target() to be invoked from five call sites");
        };

    checkCalledFromOneSite.accept("C1");
    checkCalledFromOneSite.accept("C2");
    checkCalledFromOneSite.accept("C3");
    checkCalledFromOneSite.accept("C4");
    checkCalledFromOneSite.accept("C5");
  }

  public static CallGraph testCHA(
      String scopeFile, final String mainClass, final String exclusionsFile)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    return testCHA(scopeFile, exclusionsFile, cha -> Util.makeMainEntrypoints(cha, mainClass));
  }

  public static CallGraph testCHA(
      String scopeFile,
      String exclusionsFile,
      Function<IClassHierarchy, Iterable<Entrypoint>> makeEntrypoints)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(scopeFile, exclusionsFile);
    IClassHierarchy cha = ClassHierarchyFactory.make(scope);

    CHACallGraph CG = new CHACallGraph(cha);
    CG.init(makeEntrypoints.apply(cha));
    System.err.println(CallGraphStats.getCGStats(CG));
    // basic well-formedness
    for (CGNode node : CG) {
      int nodeNum = CG.getNumber(node);
      CG.getSuccNodeNumbers(node)
          .foreach(
              succNum -> {
                CGNode succNode = CG.getNode(succNum);
                IntSet predNodeNumbers = CG.getPredNodeNumbers(succNode);
                assertNotNull(
                    predNodeNumbers,
                    "no predecessors for " + succNode + " which is called by " + node);
                assertTrue(
                    predNodeNumbers.contains(nodeNum),
                    "missing predecessor " + node + " for " + succNode);
              });
    }
    return CG;
  }

  public static void main(String[] args)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testCHA(args[0], args.length > 1 ? args[1] : null, "Java60RegressionExclusions.txt");
  }
}
