package com.ibm.wala.core.tests.ir;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cfg.ExceptionPrunedCFG;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import org.junit.jupiter.api.Test;

public class PrunedCFGTest {
  private static final TypeReference OWNER =
      TypeReference.findOrCreate(ClassLoaderReference.Application, "LPrunedCFGTest");
  private static final FieldReference EFFECT =
      FieldReference.findOrCreate(OWNER, Atom.findOrCreateUnicodeAtom("effect"), TypeReference.Int);
  private static final SSAInstructionFactory INSTRUCTIONS = JavaLanguage.get().instructionFactory();

  @Test
  public void filteringTheOnlyExitRemovesAllInteriorBlocks() {
    InducedCFG cfg =
        makeCFG(
            INSTRUCTIONS.GotoInstruction(0, 1),
            INSTRUCTIONS.PutInstruction(1, 1, EFFECT),
            INSTRUCTIONS.ThrowInstruction(2, 2));

    PrunedCFG<SSAInstruction, InducedCFG.BasicBlock> pruned = ExceptionPrunedCFG.make(cfg);

    assertThat(pruned).containsExactlyInAnyOrder(cfg.entry(), cfg.exit());
    assertThat(pruned).allSatisfy(block -> assertThat(pruned.getSuccNodeCount(block)).isZero());
  }

  @Test
  public void aNormalReturnPreservesTheBodyWhenExceptionalExitsAreFiltered() {
    InducedCFG cfg =
        makeCFG(
            INSTRUCTIONS.GotoInstruction(0, 1),
            INSTRUCTIONS.PutInstruction(1, 1, EFFECT),
            INSTRUCTIONS.ReturnInstruction(2));

    PrunedCFG<SSAInstruction, InducedCFG.BasicBlock> pruned = ExceptionPrunedCFG.make(cfg);

    assertThat(pruned).containsExactlyInAnyOrderElementsOf(cfg);
    assertThat(pruned.getNormalSuccessors(cfg.entry()))
        .containsExactly(cfg.getBlockForInstruction(1));
    assertThat(pruned.getNormalSuccessors(cfg.getBlockForInstruction(2)))
        .containsExactly(cfg.exit());
  }

  private static InducedCFG makeCFG(SSAInstruction... instructions) {
    MethodReference method =
        MethodReference.findOrCreate(OWNER, "test", "(ILjava/lang/Throwable;)V");
    return new InducedCFG(
        instructions, new SyntheticMethod(method, null, true, false), Everywhere.EVERYWHERE);
  }
}
