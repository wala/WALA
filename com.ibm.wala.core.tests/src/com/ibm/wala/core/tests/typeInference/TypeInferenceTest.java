/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.typeInference;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
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
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;
import com.ibm.wala.util.strings.UTF8Convert;
import com.ibm.wala.util.warnings.Warnings;

/**
 * Test that the SSA-numbering of variables in the IR is deterministic.
 * 
 * Introduced 05-AUG-03; the default implementation of hashCode was being
 * invoked. Object.hashCode is a source of random numbers and has no place in a
 * deterministic program.
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

  @BeforeClass
  public static void beforeClass() throws Exception {

    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"), MY_CLASSLOADER);

    options = new AnalysisOptions(scope, null);
    cache = new AnalysisCacheImpl();
    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions());

    try {
      cha = ClassHierarchyFactory.make(scope, factory);
    } catch (ClassHierarchyException e) {
      throw new Exception(e);
    }
  }

  @AfterClass
  public static void afterClass() throws Exception {
    Warnings.clear();
    scope = null;
    cha = null;
    options = null;
    cache = null;
  }

  @Test public void test1() {
    MethodReference method = scope.findMethod(AnalysisScope.APPLICATION, "LtypeInference/TI", Atom.findOrCreateUnicodeAtom("foo"),
        new ImmutableByteArray(UTF8Convert.toUTF8("()V")));
    Assert.assertNotNull("method not found", method);
    IMethod imethod = cha.resolveMethod(method);
    Assert.assertNotNull("imethod not found", imethod);
    IR ir = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    System.out.println(ir);

    TypeInference ti = TypeInference.make(ir, false);
    for (int i = 1; i <= ir.getSymbolTable().getMaxValueNumber(); i++) {
      System.err.println(i + " " + ti.getType(i));
    }
  }

  @Test public void test2() {
    MethodReference method = scope.findMethod(AnalysisScope.APPLICATION, "LtypeInference/TI", Atom.findOrCreateUnicodeAtom("bar"),
        new ImmutableByteArray(UTF8Convert.toUTF8("(I)V")));
    Assert.assertNotNull("method not found", method);
    IMethod imethod = cha.resolveMethod(method);
    Assert.assertNotNull("imethod not found", imethod);
    IR ir = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    System.out.println(ir);

    TypeInference ti = TypeInference.make(ir, true);
    Assert.assertNotNull("null type abstraction for parameter", ti.getType(2));
  }

  @Test public void test3() {
    MethodReference method = scope.findMethod(AnalysisScope.APPLICATION, "LtypeInference/TI", Atom.findOrCreateUnicodeAtom("inferInt"),
        new ImmutableByteArray(UTF8Convert.toUTF8("()V")));
    Assert.assertNotNull("method not found", method);
    IMethod imethod = cha.resolveMethod(method);
    Assert.assertNotNull("imethod not found", imethod);
    IR ir = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    System.out.println(ir);

    TypeInference ti = TypeInference.make(ir, true);
    TypeAbstraction type = ti.getType(7);
    Assert.assertNotNull("null type abstraction", type);
    Assert.assertTrue("inferred wrong type", type.toString().equals("int"));
  }

  @Test public void test4() {
    MethodReference method = scope.findMethod(AnalysisScope.APPLICATION, "LtypeInference/TI", Atom.findOrCreateUnicodeAtom("useCast"),
        new ImmutableByteArray(UTF8Convert.toUTF8("(Ljava/lang/Object;)V")));
    Assert.assertNotNull("method not found", method);
    IMethod imethod = cha.resolveMethod(method);
    Assert.assertNotNull("imethod not found", imethod);
    IR ir = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    System.out.println(ir);

    TypeInference ti = TypeInference.make(ir, false);
    TypeAbstraction type = ti.getType(4);
    Assert.assertNotNull("null type abstraction", type);
    Assert.assertTrue("inferred wrong type " + type, type instanceof ConeType && ((ConeType)type).getTypeReference().getName().toString().equals("Ljava/lang/String"));
  }


}
