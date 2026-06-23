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
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Characterizes the class-hierarchy ordering invariant behind <a
 * href="https://github.com/wala/WALA/issues/1957">#1957</a>: a source class that {@code extends} a
 * type modeled only by an XML method summary keeps that inheritance edge only if the summary class
 * exists <em>before</em> the subclass is added to the {@link ClassHierarchy}.
 *
 * <p>The {@link ClassHierarchy} snapshots each class's superclass at {@code addClass} time. So
 * registering a summary-modeled base <em>after</em> the subclass (as happens today, since {@code
 * Util.addBypassLogic(...)} materializes summary classes only after {@code buildClassHierarchy()})
 * leaves the subclass parented at {@code Object}. Registering the base <em>before</em> the
 * subclass, the remedy in #1957, preserves the edge. Both orderings are exercised below.
 *
 * <p>This models the class-hierarchy half of the bug. The loader-local <em>resolution</em> half,
 * which is what bites the CAst/Python front-end in practice, is covered by {@code
 * com.ibm.wala.cast.test.SummaryClassShellResolutionTest}.
 */
public class SummaryClassSuperclassOrderingTest extends WalaTestCase {

  /** Registering the summary-modeled base before the subclass preserves the inheritance edge. */
  @Test
  public void testBaseRegisteredBeforeSubclass() throws ClassHierarchyException, IOException {
    Fixture f = new Fixture();

    f.registerBase();
    f.registerSub();

    assertThat(f.sub().getSuperclass()).isSameAs(f.base());
    assertThat(f.cha.getImmediateSubclasses(f.base())).contains(f.sub());
  }

  /**
   * Registering the summary-modeled base after the subclass (the current ordering) loses the
   * inheritance edge: the subclass falls back to {@code Object}.
   */
  @Test
  public void testBaseRegisteredAfterSubclass() throws ClassHierarchyException, IOException {
    Fixture f = new Fixture();

    f.registerSub();
    f.registerBase();

    assertThat(f.sub().getSuperclass()).isSameAs(f.cha.getRootClass());
    assertThat(f.cha.getImmediateSubclasses(f.base())).doesNotContain(f.sub());
  }

  /**
   * Holds a hierarchy plus a summary-modeled {@code Base} and a {@code Sub extends Base}, both as
   * synthetic classes registered through the {@link BypassSyntheticClassLoader}. {@code Sub} caches
   * its superclass on first resolution, mirroring {@link
   * com.ibm.wala.classLoader.BytecodeClass#getSuperclass()}.
   */
  private static final class Fixture {
    final ClassHierarchy cha;
    private final BypassSyntheticClassLoader synthetic;
    private final SummaryClass base;
    private final SummaryClass sub;
    private final TypeReference baseRef;
    private final TypeReference subRef;

    Fixture() throws ClassHierarchyException, IOException {
      AnalysisScope scope =
          CallGraphTestUtil.makeJ2SEAnalysisScope(
              TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
      cha = ClassHierarchyFactory.make(scope);
      synthetic = (BypassSyntheticClassLoader) cha.getLoader(scope.getSyntheticLoader());
      baseRef =
          TypeReference.findOrCreate(scope.getSyntheticLoader(), TypeName.string2TypeName("LBase"));
      subRef =
          TypeReference.findOrCreate(scope.getSyntheticLoader(), TypeName.string2TypeName("LSub"));
      base = new SummaryClass(baseRef, cha, null);
      sub = new SummaryClass(subRef, cha, baseRef.getName());
    }

    void registerBase() {
      synthetic.registerClass(baseRef.getName(), base);
    }

    void registerSub() {
      synthetic.registerClass(subRef.getName(), sub);
    }

    IClass base() {
      return base;
    }

    IClass sub() {
      return cha.lookupClass(subRef);
    }
  }

  /**
   * A minimal {@link SyntheticClass} that resolves its superclass through its class loader and
   * caches the first resolution, mirroring the contract of {@link
   * com.ibm.wala.classLoader.BytecodeClass#getSuperclass()}.
   */
  private static final class SummaryClass extends SyntheticClass {

    private final TypeName superName;

    private boolean superclassComputed;
    private IClass superClass;

    SummaryClass(TypeReference type, ClassHierarchy cha, TypeName superName) {
      super(type, cha);
      this.superName = superName;
    }

    @Override
    public IClass getSuperclass() {
      if (!superclassComputed) {
        superclassComputed = true;
        IClassLoader loader = getClassLoader();
        if (superName != null) {
          superClass = loader.lookupClass(superName);
        }
        if (superClass == null) {
          superClass = loader.lookupClass(TypeReference.JavaLangObject.getName());
        }
      }
      return superClass;
    }

    @Override
    public Collection<IClass> getDirectInterfaces() {
      return Collections.emptySet();
    }

    @Override
    public Collection<IClass> getAllImplementedInterfaces() {
      return Collections.emptySet();
    }

    @Override
    public IMethod getMethod(Selector selector) {
      return null;
    }

    @Override
    public IField getField(Atom name) {
      return null;
    }

    @Override
    public IMethod getClassInitializer() {
      return null;
    }

    @Override
    public Collection<? extends IMethod> getDeclaredMethods() {
      return Collections.emptySet();
    }

    @Override
    public Collection<IField> getDeclaredInstanceFields() {
      return Collections.emptySet();
    }

    @Override
    public Collection<IField> getDeclaredStaticFields() {
      return Collections.emptySet();
    }

    @Override
    public Collection<IField> getAllInstanceFields() {
      return Collections.emptySet();
    }

    @Override
    public Collection<IField> getAllStaticFields() {
      return Collections.emptySet();
    }

    @Override
    public Collection<IField> getAllFields() {
      return Collections.emptySet();
    }

    @Override
    public Collection<? extends IMethod> getAllMethods() {
      return Collections.emptySet();
    }

    @Override
    public boolean isReferenceType() {
      return getReference().isReferenceType();
    }

    @Override
    public boolean isPublic() {
      return true;
    }

    @Override
    public boolean isPrivate() {
      return false;
    }

    @Override
    public int getModifiers() {
      return 0;
    }
  }
}
