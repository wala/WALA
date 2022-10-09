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
package com.ibm.wala.core.tests.ir;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 * tests for weird corner cases, such as when the input program doesn't verify
 *
 * @author sfink
 */
public class CornerCasesTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = CornerCasesTest.class.getClassLoader();

  /** test that getMethod() works even if a declared ancestor interface doesn't exist */
  @Test
  public void testBug38484() throws ClassHierarchyException, IOException {

    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA,
            new FileProvider().getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS),
            MY_CLASSLOADER);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    TypeReference t =
        TypeReference.findOrCreateClass(
            scope.getApplicationLoader(), "cornerCases", "YuckyInterface");
    IClass klass = cha.lookupClass(t);
    Assert.assertNotNull(klass);
    IMethod m =
        klass.getMethod(
            new Selector(Atom.findOrCreateAsciiAtom("x"), Descriptor.findOrCreateUTF8("()V")));
    Assert.assertNull(m);
  }

  /**
   * test that type inference works in the presence of a getfield where the field's declared type
   * cannot be loaded
   */
  @Test
  public void testBug38540() throws ClassHierarchyException, IOException {

    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA,
            new FileProvider().getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS),
            MY_CLASSLOADER);
    AnalysisOptions options = new AnalysisOptions();
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    TypeReference t =
        TypeReference.findOrCreateClass(scope.getApplicationLoader(), "cornerCases", "Main");
    IClass klass = cha.lookupClass(t);
    Assert.assertNotNull(klass);
    ShrikeCTMethod m =
        (ShrikeCTMethod)
            klass.getMethod(
                new Selector(
                    Atom.findOrCreateAsciiAtom("foo"),
                    Descriptor.findOrCreateUTF8("()Ljava/lang/Object;")));
    Assert.assertNotNull(m);
    IR ir =
        new AnalysisCacheImpl()
            .getSSACache()
            .findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions());
    TypeInference.make(ir, false);
  }
}
