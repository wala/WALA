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
package com.ibm.wala.cast.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.Pair;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests the loader-local resolution of summary-modeled class shells, the mechanism behind <a
 * href="https://github.com/wala/WALA/issues/1957">#1957</a>.
 *
 * <p>A CAst class ({@code CoreClass}, the base of e.g. the Python front-end's {@code PythonClass})
 * resolves its superclass through its own loader's type map only: {@code CoreClass.getSuperclass()}
 * is a live, uncached {@code types.get(superName)} with no parent-loader or class-hierarchy
 * fallback. So a source subclass can only inherit a summary-modeled base if a shell for that base
 * has been registered <em>into the same loader</em> before the subclass is defined. Registering the
 * shell into the {@code BypassSyntheticClassLoader} (where XML method summaries are otherwise
 * materialized) would never be seen here.
 */
public class SummaryClassShellResolutionTest {

  private static final TypeName BASE = TypeName.string2TypeName("Ltensorflow/keras/layers/Layer");
  private static final TypeName SUB = TypeName.string2TypeName("Lmy/model/GCN");

  /** The hook registers a resolvable shell and is idempotent. */
  @Test
  public void testShellIsRegisteredAndIdempotent() {
    TestLoader loader = new TestLoader();

    assertThat(loader.lookupClass(BASE)).isNull();

    IClass shell = loader.defineSummaryClassShell(BASE, null);
    assertThat(shell).isNotNull();
    assertThat(loader.lookupClass(BASE)).isSameAs(shell);

    // Idempotent: a second registration returns the same class rather than replacing it.
    assertThat(loader.defineSummaryClassShell(BASE, null)).isSameAs(shell);
  }

  /** A subclass defined after its base shell resolves the inheritance edge. */
  @Test
  public void testSubclassResolvesRegisteredBase() {
    TestLoader loader = new TestLoader();
    IClass shell = loader.defineSummaryClassShell(BASE, null);
    IClass sub = loader.new CoreClass(SUB, BASE, loader, null);
    assertThat(sub.getSuperclass()).isSameAs(shell);
  }

  /**
   * Without a shell, the base is invisible to loader-local resolution, exactly the #1957 symptom,
   * since the summary-modeled base lives in a different loader (and is created only after the
   * hierarchy is built).
   */
  @Test
  public void testSubclassWithoutShellHasNoSuperclass() {
    TestLoader loader = new TestLoader();

    IClass sub = loader.new CoreClass(SUB, BASE, loader, null);

    assertThat(sub.getSuperclass()).isNull();
  }

  /**
   * A minimal concrete {@link CAstAbstractModuleLoader} sufficient to exercise shell resolution.
   */
  private static final class TestLoader extends CAstAbstractModuleLoader {

    TestLoader() {
      super(null, null);
    }

    @Override
    public Language getLanguage() {
      return JavaLanguage.get();
    }

    @Override
    public ClassLoaderReference getReference() {
      return ClassLoaderReference.Application;
    }

    @Override
    public SSAInstructionFactory getInstructionFactory() {
      return JavaLanguage.get().instructionFactory();
    }

    @Override
    protected TranslatorToCAst getTranslatorToCAst(CAst ast, ModuleEntry m, List<Module> modules) {
      throw new UnsupportedOperationException("not needed for this test");
    }

    @Override
    protected boolean shouldTranslate(CAstEntity entity) {
      return false;
    }

    @Override
    protected TranslatorToIR initTranslator(Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities) {
      throw new UnsupportedOperationException("not needed for this test");
    }
  }
}
