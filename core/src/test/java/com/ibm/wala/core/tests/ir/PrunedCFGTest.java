package com.ibm.wala.core.tests.ir;

import static com.ibm.wala.cfg.MinimalCFGAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.ipa.cfg.ExceptionPrunedCFG;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class PrunedCFGTest extends WalaTestCase {
  private final IClassHierarchy cha = AnnotationTest.makeCHA();

  public PrunedCFGTest() throws IOException, ClassHierarchyException {}

  @Test
  public void filteringTheOnlyExitRemovesAllInteriorBlocks() {
    SSACFG cfg = makeCFG("cfg.Pruning.writeThenThrow(ILjava/lang/RuntimeException;)V");

    PrunedCFG<SSAInstruction, ISSABasicBlock> pruned = ExceptionPrunedCFG.make(cfg);

    assertThat(pruned).nodes().containsExactlyInAnyOrder(cfg.entry(), cfg.exit());
    assertThat(pruned)
        .nodes()
        .allSatisfy(block -> assertThat(pruned).successorCountOf(block).isZero());
  }

  @Test
  public void aNormalReturnPreservesTheBodyWhenExceptionalExitsAreFiltered() {
    SSACFG cfg = makeCFG("cfg.Pruning.writeThenReturn(I)V");

    PrunedCFG<SSAInstruction, ISSABasicBlock> pruned = ExceptionPrunedCFG.make(cfg);

    assertThat(pruned).nodes().containsExactlyInAnyOrderElementsOf(cfg);
    assertThat(pruned)
        .nodes()
        .allSatisfy(block -> {
          assertThat(pruned)
              .normalSuccessorsOf(block)
              .containsExactlyInAnyOrderElementsOf(cfg.getNormalSuccessors(block));
          assertThat(pruned).exceptionalSuccessorsOf(block).isEmpty();
        });
  }

  private SSACFG makeCFG(String methodSignature) {
    IMethod method = cha.resolveMethod(StringStuff.makeMethodReference(methodSignature));
    assertThat(method).as("method %s", methodSignature).isNotNull();
    return makeAnalysisCache().getIR(method).getControlFlowGraph();
  }
}
