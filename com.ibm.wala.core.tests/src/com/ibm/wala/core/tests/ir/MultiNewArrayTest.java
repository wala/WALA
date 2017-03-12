/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.ir;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

public class MultiNewArrayTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = MultiNewArrayTest.class.getClassLoader();
  
  @Test public void testMultiNewArray1() throws IOException, ClassHierarchyException {
    AnalysisScope scope = null;
    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"), MY_CLASSLOADER);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    IClass klass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, TestConstants.MULTI_DIM_MAIN));
    Assert.assertTrue(klass != null);
    IMethod m = klass.getMethod(Selector.make(Language.JAVA, "testNewMultiArray()V"));
    Assert.assertTrue(m != null);
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    IR ir = cache.getIRFactory().makeIR(m, Everywhere.EVERYWHERE, new SSAOptions());
    Assert.assertTrue(ir != null);
    SSAInstruction[] instructions = ir.getInstructions();
    for (SSAInstruction instr : instructions) {
      if (instr instanceof SSANewInstruction) {
        System.err.println(instr.toString(ir.getSymbolTable()));
        Assert.assertTrue(instr.getNumberOfUses() == 2);
        Assert.assertTrue(ir.getSymbolTable().getIntValue(instr.getUse(0)) == 3);
        Assert.assertTrue(ir.getSymbolTable().getIntValue(instr.getUse(1)) == 4);
      }
    }
  }
}
