/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.callGraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.CancelRuntimeException;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.CPAContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Check properties of a call to clone() in RTA */
public class CPATest extends WalaTestCase {

  @Test
  public void cpaTest1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doCPATest("Lcpa/CPATest1", "(Lcpa/CPATest1$N;)Lcpa/CPATest1$N;");
  }

  @Test
  public void cpaTest2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doCPATest("Lcpa/CPATest2", "(Lcpa/CPATest2$N;I)Lcpa/CPATest2$N;");
  }

  @Test
  public void cpaTest3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    doCPATest("Lcpa/CPATest3", "(Lcpa/CPATest3$N;)Lcpa/CPATest3$N;");
  }

  /**
   * Checks that canceling a call-graph build from inside the cross product of a call's arguments
   * stops the build.
   */
  @Test
  public void cancelInsideCrossProduct()
      throws ClassHierarchyException, IllegalArgumentException, IOException {
    SSAPropagationCallGraphBuilder builder = makeCPABuilder("Lcpa/CPATest1");
    // Report cancellation only while the cross product is being enumerated, so the build can be
    // canceled only by the check there.
    IProgressMonitor monitor =
        new NullProgressMonitor() {
          @Override
          public boolean isCanceled() {
            return StackWalker.getInstance()
                .walk(frames ->
                    frames.anyMatch(frame -> frame.getClassName().endsWith("$CrossProductRec")));
          }

          @Override
          public String getCancelMessage() {
            return "canceled in the cross product";
          }
        };
    assertThatThrownBy(() -> builder.makeCallGraph(builder.getOptions(), monitor))
        .isInstanceOf(CallGraphBuilderCancelException.class)
        .cause()
        .isInstanceOf(CancelRuntimeException.class);
  }

  private static void doCPATest(String testClass, String testIdSignature)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    SSAPropagationCallGraphBuilder builder = makeCPABuilder(testClass);
    CallGraph cg = builder.makeCallGraph(builder.getOptions(), null);

    // Find id
    TypeReference str = TypeReference.findOrCreate(ClassLoaderReference.Application, testClass);
    MethodReference ct =
        MethodReference.findOrCreate(
            str, Atom.findOrCreateUnicodeAtom("id"), Descriptor.findOrCreateUTF8(testIdSignature));
    Set<CGNode> idNodes = cg.getNodes(ct);

    System.err.println(cg);

    assertThat(idNodes).hasSize(2);
  }

  /**
   * Returns a 0-CFA builder with a {@link CPAContextSelector} for the {@code main} method of {@code
   * testClass}.
   *
   * @param testClass the name of a class in the test data whose {@code main} method is the entry
   *     point
   * @return the builder, whose options hold the entry point
   */
  private static SSAPropagationCallGraphBuilder makeCPABuilder(String testClass)
      throws ClassHierarchyException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cha, testClass);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    SSAPropagationCallGraphBuilder builder =
        Util.makeZeroCFABuilder(JavaLanguage.get(), options, new AnalysisCacheImpl(), cha);
    builder.setContextSelector(new CPAContextSelector(builder.getContextSelector()));
    return builder;
  }
}
