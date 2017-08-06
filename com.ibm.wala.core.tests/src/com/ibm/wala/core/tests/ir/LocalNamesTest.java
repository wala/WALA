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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IClass;
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
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPiNodePolicy;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;
import com.ibm.wala.util.strings.UTF8Convert;

/**
 * Test IR's getLocalNames.
 */
public class LocalNamesTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = LocalNamesTest.class.getClassLoader();

  private static AnalysisScope scope;

  private static ClassHierarchy cha;

  private static AnalysisOptions options;

  public static void main(String[] args) {
    justThisTest(LocalNamesTest.class);
  }

  /*
   * @see junit.framework.TestCase#setUp()
   */
  @BeforeClass
  public static void beforeClass() throws Exception {

    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA,
        (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"), MY_CLASSLOADER);

    options = new AnalysisOptions(scope, null);
    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions());

    try {
      cha = ClassHierarchyFactory.make(scope, factory);
    } catch (ClassHierarchyException e) {
      throw new Exception(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#tearDown()
   */
  @AfterClass
  public static void afterClass() throws Exception {
    scope = null;
    cha = null;
    options = null;
  }

  /**
   * Build an IR, then check getLocalNames
   */
  @Test
  public void testAliasNames() {
    try {
      AnalysisScope scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, (new FileProvider())
          .getFile("J2SEClassHierarchyExclusions.txt"), MY_CLASSLOADER);
      ClassHierarchy cha = ClassHierarchyFactory.make(scope);
      TypeReference t = TypeReference.findOrCreateClass(scope.getApplicationLoader(), "cornerCases", "AliasNames");
      IClass klass = cha.lookupClass(t);
      Assert.assertTrue(klass != null);
      IMethod m = klass.getMethod(new Selector(Atom.findOrCreateAsciiAtom("foo"), Descriptor
          .findOrCreateUTF8("([Ljava/lang/String;)V")));

      AnalysisOptions options = new AnalysisOptions();
      options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
      
      IAnalysisCacheView cache = new AnalysisCacheImpl(options.getSSAOptions());

      IR ir = cache.getIR(m, Everywhere.EVERYWHERE);

      for (int offsetIndex = 0; offsetIndex < ir.getInstructions().length; offsetIndex++) {
        SSAInstruction instr = ir.getInstructions()[offsetIndex];
        if (instr != null) {
          String[] localNames = ir.getLocalNames(offsetIndex, instr.getDef());
          if (localNames != null && localNames.length > 0 && localNames[0] == null) {
            System.err.println(ir);
            Assert.assertTrue(" getLocalNames() returned [null,...] for the def of instruction at offset " + offsetIndex
                + "\n\tinstr", false);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  @Test
  public void testLocalNamesWithoutPiNodes() {
    SSAPiNodePolicy save = options.getSSAOptions().getPiNodePolicy();
    options.getSSAOptions().setPiNodePolicy(null);
    MethodReference mref = scope.findMethod(AnalysisScope.APPLICATION, "LcornerCases/Locals", Atom.findOrCreateUnicodeAtom("foo"),
        new ImmutableByteArray(UTF8Convert.toUTF8("([Ljava/lang/String;)V")));
    Assert.assertNotNull("method not found", mref);
    IMethod imethod = cha.resolveMethod(mref);
    Assert.assertNotNull("imethod not found", imethod);
    IAnalysisCacheView cache = new AnalysisCacheImpl(options.getSSAOptions());
    IR ir = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    options.getSSAOptions().setPiNodePolicy(save);

    // v1 should be the parameter "a" at pc 0
    String[] names = ir.getLocalNames(0, 1);
    Assert.assertTrue("failed local name resolution for v1@0", names != null);
    Assert.assertTrue("incorrect number of local names for v1@0: " + names.length, names.length == 1);
    Assert.assertTrue("incorrect local name resolution for v1@0: " + names[0], names[0].equals("a"));

    // v2 is a compiler-induced temporary
    Assert.assertTrue("didn't expect name for v2 at pc 2", ir.getLocalNames(2, 2) == null);

    // at pc 5, v1 should represent the locals "a" and "b"
    names = ir.getLocalNames(5, 1);
    Assert.assertTrue("failed local name resolution for v1@5", names != null);
    Assert.assertTrue("incorrect number of local names for v1@5: " + names.length, names.length == 2);
    Assert.assertTrue("incorrect local name resolution #0 for v1@5: " + names[0], names[0].equals("a"));
    Assert.assertTrue("incorrect local name resolution #1 for v1@5: " + names[1], names[1].equals("b"));
  }

  @Test
  public void testLocalNamesWithPiNodes() {
    SSAPiNodePolicy save = options.getSSAOptions().getPiNodePolicy();
    options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
    MethodReference mref = scope.findMethod(AnalysisScope.APPLICATION, "LcornerCases/Locals", Atom.findOrCreateUnicodeAtom("foo"),
        new ImmutableByteArray(UTF8Convert.toUTF8("([Ljava/lang/String;)V")));
    Assert.assertNotNull("method not found", mref);
    IMethod imethod = cha.resolveMethod(mref);
    Assert.assertNotNull("imethod not found", imethod);
    IAnalysisCacheView cache = new AnalysisCacheImpl(options.getSSAOptions());
    IR ir = cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    options.getSSAOptions().setPiNodePolicy(save);

    // v1 should be the parameter "a" at pc 0
    String[] names = ir.getLocalNames(0, 1);
    Assert.assertTrue("failed local name resolution for v1@0", names != null);
    Assert.assertTrue("incorrect number of local names for v1@0: " + names.length, names.length == 1);
    Assert.assertTrue("incorrect local name resolution for v1@0: " + names[0], names[0].equals("a"));

    // v2 is a compiler-induced temporary
    Assert.assertTrue("didn't expect name for v2 at pc 2", ir.getLocalNames(2, 2) == null);

    // at pc 5, v1 should represent the locals "a" and "b"
    names = ir.getLocalNames(5, 1);
    Assert.assertTrue("failed local name resolution for v1@5", names != null);
    Assert.assertTrue("incorrect number of local names for v1@5: " + names.length, names.length == 2);
    Assert.assertTrue("incorrect local name resolution #0 for v1@5: " + names[0], names[0].equals("a"));
    Assert.assertTrue("incorrect local name resolution #1 for v1@5: " + names[1], names[1].equals("b"));
  }
}
