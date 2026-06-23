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
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.summaries.SummaryClassShell;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.types.TypeReference;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Tests the generic, front-end-agnostic summary class-shell mechanism behind <a
 * href="https://github.com/wala/WALA/issues/1957">#1957</a>: a summary {@code <class>} that
 * declares a {@code super} opts in to being materialized as a standalone class shell positioned at
 * that superclass, via {@link Util#addSummaryClassShells}; a {@code <class>} without a {@code
 * super} stays method-summary-only.
 */
public class SummaryClassShellTest extends WalaTestCase {

  /** A {@code <class super=...>} opts in as a shell; a plain {@code <class>} does not. */
  private static final String XML =
      """
      <summary-spec>
        <classloader name="Synthetic">
          <class name="Base" super="Ljava/lang/Object"/>
          <class name="NotAShell"/>
        </classloader>
      </summary-spec>
      """;

  private static XMLMethodSummaryReader read(AnalysisScope scope) {
    return new XMLMethodSummaryReader(
        new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)), scope);
  }

  @Test
  public void testReaderCapturesSuperAsShellOptIn() throws IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    XMLMethodSummaryReader reader = read(scope);

    TypeReference base = TypeReference.findOrCreate(scope.getSyntheticLoader(), "LBase");
    TypeReference notAShell = TypeReference.findOrCreate(scope.getSyntheticLoader(), "LNotAShell");

    // Both are declared classes...
    assertThat(reader.getClasses()).contains(base, notAShell);
    // ...but only the one declaring a super opts in to being a shell.
    assertThat(reader.getClassSuperclasses()).containsKey(base).doesNotContainKey(notAShell);
    assertThat(reader.getClassSuperclasses().get(base))
        .isEqualTo(TypeReference.findOrCreate(scope.getSyntheticLoader(), "Ljava/lang/Object"));
  }

  @Test
  public void testDriverRegistersShellResolvingItsSuperclass()
      throws ClassHierarchyException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    XMLMethodSummaryReader reader = read(scope);
    IClassLoader synthetic = cha.getLoader(scope.getSyntheticLoader());

    Util.addSummaryClassShells(synthetic, reader);

    TypeReference baseRef = TypeReference.findOrCreate(scope.getSyntheticLoader(), "LBase");
    IClass base = cha.lookupClass(baseRef);
    assertThat(base).isInstanceOf(SummaryClassShell.class);
    assertThat(base.getSuperclass()).isSameAs(cha.lookupClass(TypeReference.JavaLangObject));

    // The opt-out class did not get a shell.
    TypeReference notAShell = TypeReference.findOrCreate(scope.getSyntheticLoader(), "LNotAShell");
    assertThat(cha.lookupClass(notAShell)).isNull();
  }
}
