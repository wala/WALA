package com.ibm.wala.cast.java.test;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import java.io.IOException;
import org.junit.Test;

public abstract class Issue667Test extends IRTests {

  public Issue667Test(String projectName) {
    super(projectName);
  }

  @Test
  public void testDominanceFrontierCase() throws CancelException, IOException {
    Pair<CallGraph, ?> result =
        runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);

    MethodReference cm =
        MethodReference.findOrCreate(
            TypeReference.findOrCreate(
                JavaSourceAnalysisScope.SOURCE, TypeName.string2TypeName("LDominanceFrontierCase")),
            Atom.findOrCreateUnicodeAtom("convert"),
            Descriptor.findOrCreateUTF8(Language.JAVA, "(Ljava/lang/Integer;)I"));
    result
        .fst
        .getNodes(cm)
        .forEach(
            n -> {
              try {
                ControlDependenceGraph<ISSABasicBlock> cdg =
                    new ControlDependenceGraph<>(n.getIR().getControlFlowGraph());
                assert cdg != null;
              } catch (IllegalArgumentException e) {
                System.err.println(n.getIR());
                throw e;
              }
            });
  }
}
