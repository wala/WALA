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

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.ImmutableByteArray;
import com.ibm.wala.util.UTF8Convert;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

/**
 * Test IR's getLocalNames.
 */
public class LocalNamesTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = LocalNamesTest.class.getClassLoader();
  
  private AnalysisScope scope;

  private ClassHierarchy cha;

  private AnalysisOptions options;
  
  private AnalysisCache cache;

  public static void main(String[] args) {
    justThisTest(LocalNamesTest.class);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {

    scope = new EMFScopeWrapper(TestConstants.WALA_TESTDATA, "J2SEClassHierarchyExclusions.xml", MY_CLASSLOADER);

    options = new AnalysisOptions(scope, null);
    cache = new AnalysisCache();
    ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions() );

    try {
      cha = ClassHierarchy.make(scope, factory);
    } catch (ClassHierarchyException e) {
      throw new Exception();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    scope = null;
    cha = null;
    super.tearDown();
  }

  /**
   * Build an IR, then check getLocalNames
   */
  public void testAliasNames() {
    try {
      AnalysisScope scope = new EMFScopeWrapper(TestConstants.WALA_TESTDATA, "J2SEClassHierarchyExclusions.xml", MY_CLASSLOADER);
      ClassHierarchy cha = ClassHierarchy.make(scope);
      TypeReference t = TypeReference.findOrCreateClass(scope.getApplicationLoader(), "cornerCases", "AliasNames");
      IClass klass = cha.lookupClass(t);
      assertTrue(klass != null);
      IMethod m = klass.getMethod(new Selector(Atom.findOrCreateAsciiAtom("foo"), Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V")));

      AnalysisOptions options = new AnalysisOptions();
      options.getSSAOptions().setUsePiNodes(true);
      IR ir = cache.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions() );

      for (int offsetIndex = 0; offsetIndex < ir.getInstructions().length; offsetIndex++) {
        SSAInstruction instr = ir.getInstructions()[offsetIndex];
        if (instr != null) {
          String[] localNames = ir.getLocalNames(offsetIndex, instr.getDef());
          if (localNames != null && localNames.length > 0 && localNames[0] == null) {
            Trace.println(ir);
            assertTrue(" getLocalNames() returned [null,...] for the def of instruction at offset " + offsetIndex + "\n\tinstr", false);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  public void testLocalNamesWithoutPiNodes() {
    boolean save = options.getSSAOptions().getUsePiNodes();
    options.getSSAOptions().setUsePiNodes(false);
    MethodReference mref = scope.findMethod(AnalysisScope.APPLICATION, "LcornerCases/Locals", Atom.findOrCreateUnicodeAtom("foo"),
        new ImmutableByteArray(UTF8Convert.toUTF8("([Ljava/lang/String;)V")));
    assertNotNull("method not found", mref);
    IMethod imethod = cha.resolveMethod(mref);
    assertNotNull("imethod not found", imethod);
    IR ir =  cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    options.getSSAOptions().setUsePiNodes(save);
  
    // v1 should be the parameter "a" at pc 0
    String[] names = ir.getLocalNames(0, 1);
    assertTrue("failed local name resolution for v1@0", names != null);
    assertTrue("incorrect number of local names for v1@0: " + names.length, names.length == 1);
    assertTrue("incorrect local name resolution for v1@0: " + names[0], names[0].equals("a"));
  
    // v2 is a compiler-induced temporary
    assertTrue("didn't expect name for v2 at pc 2", ir.getLocalNames(2, 2) == null);
  
    // at pc 5, v1 should represent the locals "a" and "b"
    names = ir.getLocalNames(5, 1);
    assertTrue("failed local name resolution for v1@5", names != null);
    assertTrue("incorrect number of local names for v1@5: " + names.length, names.length == 2);
    assertTrue("incorrect local name resolution #0 for v1@5: " + names[0], names[0].equals("a"));
    assertTrue("incorrect local name resolution #1 for v1@5: " + names[1], names[1].equals("b"));
  }

  public void testLocalNamesWithPiNodes() {
    boolean save = options.getSSAOptions().getUsePiNodes();
    options.getSSAOptions().setUsePiNodes(true);
    MethodReference mref = scope.findMethod(AnalysisScope.APPLICATION, "LcornerCases/Locals", Atom.findOrCreateUnicodeAtom("foo"),
        new ImmutableByteArray(UTF8Convert.toUTF8("([Ljava/lang/String;)V")));
    assertNotNull("method not found", mref);
    IMethod imethod = cha.resolveMethod(mref);
    assertNotNull("imethod not found", imethod);
    IR ir =  cache.getIRFactory().makeIR(imethod, Everywhere.EVERYWHERE, options.getSSAOptions());
    options.getSSAOptions().setUsePiNodes(save);
  
    // v1 should be the parameter "a" at pc 0
    String[] names = ir.getLocalNames(0, 1);
    assertTrue("failed local name resolution for v1@0", names != null);
    assertTrue("incorrect number of local names for v1@0: " + names.length, names.length == 1);
    assertTrue("incorrect local name resolution for v1@0: " + names[0], names[0].equals("a"));
  
    // v2 is a compiler-induced temporary
    assertTrue("didn't expect name for v2 at pc 2", ir.getLocalNames(2, 2) == null);
  
    // at pc 5, v1 should represent the locals "a" and "b"
    names = ir.getLocalNames(5, 1);
    assertTrue("failed local name resolution for v1@5", names != null);
    assertTrue("incorrect number of local names for v1@5: " + names.length, names.length == 2);
    assertTrue("incorrect local name resolution #0 for v1@5: " + names[0], names[0].equals("a"));
    assertTrue("incorrect local name resolution #1 for v1@5: " + names[1], names[1].equals("b"));
  }
}
