/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.core.tests.callGraph;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import com.ibm.wala.analysis.reflection.java7.MethodHandles;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ResourceJarFileModule;
import com.ibm.wala.core.tests.shrike.DynamicCallGraphTestBase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.strings.Atom;

public class KawaCallGraphTest extends DynamicCallGraphTestBase {

  @Test
  public void testKawaChess() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException, SecurityException {   
    CallGraph CG = testKawa(new ResourceJarFileModule(getClass().getClassLoader().getResource("kawachess.jar")), "main");
    
    Set<CGNode> status = getNodes(CG, "Lchess", "startingStatus", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    assert ! status.isEmpty();
    
    Set<CGNode> color = getNodes(CG, "Lchess", "startingColor", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    assert ! color.isEmpty();
    
  }

  @Test
  public void testKawaTest() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException, SecurityException {   
    CallGraph CG = testKawa(new ResourceJarFileModule(getClass().getClassLoader().getResource("kawatest.jar")), "test");
    
    Set<CGNode> nodes = getNodes(CG, "Ltest", "plusish$V", "(Lgnu/lists/LList;)Ljava/lang/Object;");
    assert ! nodes.isEmpty();
  }

  private static Set<CGNode> getNodes(CallGraph CG, String cls, String method, String descr) {
    Set<CGNode> nodes = CG.getNodes(MethodReference.findOrCreate(TypeReference.find(ClassLoaderReference.Application, cls), Atom.findOrCreateUnicodeAtom(method), Descriptor.findOrCreateUTF8(descr)));
    return nodes;
  }

  public CallGraph testKawa(Module code, String main) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException, SecurityException {   
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope("base.txt", CallGraphTestUtil.REGRESSION_EXCLUSIONS_FOR_GUI);
    scope.addToScope(ClassLoaderReference.Application, new ResourceJarFileModule(getClass().getClassLoader().getResource("kawa.jar")));
    scope.addToScope(ClassLoaderReference.Application, code);
    
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, "L" + main);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    
    options.setReflectionOptions(ReflectionOptions.NONE);
    options.setTraceStringConstants(true);
    options.setUseConstantSpecificKeys(true);
    
    SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);

    MethodHandles.analyzeMethodHandles(options, builder);

    return builder.makeCallGraph(options, null); 
   }

}
