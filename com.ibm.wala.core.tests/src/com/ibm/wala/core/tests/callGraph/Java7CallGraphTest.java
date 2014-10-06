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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import com.ibm.wala.analysis.reflection.java7.MethodHandles;
import com.ibm.wala.core.tests.shrike.DynamicCallGraphTestBase;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.io.TemporaryFile;

public class Java7CallGraphTest extends DynamicCallGraphTestBase {
  
  @Test public void testOcamlHelloHash() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException, ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
   
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope("ocaml_hello_hash.txt", CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchy.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, "Lpack/ocamljavaMain");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    AnalysisCache cache = new AnalysisCache();
    
    SSAPropagationCallGraphBuilder builder = Util.makeZeroOneContainerCFABuilder(options, cache, cha, scope);
    
    builder.setContextSelector(new MethodHandles.ContextSelectorImpl(builder.getContextSelector()));
    builder.setContextInterpreter(new DelegatingSSAContextInterpreter(new MethodHandles.ContextInterpreterImpl(), builder.getCFAContextInterpreter()));
    
    CallGraph cg = builder.makeCallGraph(options, null); 
    
    File F = TemporaryFile.urlToFile("hello_hash_test_jar.jar", getClass().getClassLoader().getResource("hello_hash.jar"));
    F.deleteOnExit();
    instrument(F.getAbsolutePath());
    run("pack.ocamljavaMain", null);
    
    checkNodes(cg, new Predicate<MethodReference>() {
      @Override
      public boolean test(MethodReference t) {
        String s = t.toString();
        return s.contains("Lpack/") || s.contains("Locaml/stdlib/");
      }
    });
  }

}
