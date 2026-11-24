package com.ibm.wala.core.tests.ir;

import static com.ibm.wala.util.collections.IteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.types.TypeReference;
import java.util.Iterator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IRIteratorTest {
  private static ClassHierarchy cha;

  @BeforeAll
  static void makeHierarchy() throws Exception {
    cha =
        ClassHierarchyFactory.make(
            CallGraphTestUtil.makeJ2SEAnalysisScope(
                TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS));
  }

  @Test
  void normalIteratorThrowsAfterLastInstruction() {
    Iterator<SSAInstruction> iterator = makeIR("empty()V").iterateNormalInstructions();

    assertThat(iterator.next()).isNotNull();
    iterator.forEachRemaining(instruction -> assertThat(instruction).isNotNull());
    assertExhausted(iterator);
  }

  @Test
  void emptyPhiIteratorThrowsOnExhaustion() {
    assertExhausted(makeIR("empty()V").iteratePhis());
  }

  @Test
  void phiIteratorThrowsAfterLastInstruction() {
    Iterator<? extends SSAInstruction> iterator =
        makeIR("branch(Ljava/lang/Object;)I").iteratePhis();

    assertThat(iterator.next()).isInstanceOf(SSAPhiInstruction.class);
    iterator.forEachRemaining(
        instruction -> assertThat(instruction).isInstanceOf(SSAPhiInstruction.class));
    assertExhausted(iterator);
  }

  @Test
  void emptyPiIteratorThrowsOnExhaustion() {
    assertExhausted(makeIR("empty()V").iteratePis());
  }

  @Test
  void piIteratorThrowsAfterLastInstruction() {
    Iterator<? extends SSAInstruction> iterator =
        makeIR("branch(Ljava/lang/Object;)I").iteratePis();

    assertThat(iterator.next()).isInstanceOf(SSAPiInstruction.class);
    iterator.forEachRemaining(
        instruction -> assertThat(instruction).isInstanceOf(SSAPiInstruction.class));
    assertExhausted(iterator);
  }

  @Test
  void emptyCatchIteratorThrowsOnExhaustion() {
    assertExhausted(makeIR("empty()V").iterateCatchInstructions());
  }

  @Test
  void catchIteratorThrowsAfterLastInstruction() {
    Iterator<SSAInstruction> iterator = makeIR("catchException([I)I").iterateCatchInstructions();

    assertThat(iterator.next()).isInstanceOf(SSAGetCaughtExceptionInstruction.class);
    iterator.forEachRemaining(instruction ->
        assertThat(instruction).isInstanceOf(SSAGetCaughtExceptionInstruction.class));
    assertExhausted(iterator);
  }

  @Test
  void emptyCallSiteIteratorThrowsOnExhaustion() {
    assertExhausted(makeIR("empty()V").iterateCallSites());
  }

  @Test
  void callSiteIteratorThrowsAfterLastSite() {
    Iterator<CallSiteReference> iterator = makeIR("branch(Ljava/lang/Object;)I").iterateCallSites();

    assertThat(iterator.next().getDeclaredTarget().getName().toString()).isEqualTo("hashCode");
    assertExhausted(iterator);
  }

  @Test
  void allInstructionsIteratorPropagatesExhaustion() {
    Iterator<SSAInstruction> iterator = makeIR("empty()V").iterateAllInstructions();

    assertThat(iterator.next()).isNotNull();
    iterator.forEachRemaining(instruction -> assertThat(instruction).isNotNull());
    assertExhausted(iterator);
  }

  @Test
  void emptyBasicBlockIteratorThrowsOnExhaustion() {
    assertExhausted(makeIR("empty()V").getControlFlowGraph().entry().iterateNormalInstructions());
  }

  @Test
  void basicBlockIteratorStopsAtBlockBoundary() {
    IR ir = makeIR("branch(Ljava/lang/Object;)I");
    SSACFG.BasicBlock block = ir.getControlFlowGraph().getBlockForInstruction(0);
    Iterator<SSAInstruction> iterator = block.iterateNormalInstructions();

    assertThat(iterator.next()).isNotNull();
    iterator.forEachRemaining(instruction -> assertThat(instruction).isNotNull());
    assertExhausted(iterator);
  }

  @Test
  void emptyCaughtTypesIteratorThrowsOnExhaustion() {
    SSACFG cfg = makeIR("empty()V").getControlFlowGraph();
    SSACFG.ExceptionHandlerBasicBlock handler = cfg.new ExceptionHandlerBasicBlock(0);

    assertExhausted(handler.getCaughtExceptionTypes());
  }

  @Test
  void caughtTypesIteratorStopsBeforeUnusedCapacity() {
    SSACFG cfg = makeIR("empty()V").getControlFlowGraph();
    SSACFG.ExceptionHandlerBasicBlock handler = cfg.new ExceptionHandlerBasicBlock(0);
    handler.addCaughtExceptionType(TypeReference.JavaLangException);
    Iterator<TypeReference> iterator = handler.getCaughtExceptionTypes();

    assertThat(iterator.next()).isEqualTo(TypeReference.JavaLangException);
    assertExhausted(iterator);
  }

  private static IR makeIR(String selector) {
    IMethod method =
        cha.resolveMethod(StringStuff.makeMethodReference("cfg.IteratorSubject." + selector));
    assertThat(method).as("method %s", selector).isNotNull();
    SSAOptions options = new SSAOptions();
    options.setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
    return new AnalysisCacheImpl(options).getIR(method);
  }
}
