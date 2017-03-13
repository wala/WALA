package com.ibm.wala.core.tests.arraybounds;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;

import java.io.IOException;
import java.util.LinkedHashSet;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.ibm.wala.analysis.arraybounds.ArrayOutOfBoundsAnalysis;
import com.ibm.wala.analysis.nullpointer.IntraproceduralNullPointerAnalysis;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter2EdgeFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.filter.ArrayOutOfBoundFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.filter.CombinedExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.filter.NullPointerExceptionFilter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.AllIntegerDueToBranchePiPolicy;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.AnalysisScopeReader;

/**
 * This test will check that:
 * <ul>
 * <li>no normal edge is removed
 * <li>in case of an exceptional edge being removed, the instruction causing the
 * edge is an instruction that is able to throw a NullpointerException or an
 * ArrayIndexOutOfBoundException and no other exception may be thrown by this
 * instruction.
 * <li>the number of removed edges is as expected
 * </ul>
 * 
 * So there is no explicit check for specific lines.
 * 
 * For an example how to use the exception pruning see
 * {@link PruneArrayOutOfBoundExceptionEdge#computeCfgAndPrunedCFG(IMethod)}
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class PruneArrayOutOfBoundExceptionEdge {
  private static ClassLoader CLASS_LOADER = PruneArrayOutOfBoundExceptionEdge.class.getClassLoader();

  private static final String DETECTABLE_TESTDATA = "Larraybounds/Detectable";
  /**
   * The number of Basic Blocks, which have an exception edge, that should be
   * removed. (#[array access] + #[other])
   */
  private static final int DETECTABLE_EXPECTED_COUNT = 34 + 2;

  private static final String NOT_DETECTABLE_TESTDATA = "Larraybounds/NotDetectable";
  /**
   * The number of Basic Blocks, which have an exception edge, that should be
   * removed. (#[array access] + #[other])
   */
  private static final int NOT_DETECTABLE_EXPECTED_COUNT = 0 + 3;

  private static final String NOT_IN_BOUND_TESTDATA = "Larraybounds/NotInBound";
  /**
   * The number of Basic Blocks, which have an exception edge, that should be
   * removed. (#[array access] + #[other])
   */
  private static final int NOT_IN_BOUND_EXPECTED_COUNT = 0 + 1;

  private static IRFactory<IMethod> irFactory;
  private static AnalysisOptions options;
  private static AnalysisScope scope;
  private static ClassHierarchy cha;

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @BeforeClass
  public static void init() throws IOException, ClassHierarchyException {
    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, null, CLASS_LOADER);
    cha = ClassHierarchyFactory.make(scope);

    irFactory = new DefaultIRFactory();
    options = new AnalysisOptions();
    options.getSSAOptions().setPiNodePolicy(new AllIntegerDueToBranchePiPolicy());
  }

  private static IR getIr(IMethod method) {
    return irFactory.makeIR(method, Everywhere.EVERYWHERE, options.getSSAOptions());
  }

  public static Pair<SSACFG, PrunedCFG<SSAInstruction, ISSABasicBlock>> computeCfgAndPrunedCFG(IMethod method) {
    IR ir = getIr(method);
    SSACFG cfg = ir.getControlFlowGraph();

    ArrayOutOfBoundsAnalysis arrayBoundsAnalysis = new ArrayOutOfBoundsAnalysis(ir);
    IntraproceduralNullPointerAnalysis nullPointerAnalysis = new IntraproceduralNullPointerAnalysis(ir);

    CombinedExceptionFilter<SSAInstruction> filter = new CombinedExceptionFilter<>();
    filter.add(new ArrayOutOfBoundFilter(arrayBoundsAnalysis));
    filter.add(new NullPointerExceptionFilter(nullPointerAnalysis));

    ExceptionFilter2EdgeFilter<ISSABasicBlock> edgeFilter = new ExceptionFilter2EdgeFilter<>(filter, cha, cfg);
    PrunedCFG<SSAInstruction, ISSABasicBlock> prunedCfg = PrunedCFG.make(cfg, edgeFilter);

    return Pair.make(cfg, prunedCfg);
  }

  private static IClass getIClass(String name) {
    final TypeReference typeRef = TypeReference.findOrCreate(scope.getApplicationLoader(), name);
    return cha.lookupClass(typeRef);
  }

  @Test
  public void detectable() throws ClassNotFoundException {
    IClass iClass = getIClass(DETECTABLE_TESTDATA);
    checkRemovedEdges(iClass, DETECTABLE_EXPECTED_COUNT);
  }

  @Test
  public void notDetectable() {
    IClass iClass = getIClass(NOT_DETECTABLE_TESTDATA);
    checkRemovedEdges(iClass, NOT_DETECTABLE_EXPECTED_COUNT);
  }

  @Test
  public void notInBound() {
    IClass iClass = getIClass(NOT_IN_BOUND_TESTDATA);
    checkRemovedEdges(iClass, NOT_IN_BOUND_EXPECTED_COUNT);
  }

  private void checkRemovedEdges(IClass iClass, int expectedNumberOfArrayAccesses) {
    int numberOfDeletedExceptionEdges = 0;
    for (IMethod method : iClass.getAllMethods()) {
      if (method.getDeclaringClass().equals(iClass)) {
        String identifyer = method.getDeclaringClass().getName().toString() + "#" + method.getName().toString();
        Pair<SSACFG, PrunedCFG<SSAInstruction, ISSABasicBlock>> cfgs = computeCfgAndPrunedCFG(method);
        SSACFG cfg = cfgs.fst;
        PrunedCFG<SSAInstruction, ISSABasicBlock> prunedCfg = cfgs.snd;

        for (ISSABasicBlock block : cfg) {
          checkNormalSuccessors(cfg, prunedCfg, block);
          boolean isEdgeRemoved = checkExceptionalSuccessors(block, cfg, prunedCfg, method, identifyer);
          numberOfDeletedExceptionEdges += isEdgeRemoved ? 1 : 0;
        }
      }
    }

    /*
     * Possible reasons for this to fail are:
     * 
     * 
     * *_NUMBER_OF_BB_WITHOUT_EXCEPTION is not set to the correct value (maybe
     * the test data has changed).
     * 
     * Not all methods of the class analyzed.
     * 
     * There is a bug, so not all edges are deleted.
     */
    collector.checkThat("Number of deleted edges is not as expected for " + iClass.getName().toString(),
        numberOfDeletedExceptionEdges, equalTo(expectedNumberOfArrayAccesses));
  }

  /**
   * Check in case of an exceptional edge being removed, the instruction causing
   * the edge is an instruction that is able to throw a NullpointerException or
   * an ArrayIndexOutOfBoundException and no other exception may be thrown by
   * this instruction.
   * 
   * @param block
   * @param cfg
   * @param prunedCfg
   * @param method
   * @param identifyer
   * @return if an edge of block was removed
   */
  private boolean checkExceptionalSuccessors(ISSABasicBlock block, SSACFG cfg, PrunedCFG<SSAInstruction, ISSABasicBlock> prunedCfg,
      IMethod method, String identifyer) {
    boolean isEdgeRemoved = false;
    LinkedHashSet<ISSABasicBlock> exceptionalSuccessorCfg = new LinkedHashSet<>(cfg.getExceptionalSuccessors(block));
    LinkedHashSet<ISSABasicBlock> exceptionalSuccessorPruned = new LinkedHashSet<>(
        prunedCfg.getExceptionalSuccessors(block));

    if (!exceptionalSuccessorCfg.equals(exceptionalSuccessorPruned)) {
      isEdgeRemoved = true;

      if (block.getLastInstructionIndex() >= 0) {
        SSAInstruction lastInstruction = block.getLastInstruction();
        lastInstruction.getExceptionTypes();

        Matcher<Iterable<? super TypeReference>> matcher1 = anyOf(hasItem(equalTo(TypeReference.JavaLangNullPointerException)),
            hasItem(equalTo(TypeReference.JavaLangArrayIndexOutOfBoundsException)));

        collector.checkThat("Edge deleted but cause instruction can't throw NullPointerException"
            + "nor ArrayIndexOutOfBoundsException: " + identifyer + ":" + method.getLineNumber(lastInstruction.iindex),
            lastInstruction.getExceptionTypes(), matcher1);

        Matcher<Iterable<TypeReference>> matcher2 = everyItem(anyOf(equalTo(TypeReference.JavaLangNullPointerException),
            equalTo(TypeReference.JavaLangArrayIndexOutOfBoundsException)));

        collector.checkThat("Edge deleted but cause instruction throws other exceptions as NullPointerException"
            + "and ArrayIndexOutOfBoundsException: " + identifyer + ":" + method.getLineNumber(lastInstruction.iindex),
            lastInstruction.getExceptionTypes(), matcher2);

      } else {
        collector.addError(new Throwable("Exceptional edge deleted, but no instruction as cause. - No last instruction."));
      }

    }
    return isEdgeRemoved;
  }

  private void checkNormalSuccessors(SSACFG cfg, PrunedCFG<SSAInstruction, ISSABasicBlock> prunedCfg, ISSABasicBlock block) {
    LinkedHashSet<ISSABasicBlock> normalSuccessorCfg = new LinkedHashSet<>(cfg.getNormalSuccessors(block));
    LinkedHashSet<ISSABasicBlock> normalSuccessorPruned = new LinkedHashSet<>(prunedCfg.getNormalSuccessors(block));
    collector.checkThat("", normalSuccessorPruned, equalTo(normalSuccessorCfg));
  }

  @AfterClass
  public static void free() throws IOException, ClassHierarchyException {
    scope = null;
    cha = null;
    irFactory = null;
    options = null;
  }
}
