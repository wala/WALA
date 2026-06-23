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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.summaries.SummaryClassShell;
import com.ibm.wala.ipa.summaries.SummaryClassShellLoader;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
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

  /** A shell declared in a loader other than the one the driver is asked to populate. */
  private static final String XML_PRIMORDIAL =
      """
      <summary-spec>
        <classloader name="Primordial">
          <class name="Foreign" super="Ljava/lang/Object"/>
        </classloader>
      </summary-spec>
      """;

  private static AnalysisScope scope() throws IOException {
    return CallGraphTestUtil.makeJ2SEAnalysisScope(
        TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
  }

  private static XMLMethodSummaryReader read(AnalysisScope scope, String xml) {
    return new XMLMethodSummaryReader(
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), scope);
  }

  @Test
  public void testReaderCapturesSuperAsShellOptIn() throws IOException {
    AnalysisScope scope = scope();
    XMLMethodSummaryReader reader = read(scope, XML);

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
    AnalysisScope scope = scope();
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    XMLMethodSummaryReader reader = read(scope, XML);
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

  /** A shell carries no members; its only role is to occupy a place in the hierarchy. */
  @Test
  public void testShellHasNoMembers() throws ClassHierarchyException, IOException {
    AnalysisScope scope = scope();
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Util.addSummaryClassShells(cha.getLoader(scope.getSyntheticLoader()), read(scope, XML));

    IClass base = cha.lookupClass(TypeReference.findOrCreate(scope.getSyntheticLoader(), "LBase"));
    assertThat(base).isInstanceOf(SummaryClassShell.class);

    assertThat(base.getDeclaredMethods()).isEmpty();
    assertThat(base.getAllMethods()).isEmpty();
    assertThat(base.getDeclaredInstanceFields()).isEmpty();
    assertThat(base.getDeclaredStaticFields()).isEmpty();
    assertThat(base.getAllInstanceFields()).isEmpty();
    assertThat(base.getAllStaticFields()).isEmpty();
    assertThat(base.getAllFields()).isEmpty();
    assertThat(base.getDirectInterfaces()).isEmpty();
    assertThat(base.getAllImplementedInterfaces()).isEmpty();
    assertThat(base.getMethod(Selector.make("foo()V"))).isNull();
    assertThat(base.getField(Atom.findOrCreateAsciiAtom("f"))).isNull();
    assertThat(base.getClassInitializer()).isNull();
    assertThat(base.isPublic()).isTrue();
    assertThat(base.isPrivate()).isFalse();
    assertThat(base.isReferenceType()).isTrue();
    assertThat(base.getModifiers()).isZero();
    assertThat(base.toString()).contains("Base");
  }

  /** Registering a shell twice returns the same class, and a null super defaults to the root. */
  @Test
  public void testShellRegistrationIsIdempotentAndDefaultsSuperToRoot()
      throws ClassHierarchyException, IOException {
    AnalysisScope scope = scope();
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    SummaryClassShellLoader synthetic =
        (SummaryClassShellLoader) cha.getLoader(scope.getSyntheticLoader());
    TypeName name = TypeName.string2TypeName("LRootless");

    IClass first = synthetic.defineSummaryClassShell(name, null);
    assertThat(first.getSuperclass()).isSameAs(cha.getRootClass());
    assertThat(synthetic.defineSummaryClassShell(name, null)).isSameAs(first);
  }

  /** The driver rejects null arguments. */
  @Test
  public void testDriverRejectsNullArguments() throws ClassHierarchyException, IOException {
    AnalysisScope scope = scope();
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    IClassLoader synthetic = cha.getLoader(scope.getSyntheticLoader());
    XMLMethodSummaryReader reader = read(scope, XML);

    assertThatThrownBy(() -> Util.addSummaryClassShells(null, reader))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Util.addSummaryClassShells(synthetic, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** A loader that cannot introduce shells is silently skipped. */
  @Test
  public void testDriverSkipsLoaderWithoutCapability() throws ClassHierarchyException, IOException {
    AnalysisScope scope = scope();
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    // The primordial loader is a bytecode loader and does not implement SummaryClassShellLoader.
    IClassLoader primordial = cha.getLoader(scope.getPrimordialLoader());

    Util.addSummaryClassShells(primordial, read(scope, XML));

    assertThat(cha.lookupClass(TypeReference.findOrCreate(scope.getSyntheticLoader(), "LBase")))
        .isNull();
  }

  /** Only classes whose declaring loader matches the target loader are registered. */
  @Test
  public void testDriverSkipsClassesFromOtherLoaders() throws ClassHierarchyException, IOException {
    AnalysisScope scope = scope();
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    IClassLoader synthetic = cha.getLoader(scope.getSyntheticLoader());

    // The summary declares its shell under the Primordial loader, not the Synthetic one.
    Util.addSummaryClassShells(synthetic, read(scope, XML_PRIMORDIAL));

    assertThat(cha.lookupClass(TypeReference.findOrCreate(scope.getSyntheticLoader(), "LForeign")))
        .isNull();
  }
}
