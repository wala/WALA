/**
 * Refinement Analysis Tools is Copyright ©2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient’s reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents’ employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.demandpa.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.demandpa.alg.ContextSensitiveStateMachine;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.ThisFilteringHeapModel;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo.PointsToResult;
import com.ibm.wala.demandpa.alg.refinepolicy.FieldRefinePolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.ManualFieldPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.ManualRefinementPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.RefinementPolicyFactory;
import com.ibm.wala.demandpa.alg.refinepolicy.TunedRefinementPolicy;
import com.ibm.wala.demandpa.alg.statemachine.StateMachineFactory;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.genericutil.Predicate;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.demandpa.util.SimpleMemoryAccessMap;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WalaException;

/**
 * Uses a demand-driven points-to analysis to check the safety of downcasts.
 * 
 * @author Manu Sridharan
 * 
 */
public class DemandCastChecker {

  private static final String CAST_TO_CHECK = null;

  private static final String CAST_TO_CHECK_METHOD = null;

  private static final int MAX_CASTS = Integer.MAX_VALUE;

  private static final boolean DUMP_ALL_IR = false;

  private final static String CHA_EXCLUSIONS = false ? null : "PLDIChaExclusions.txt";

  /**
   * @param args
   * @throws CancelException 
   * @throws IllegalArgumentException 
   * @throws IOException 
   */
  public static void main(String[] args) throws IllegalArgumentException, CancelException, IOException {
    WalaUtil.initializeTraceFile();
    try {
      p = new Properties();
      p.putAll(WalaProperties.loadProperties());
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }

    // runTestCase("Lspec/benchmarks/_201_compress/Main", "compress.xml",
    // "compress");
    // runTestCase("Lspec/benchmarks/_209_db/Main", "db.xml", "db");
    // runTestCase("Lspec/benchmarks/_228_jack/Main", "jack.xml", "jack");
    // runTestCase("Lspec/benchmarks/_213_javac/Main", "javac.xml", "javac");
    // runTestCase("Lspec/benchmarks/_202_jess/Main", "jess.xml", "jess");
    // runTestCase("Lspec/benchmarks/_222_mpegaudio/Main", "mpegaudio.xml",
    // "mpegaudio");
    // runTestCase("Lspec/benchmarks/_227_mtrt/Main", "mtrt.xml", "mtrt");
    // runTestCase("Lca/mcgill/sable/soot/jimple/Main", "soot-c.xml", "soot-c");
    // runTestCase("LSableCC", "sablecc-j.xml", "sablecc-j");
    runTestCase("Lpolyglot/main/Main", "polyglot.xml", "polyglot");
  }

  private static void runTestCase(String mainClass, String scopeFile, String benchName) throws IllegalArgumentException, CancelException, IOException {
    Trace.println("=====BENCHMARK " + benchName + "=====");
    System.err.println("analyzing " + benchName);
    DemandRefinementPointsTo dmp = null;
    try {
      dmp = makeDemandPointerAnalysis(scopeFile, mainClass, benchName);
      if (DUMP_ALL_IR) {
        WalaUtil.dumpAllIR(dmp.getBaseCallGraph(), benchName, p);
      }
      findFailingCasts(dmp.getBaseCallGraph(), dmp);
    } catch (ClassHierarchyException e) {
      e.printStackTrace();
    }
    Trace.println("=*=*=*=*=*=*=*=*=*=*=*=*=*=*");
    Trace.println("");
    Trace.println("");
  }

  private static DemandRefinementPointsTo makeDemandPointerAnalysis(String scopeFile, String mainClass, String benchName)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(scopeFile, getExclusions(benchName));
    // build a type hierarchy
    ClassHierarchy cha = ClassHierarchy.make(scope);

    // set up call graph construction options; mainly what should be considered
    // entrypoints?
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    System.err.print("constructing call graph...");
    final CallGraph cg = buildCallGraph(scope, cha, options);
    System.err.println("done");
    // System.err.println(cg.toString());

    MemoryAccessMap fam = new SimpleMemoryAccessMap(cg, false);
    DemandRefinementPointsTo fullDemandPointsTo = new DemandRefinementPointsTo(cg, new ThisFilteringHeapModel(heapModel,cha), fam, cha, options,
        makeStateMachineFactory());
    fullDemandPointsTo.setRefinementPolicyFactory(chooseRefinePolicyFactory(cha));
    // fullDemandPointsTo.setFieldRefinePolicy(chooseFieldRefinePolicy(cha));
    // fullDemandPointsTo.setCGRefinePolicy(chooseCGRefinePolicy());
    // fullDemandPointsTo.setTraversalBudget(BUDGET);
    return fullDemandPointsTo;
  }

  // private static final boolean EXCLUDE_STUFF = false;

  private static String getExclusions(String benchName) {
    if (benchName.equals("sablecc-j")) {
      return CHA_EXCLUSIONS;
    } else {
      // TODO can we just return null here?
      return "J2SEClassHierarchyExclusions.txt";
    }
  }

  // private static CallGraphRefinePolicy chooseCGRefinePolicy() {
  // if (true) {
  // return new AlwaysRefineCGPolicy();
  // // return new HackedCGRefinePolicy();
  // } else {
  // return new NeverRefineCGPolicy();
  // }
  // }

  private static final boolean CHEAP_CG = true;

  private static HeapModel heapModel;

  private static Properties p;

  /**
   * builds a call graph, and sets the corresponding heap model for analysis
   * 
   * @param scope
   * @param cha
   * @param options
   * @return
   * @throws CancelException 
   * @throws IllegalArgumentException 
   */
  private static CallGraph buildCallGraph(AnalysisScope scope, ClassHierarchy cha, AnalysisOptions options) throws IllegalArgumentException, CancelException {
    CallGraph ret = null;
    final AnalysisCache cache = new AnalysisCache();
    if (CHEAP_CG) {
      // ret = CallGraphTestUtil.buildRTA(options, cha, scope, warnings);
      SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(options, cache, cha, scope);
      ret = builder.makeCallGraph(options);
      // we want vanilla 0-1 CFA, which has one abstract loc per allocation
      heapModel = Util.makeVanillaZeroOneCFABuilder(options, cache, cha, scope);
    } else {
      final SSAPropagationCallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cache, cha, scope);
      heapModel = builder;
      ret = builder.makeCallGraph(options);

    }
    return ret;
  }

  @SuppressWarnings("unused")
  private static RefinementPolicyFactory chooseRefinePolicyFactory(ClassHierarchy cha) {
    if (true) {
      return new TunedRefinementPolicy.Factory(cha);
    } else {
      return new ManualRefinementPolicy.Factory(cha);
    }
  }

  private static StateMachineFactory<IFlowLabel> makeStateMachineFactory() {
    return new ContextSensitiveStateMachine.Factory();
  }

  private static List<Pair<CGNode, SSACheckCastInstruction>> findFailingCasts(CallGraph cg, DemandRefinementPointsTo dmp) {
    final ClassHierarchy cha = dmp.getClassHierarchy();
    List<Pair<CGNode, SSACheckCastInstruction>> failing = new ArrayList<Pair<CGNode, SSACheckCastInstruction>>();

    int numSafe = 0, numMightFail = 0;
    outer: for (Iterator<? extends CGNode> nodeIter = cg.iterator(); nodeIter.hasNext();) {
      CGNode node = nodeIter.next();
      TypeReference declaringClass = node.getMethod().getReference().getDeclaringClass();
      // skip library classes
      if (declaringClass.getClassLoader().equals(ClassLoaderReference.Primordial)) {
        continue;
      }
      // skip spec/io stuff
      // if (declaringClass.toString().indexOf("Lspec/io") != -1) {
      // continue;
      // }
      if (CAST_TO_CHECK_METHOD != null) {
        if (!node.getMethod().toString().equals(CAST_TO_CHECK_METHOD)) {
          continue;
        } else {
          System.err.println("found chosen method");
        }
      }
      IR ir = node.getIR();
      if (ir == null)
        continue;
      SSAInstruction[] instrs = ir.getInstructions();
      for (int i = 0; i < instrs.length; i++) {
        if (numSafe + numMightFail > MAX_CASTS)
          break outer;
        Trace.flush();
        SSAInstruction instruction = instrs[i];
        if (instruction instanceof SSACheckCastInstruction) {
          SSACheckCastInstruction castInstr = (SSACheckCastInstruction) instruction;
          if (CAST_TO_CHECK != null) {
            if (!(castInstr.toString().equals(CAST_TO_CHECK))) {
              continue;
            } else {
              System.err.println("found chosen cast");
            }
          }
          final TypeReference declaredResultType = castInstr.getDeclaredResultType();
          if (declaredResultType.isPrimitiveType())
            continue;
          // if (declaredResultType.isArrayType()) {
          // warnings.add(new WeirdCastWarning(castInstr.toString()));
          // continue;
          // }
          Trace.println("CHECKING " + castInstr + " in " + node.getMethod());
          PointerKey castedPk = heapModel.getPointerKeyForLocal(node, castInstr.getUse(0));
          Predicate<Collection<InstanceKey>> castPred = new Predicate<Collection<InstanceKey>>() {

            @Override
            public boolean test(Collection<InstanceKey> p2set) {
              for (InstanceKey ik : p2set) {
                TypeReference ikTypeRef = ik.getConcreteType().getReference();
                if (!cha.isAssignableFrom(cha.lookupClass(declaredResultType), cha.lookupClass(ikTypeRef))) {
                  return false;
                }
              }
              return true;
            }

          };
          long startTime = System.currentTimeMillis();
          Pair<PointsToResult, Collection<InstanceKey>> queryResult = dmp.getPointsTo(castedPk, castPred);
          long runningTime = System.currentTimeMillis() - startTime;
          Trace.println("running time: " + runningTime + "ms");
          final FieldRefinePolicy fieldRefinePolicy = dmp.getRefinementPolicy().getFieldRefinePolicy();
          switch (queryResult.fst) {
          case SUCCESS:
            Trace.println("SAFE: " + castInstr + " in " + node.getMethod());
            if (fieldRefinePolicy instanceof ManualFieldPolicy) {
              ManualFieldPolicy hackedFieldPolicy = (ManualFieldPolicy) fieldRefinePolicy;
              Trace.println(hackedFieldPolicy.getHistory());
            }
            Trace.println("TRAVERSED " + dmp.getNumNodesTraversed() + " nodes");
            numSafe++;
            break;
          case NOMOREREFINE:
            if (queryResult.snd != null) {
              Trace.println("MIGHT FAIL: no more refinement possible for " + castInstr + " in " + node.getMethod());
            } else {
              Trace.println("MIGHT FAIL: exceeded budget for " + castInstr + " in " + node.getMethod());
            }
            failing.add(Pair.make(node, castInstr));
            numMightFail++;
            break;
          case BUDGETEXCEEDED:
            Trace.println("MIGHT FAIL: exceeded budget for " + castInstr + " in " + node.getMethod());
            failing.add(Pair.make(node, castInstr));
            numMightFail++;
            break;
          default:
            Assertions.UNREACHABLE();
          }
        }
      }
      // break outer;
    }
    Trace.println("TOTAL SAFE: " + numSafe);
    Trace.println("TOTAL MIGHT FAIL: " + numMightFail);
    return failing;
  }

}
