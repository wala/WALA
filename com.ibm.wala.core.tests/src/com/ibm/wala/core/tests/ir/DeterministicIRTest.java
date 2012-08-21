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
package com.ibm.wala.core.tests.ir;

import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;
import com.ibm.wala.util.strings.UTF8Convert;
import com.ibm.wala.util.warnings.Warnings;

/**
 * Test that the SSA-numbering of variables in the IR is deterministic.
 * 
 * Introduced 05-AUG-03; the default implementation of hashCode was being invoked. Object.hashCode is a source of random numbers and
 * has no place in a deterministic program.
 */
public class DeterministicIRTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = DeterministicIRTest.class.getClassLoader();

  private static AnalysisScope scope;

  private static ClassHierarchy cha;

  private static AnalysisOptions options;

  private static AnalysisCache cache;

  public static void main(String[] args) {
    justThisTest(DeterministicIRTest.class);
  }

  @BeforeClass
  public static void beforeClass() throws Exception {

    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA,
        (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"), MY_CLASSLOADER);
    options = new AnalysisOptions(scope, null);
    cache = new AnalysisCache();
    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions());

    try {
      cha = ClassHierarchy.make(scope, factory);
    } catch (ClassHierarchyException e) {
      throw new Exception();
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

  /**
   * @param method
   */
  private IR doMethod(MethodReference method) {
    Assert.assertNotNull("method not found", method);
    IMethod imethod = cha.resolveMethod(method);
    Assert.assertNotNull("imethod not found", imethod);
    IR ir1 = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    cache.getSSACache().wipe();

    checkNotAllNull(ir1.getInstructions());
    checkNoneNull(ir1.iterateAllInstructions());
    try {
      GraphIntegrity.check(ir1.getControlFlowGraph());
    } catch (UnsoundGraphException e) {
      System.err.println(ir1);
      e.printStackTrace();
      Assert.assertTrue("unsound CFG for ir1", false);
    }

    IR ir2 = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    cache.getSSACache().wipe();

    try {
      GraphIntegrity.check(ir2.getControlFlowGraph());
    } catch (UnsoundGraphException e1) {
      System.err.println(ir2);
      e1.printStackTrace();
      Assert.assertTrue("unsound CFG for ir2", false);
    }

    Assert.assertEquals(ir1.toString(), ir2.toString());
    return ir1;
  }

  // The Tests ///////////////////////////////////////////////////////

  /**
   * @param iterator
   */
  private void checkNoneNull(Iterator<?> iterator) {
    while (iterator.hasNext()) {
      Assert.assertTrue(iterator.next() != null);
    }

  }

  /**
   * @param instructions
   */
  private static void checkNotAllNull(SSAInstruction[] instructions) {
    for (int i = 0; i < instructions.length; i++) {
      if (instructions[i] != null) {
        return;
      }
    }
    Assert.assertTrue("no instructions generated", false);
  }

  @Test public void testIR1() {
    // 'remove' is a nice short method
    doMethod(scope.findMethod(AnalysisScope.APPLICATION, "Ljava/util/HashMap", Atom.findOrCreateUnicodeAtom("remove"),
        new ImmutableByteArray(UTF8Convert.toUTF8("(Ljava/lang/Object;)Ljava/lang/Object;"))));
  }

  @Test public void testIR2() {
    // 'equals' is a nice medium-sized method
    doMethod(scope.findMethod(AnalysisScope.APPLICATION, "Ljava/lang/String", Atom.findOrCreateUnicodeAtom("equals"),
        new ImmutableByteArray(UTF8Convert.toUTF8("(Ljava/lang/Object;)Z"))));
  }

  @Test public void testIR3() {
    // 'resolveProxyClass' is a nice long method (at least in Sun libs)
    doMethod(scope.findMethod(AnalysisScope.APPLICATION, "Ljava/io/ObjectInputStream", Atom
        .findOrCreateUnicodeAtom("resolveProxyClass"), new ImmutableByteArray(UTF8Convert
        .toUTF8("([Ljava/lang/String;)Ljava/lang/Class;"))));
  }

  @Test public void testIR4() {
    // test some corner cases with try-finally
    doMethod(scope.findMethod(AnalysisScope.APPLICATION, "LcornerCases/TryFinally", Atom.findOrCreateUnicodeAtom("test1"),
        new ImmutableByteArray(UTF8Convert.toUTF8("(Ljava/io/InputStream;Ljava/io/InputStream;)V"))));
  }
}
