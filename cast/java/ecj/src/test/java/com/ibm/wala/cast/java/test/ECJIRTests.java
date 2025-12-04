package com.ibm.wala.cast.java.test;

import com.ibm.wala.cast.java.client.ECJJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * This class is a base class for IR tests for ECJ. It extends the base @IRTests rather
 * than @JavaIRTests because @JavaIRTests includes a bunch of particular unit tests, and the ECJ
 * subclasses of this test introduce their own tests and hence need a base class that does not have
 * its own tests and implements @getAnalysisEngine.
 */
abstract class ECJIRTests extends IRTests {

  @Override
  protected AbstractAnalysisEngine<InstanceKey, CallGraphBuilder<InstanceKey>, ?> getAnalysisEngine(
      final String[] mainClassDescriptors, Collection<Path> sources, List<String> libs) {
    JavaSourceAnalysisEngine engine =
        new ECJJavaSourceAnalysisEngine(getSSAOptions()) {
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
}
