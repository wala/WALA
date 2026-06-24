/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.cha;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.summaries.SummaryClassShellLoader;
import com.ibm.wala.types.TypeName;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Characterizes the class-hierarchy ordering invariant behind <a
 * href="https://github.com/wala/WALA/issues/1957">#1957</a>: a summary class shell must exist
 * before the subclass is added to the {@link ClassHierarchy} for the subclass-graph edge to be
 * recorded. The hierarchy snapshots each class's superclass at {@code addClass} time, so
 * registering a summary-modeled base after the subclass leaves the subclass graph without the edge.
 */
public class SummaryClassSuperclassOrderingTest extends WalaTestCase {

  private static final TypeName BASE = TypeName.string2TypeName("LBase");
  private static final TypeName SUB = TypeName.string2TypeName("LSub");
  private static final TypeName OBJECT = TypeName.string2TypeName("Ljava/lang/Object");

  private static SummaryClassShellLoader syntheticLoader(ClassHierarchy cha) {
    return (SummaryClassShellLoader) cha.getLoader(cha.getScope().getSyntheticLoader());
  }

  private static ClassHierarchy makeHierarchy() throws ClassHierarchyException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    return ClassHierarchyFactory.make(scope);
  }

  /** Registering the summary-modeled base before the subclass records the subclass-graph edge. */
  @Test
  public void testBaseRegisteredBeforeSubclass() throws ClassHierarchyException, IOException {
    ClassHierarchy cha = makeHierarchy();
    SummaryClassShellLoader synthetic = syntheticLoader(cha);

    IClass base = synthetic.defineSummaryClassShell(BASE, OBJECT);
    IClass sub = synthetic.defineSummaryClassShell(SUB, BASE);

    assertThat(sub.getSuperclass()).isSameAs(base);
    assertThat(cha.getImmediateSubclasses(base)).contains(sub);
  }

  /**
   * Registering the summary-modeled base after the subclass (the current ordering) leaves the
   * subclass-graph snapshot stale: the base does not see the subclass.
   */
  @Test
  public void testBaseRegisteredAfterSubclass() throws ClassHierarchyException, IOException {
    ClassHierarchy cha = makeHierarchy();
    SummaryClassShellLoader synthetic = syntheticLoader(cha);

    // The subclass is added while its base does not yet exist, so the hierarchy snapshots it under
    // the root.
    IClass sub = synthetic.defineSummaryClassShell(SUB, BASE);
    IClass base = synthetic.defineSummaryClassShell(BASE, OBJECT);

    assertThat(cha.getImmediateSubclasses(base)).doesNotContain(sub);
    // A SummaryClassShell resolves its superclass live, so the IClass itself self-heals to the base
    // once it exists; it is the hierarchy snapshot, not the IClass, that stays stale.
    assertThat(sub.getSuperclass()).isSameAs(base);
  }
}
