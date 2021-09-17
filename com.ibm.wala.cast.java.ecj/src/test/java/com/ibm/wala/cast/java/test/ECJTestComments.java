package com.ibm.wala.cast.java.test;

import com.ibm.wala.cast.java.client.ECJJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.junit.Test;

public class ECJTestComments extends IRTests {

  public ECJTestComments() {
    super(null);
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

  protected static final MethodReference testMethod =
      MethodReference.findOrCreate(
          TypeReference.findOrCreate(
              JavaSourceAnalysisScope.SOURCE, TypeName.string2TypeName("LComments")),
          Atom.findOrCreateUnicodeAtom("main"),
          Descriptor.findOrCreateUTF8(Language.JAVA, "([Ljava/lang/String;)V"));

  @Test
  public void testComments() throws IllegalArgumentException, CancelException, IOException {
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> result =
        runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
    for (CGNode node : result.fst.getNodes(testMethod)) {
      if (node.getMethod() instanceof AstMethod) {
        AstMethod m = (AstMethod) node.getMethod();
        DebuggingInformation dbg = m.debugInfo();
        for (SSAInstruction inst : node.getIR().getInstructions()) {
          System.err.println("leading for " + inst.toString(node.getIR().getSymbolTable()));
          System.err.println(dbg.getLeadingComment(inst.iIndex()));
          System.err.println("following for " + inst.toString(node.getIR().getSymbolTable()));
          System.err.println(dbg.getFollowingComment(inst.iIndex()));
        }
      }
    }
  }
}
