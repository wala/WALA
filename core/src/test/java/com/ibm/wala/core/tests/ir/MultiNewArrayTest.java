/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.tests.ir;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
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
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class MultiNewArrayTest extends WalaTestCase {

  private static final ClassLoader MY_CLASSLOADER = MultiNewArrayTest.class.getClassLoader();

  @Test
  public void testMultiNewArray1() throws IOException, ClassHierarchyException {

    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA,
            new FileProvider().getFile("J2SEClassHierarchyExclusions.txt"),
            MY_CLASSLOADER);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    IClass klass =
        cha.lookupClass(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, TestConstants.MULTI_DIM_MAIN));
    assertThat(klass).isNotNull();
    IMethod m = klass.getMethod(Selector.make(Language.JAVA, "testNewMultiArray()V"));
    assertThat(m).isNotNull();
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    IR ir = cache.getIRFactory().makeIR(m, Everywhere.EVERYWHERE, new SSAOptions());
    assertThat(ir).isNotNull();
    SSAInstruction[] instructions = ir.getInstructions();
    for (SSAInstruction instr : instructions) {
      if (instr instanceof SSANewInstruction) {
        System.err.println(instr.toString(ir.getSymbolTable()));
        assertThat(instr.getNumberOfUses()).isEqualTo(2);
        assertThat(ir.getSymbolTable().getIntValue(instr.getUse(0))).isEqualTo(3);
        assertThat(ir.getSymbolTable().getIntValue(instr.getUse(1))).isEqualTo(4);
      }
    }
  }
}
