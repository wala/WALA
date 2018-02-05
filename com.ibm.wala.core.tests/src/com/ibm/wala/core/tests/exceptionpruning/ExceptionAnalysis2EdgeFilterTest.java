package com.ibm.wala.core.tests.exceptionpruning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.ibm.wala.analysis.exceptionanalysis.ExceptionAnalysis;
import com.ibm.wala.analysis.exceptionanalysis.ExceptionAnalysis2EdgeFilter;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.EdgeFilter;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter2EdgeFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.filter.IgnoreExceptionsFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.CombinedInterproceduralExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.IgnoreExceptionsInterFilter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.AllIntegerDueToBranchePiPolicy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.ref.ReferenceCleanser;

/**
 * This Test checks, if the number of deleted edges is correct for TestPruning,
 * it is also doing a plausibility check for deleted edges (only edges after
 * exceptional instructions should be deleted) and that no new edges are
 * inserted.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class ExceptionAnalysis2EdgeFilterTest {
  private static ClassLoader CLASS_LOADER = ExceptionAnalysis2EdgeFilterTest.class.getClassLoader();
  public static String REGRESSION_EXCLUSIONS = "Java60RegressionExclusions.txt";

  private static ClassHierarchy cha;
  private static CallGraph cg;
  private static PointerAnalysis<InstanceKey> pointerAnalysis;
  private static CombinedInterproceduralExceptionFilter<SSAInstruction> filter;

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @BeforeClass
  public static void init() throws IOException, ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException {
    AnalysisOptions options;
    AnalysisScope scope;

    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, new File(REGRESSION_EXCLUSIONS), CLASS_LOADER);
    cha = ClassHierarchyFactory.make(scope);

    Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha, "Lexceptionpruning/TestPruning");
    options = new AnalysisOptions(scope, entrypoints);
    options.getSSAOptions().setPiNodePolicy(new AllIntegerDueToBranchePiPolicy());

    ReferenceCleanser.registerClassHierarchy(cha);
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    ReferenceCleanser.registerCache(cache);
    CallGraphBuilder<InstanceKey> builder = Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);
    cg = builder.makeCallGraph(options, null);
    pointerAnalysis = builder.getPointerAnalysis();

    /*
     * We will ignore some exceptions to focus on the exceptions we want to
     * raise (OwnException, ArrayIndexOutOfBoundException)
     */
    filter = new CombinedInterproceduralExceptionFilter<>();
    filter.add(new IgnoreExceptionsInterFilter<>(new IgnoreExceptionsFilter(TypeReference.JavaLangOutOfMemoryError)));
    filter.add(new IgnoreExceptionsInterFilter<>(new IgnoreExceptionsFilter(
        TypeReference.JavaLangNullPointerException)));
    filter.add(new IgnoreExceptionsInterFilter<>(new IgnoreExceptionsFilter(
        TypeReference.JavaLangExceptionInInitializerError)));
    filter.add(new IgnoreExceptionsInterFilter<>(new IgnoreExceptionsFilter(
        TypeReference.JavaLangNegativeArraySizeException)));
  }

  @Test
  public void test() {
    HashMap<String, Integer> deletedExceptional = new HashMap<>();
    int deletedNormal = 0;

    ExceptionAnalysis analysis = new ExceptionAnalysis(cg, pointerAnalysis, cha, filter);
    analysis.solve();

    for (CGNode node : cg) {
      if (node.getIR() != null && !node.getIR().isEmptyIR()) {
        EdgeFilter<ISSABasicBlock> exceptionAnalysedEdgeFilter = new ExceptionAnalysis2EdgeFilter(analysis, node);

        SSACFG cfg_orig = node.getIR().getControlFlowGraph();
        ExceptionFilter2EdgeFilter<ISSABasicBlock> filterOnlyEdgeFilter = new ExceptionFilter2EdgeFilter<>(
            filter.getFilter(node), cha, cfg_orig);
        ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = PrunedCFG.make(cfg_orig, filterOnlyEdgeFilter);
        ControlFlowGraph<SSAInstruction, ISSABasicBlock> exceptionPruned = PrunedCFG.make(cfg_orig, exceptionAnalysedEdgeFilter);

        for (ISSABasicBlock block : cfg) {
          if (exceptionPruned.containsNode(block)) {
            for (ISSABasicBlock normalSucc : cfg.getNormalSuccessors(block)) {
              if (!exceptionPruned.getNormalSuccessors(block).contains(normalSucc)) {
                checkRemovingNormalOk(node, cfg, block, normalSucc);
                if (node.getMethod().getDeclaringClass().getName().getClassName().toString().equals("TestPruning")) {
                  deletedNormal += 1;
                }
              }
            }

            for (ISSABasicBlock exceptionalSucc : cfg.getExceptionalSuccessors(block)) {
              if (!exceptionPruned.getExceptionalSuccessors(block).contains(exceptionalSucc)) {
                if (node.getMethod().getDeclaringClass().getName().getClassName().toString().equals("TestPruning")) {
                  boolean count = true;
                  SSAInstruction instruction = block.getLastInstruction();
                  if (instruction instanceof SSAInvokeInstruction && ((SSAInvokeInstruction) instruction).isSpecial()) {
                    count = false;
                  }

                  if (count) {
                    Integer value = 0;
                    String key = node.getMethod().getName().toString();
                    if (deletedExceptional.containsKey(key)) {
                      value = deletedExceptional.get(key);
                    }
                    deletedExceptional.put(key, value + 1);
                  }
                }
              }
            }
          }
        }

        checkNoNewEdges(cfg, exceptionPruned);
      }
    }

    assertEquals("Number of normal edges deleted wrong:", 0, deletedNormal);
    for (String key : deletedExceptional.keySet()) {
      int value = deletedExceptional.get(key);
      String text = "Number of exceptional edges deleted wrong for " + key + ":";
      if (key.equals("testTryCatchMultipleExceptions")) {
        assertEquals(text, 12, value);
      } else if (key.equals("testTryCatchOwnException")) {
        assertEquals(text, 5, value);
      } else if (key.equals("testTryCatchSuper")) {
        assertEquals(text, 3, value);
      } else if (key.equals("testTryCatchImplicitException")) {
        assertEquals(text, 5, value);
      } else if (key.equals("main")) {
        assertEquals(text, 4, value);
      } else {
        assertEquals(text, 0, value);
      }
    }
  }

  private static void checkRemovingNormalOk(CGNode node, ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg, ISSABasicBlock block,
      ISSABasicBlock normalSucc) {
    if (!block.getLastInstruction().isPEI() || !filter.getFilter(node).alwaysThrowsException(block.getLastInstruction())) {
      specialCaseThrowFiltered(cfg, normalSucc);
    } else {
      assertTrue(block.getLastInstruction().isPEI());
      assertTrue(filter.getFilter(node).alwaysThrowsException(block.getLastInstruction()));
    }
  }

  /**
   * If a filtered exception is thrown explicit with a throw command, all
   * previous nodes, which only have normal edges to the throw statement will be
   * deleted. They don't have a connection to the exit node anymore.
   * 
   * So, if there is a throw statement as normal successor, evrything is fine.
   * 
   * @param cfg
   * @param normalSucc
   */
  private static void specialCaseThrowFiltered(ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg, ISSABasicBlock normalSucc) {
    ISSABasicBlock next = normalSucc;
    while (!(next.getLastInstruction() instanceof SSAThrowInstruction)) {
      assertTrue(cfg.getNormalSuccessors(next).iterator().hasNext());
      next = cfg.getNormalSuccessors(next).iterator().next();
    }
  }

  private static void checkNoNewEdges(ControlFlowGraph<SSAInstruction, ISSABasicBlock> original,
      ControlFlowGraph<SSAInstruction, ISSABasicBlock> filtered) {
    for (ISSABasicBlock block : filtered) {
      for (ISSABasicBlock normalSucc : filtered.getNormalSuccessors(block)) {
        assertTrue(original.getNormalSuccessors(block).contains(normalSucc));
      }

      for (ISSABasicBlock exceptionalSucc : filtered.getExceptionalSuccessors(block)) {
        assertTrue(original.getExceptionalSuccessors(block).contains(exceptionalSucc));
      }
    }
  }
}
