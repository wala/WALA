/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.ibm.wala.cast.java.test;

import java.util.Collection;
import java.util.List;

import com.ibm.wala.cast.java.client.ECJJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class ECJJavaIRTest extends JavaIRTests {
  
  public ECJJavaIRTest() {
    super(null);
  }

  @Override
  protected <I extends InstanceKey> AbstractAnalysisEngine<I> getAnalysisEngine(final String[] mainClassDescriptors, Collection<String> sources, List<String> libs) {
    JavaSourceAnalysisEngine<I> engine = new ECJJavaSourceAnalysisEngine<I>() {
      @Override
      protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
        return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE, cha, mainClassDescriptors);
      }
    };
    engine.setExclusionsFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    populateScope(engine, sources, libs);
    return engine;
  }

}
