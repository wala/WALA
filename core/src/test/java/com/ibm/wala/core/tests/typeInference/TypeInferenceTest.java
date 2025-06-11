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
package com.ibm.wala.core.tests.typeInference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.core.util.strings.ImmutableByteArray;
import com.ibm.wala.core.util.strings.UTF8Convert;
import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test that the SSA-numbering of variables in the IR is deterministic.
 *
 * <p>Introduced 05-AUG-03; the default implementation of hashCode was being invoked.
 * Object.hashCode is a source of random numbers and has no place in a deterministic program.
 */
public class TypeInferenceTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = TypeInferenceTest.class.getClassLoader();

  private static AnalysisScope scope;

  private static ClassHierarchy cha;

  private static AnalysisOptions options;

  private static IAnalysisCacheView cache;

  public static void main(String[] args) {
    justThisTest(TypeInferenceTest.class);
  }

  @BeforeAll
  public static void beforeClass() throws Exception {

    scope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA,
            new FileProvider().getFile("J2SEClassHierarchyExclusions.txt"),
            MY_CLASSLOADER);

    options = new AnalysisOptions(scope, null);
    cache = new AnalysisCacheImpl();
    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions());

    try {
      cha = ClassHierarchyFactory.make(scope, factory);
    } catch (ClassHierarchyException e) {
      throw new Exception(e);
    }
  }

  @AfterAll
  public static void afterClass() throws Exception {
    Warnings.clear();
    scope = null;
    cha = null;
    options = null;
    cache = null;
  }

  @Test
  public void test1() {
    MethodReference method =
        scope.findMethod(
            AnalysisScope.APPLICATION,
            "LtypeInference/TI",
            Atom.findOrCreateUnicodeAtom("foo"),
            new ImmutableByteArray(UTF8Convert.toUTF8("()V")));
    assertThat(method).isNotNull();
    IMethod imethod = cha.resolveMethod(method);
    assertThat(imethod).isNotNull();
    IR ir = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    System.out.println(ir);

    TypeInference ti = TypeInference.make(ir, false);
    for (int i = 1; i <= ir.getSymbolTable().getMaxValueNumber(); i++) {
      System.err.println(i + " " + ti.getType(i));
    }
  }

  @Test
  public void test2() {
    MethodReference method =
        scope.findMethod(
            AnalysisScope.APPLICATION,
            "LtypeInference/TI",
            Atom.findOrCreateUnicodeAtom("bar"),
            new ImmutableByteArray(UTF8Convert.toUTF8("(I)V")));
    assertThat(method).isNotNull();
    IMethod imethod = cha.resolveMethod(method);
    assertThat(imethod).isNotNull();
    IR ir = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    System.out.println(ir);

    TypeInference ti = TypeInference.make(ir, true);
    assertThat(ti.getType(2)).isNotNull();
  }

  @Test
  public void test3() {
    MethodReference method =
        scope.findMethod(
            AnalysisScope.APPLICATION,
            "LtypeInference/TI",
            Atom.findOrCreateUnicodeAtom("inferInt"),
            new ImmutableByteArray(UTF8Convert.toUTF8("()V")));
    assertThat(method).isNotNull();
    IMethod imethod = cha.resolveMethod(method);
    assertThat(imethod).isNotNull();
    IR ir = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    System.out.println(ir);

    TypeInference ti = TypeInference.make(ir, true);
    TypeAbstraction type = ti.getType(7);
    assertThat(type).hasToString("int");
  }

  @Test
  public void test4() {
    MethodReference method =
        scope.findMethod(
            AnalysisScope.APPLICATION,
            "LtypeInference/TI",
            Atom.findOrCreateUnicodeAtom("useCast"),
            new ImmutableByteArray(UTF8Convert.toUTF8("(Ljava/lang/Object;)V")));
    assertThat(method).isNotNull();
    IMethod imethod = cha.resolveMethod(method);
    assertThat(imethod).isNotNull();
    IR ir = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    System.out.println(ir);

    TypeInference ti = TypeInference.make(ir, false);
    TypeAbstraction type = ti.getType(4);
    assertThat(type).isNotNull();
    assertThatObject(type)
        .extracting(coneType -> coneType.getTypeReference().getName())
        .hasToString("Ljava/lang/String");
  }
}
