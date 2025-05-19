/*
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.ibm.wala.cast.java.test;

//import static com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope.SOURCE;

import com.ibm.wala.cast.java.client.ECJJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ECJJava8IRTest extends IRTests {

  private static final String packageName = "javaeight";

  public ECJJava8IRTest() {
    super(null);
    dump = true;
  }

  @Override
  protected AbstractAnalysisEngine<InstanceKey, CallGraphBuilder<InstanceKey>, ?> getAnalysisEngine(
      final String[] mainClassDescriptors, Collection<String> sources, List<String> libs) {
    JavaSourceAnalysisEngine engine =
        new ECJJavaSourceAnalysisEngine() {
          @Override
          protected Iterable<Entrypoint> makeDefaultEntrypoints(IClassHierarchy cha) {
            return Util.makeMainEntrypoints(
                JavaSourceAnalysisScope.SOURCE, cha, mainClassDescriptors);
          }
        };
    engine.setExclusionsFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    populateScope(engine, sources, libs);
    return engine;
  }

  @Test
  public void testEmptyLambda() throws IllegalArgumentException, CancelException, IOException {
	  
	  /*
	   * TODO: The following test only partially works. The lambda subclass type is
	   * 	   returned in the IR results, but the body of the lambda function is not.
	   * 	   Consider using the AnonymousClassDeclaration type for creating the
	   * 	   function body.
	   */
	  Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> lambdaTest = runTest(
			  singlePkgTestSrc(packageName),
			  rtJar,
			  simplePkgTestEntryPoint(packageName),
			  emptyList,
			  true,
			  null
			  );
	  System.out.println(lambdaTest.fst.getClassHierarchy());
  }
}
