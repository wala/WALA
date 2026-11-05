package com.ibm.wala.benchmarks.callgraph;

import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphTransitiveClosure;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.intset.OrdinalSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * JMH macrobenchmark for {@link CallGraphTransitiveClosure#transitiveClosure}, the step that {@link
 * ModRef#computeMod} uses to close each node's mod set over its transitive callees.
 *
 * <p>{@link #setup()} builds, once per fork, the 0-1-CFA call graph and pointer analysis for the
 * BCEL verifier's {@code main}, as {@code CallGraphTest.testBcelVerifier} does, then collects each
 * node's own mod set with {@link ModRef#getMod}. {@link #closeModSets()} times only the closure of
 * those mod sets. The pointer analysis stays alive throughout, as it does for a real {@link
 * ModRef#computeMod} client.
 *
 * <p>Each invocation is one complete closure, so this benchmark uses {@link Mode#SingleShotTime
 * single-shot time mode}, in which each iteration is exactly one invocation, measured in {@link
 * TimeUnit#MILLISECONDS milliseconds}. Run with {@code ./gradlew :core:jmh}.
 */
@BenchmarkMode(Mode.SingleShotTime)
@Fork(5)
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10)
public class TransitiveClosureBenchmark {

  /** The call graph whose nodes' mod sets are closed. */
  private CallGraph cg;

  /** The pointer analysis that {@link #cg} was built with, kept alive for the whole trial. */
  private PointerAnalysis<InstanceKey> pa;

  /** Each node's own mod set, not including its callees. */
  private Map<CGNode, Collection<PointerKey>> nodeResults;

  @Setup(Level.Trial)
  public void setup()
      throws CancelException, ClassHierarchyException, IllegalArgumentException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.BCEL, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints =
        Util.makeMainEntrypoints(cha, TestConstants.BCEL_VERIFIER_MAIN);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    options.setReflectionOptions(ReflectionOptions.NONE);
    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(JavaLanguage.get(), options, new AnalysisCacheImpl(), cha);
    cg = builder.makeCallGraph(options, null);
    pa = builder.getPointerAnalysis();

    ModRef<InstanceKey> modRef = ModRef.make();
    ExtendedHeapModel h = modRef.makeHeapModel(pa);
    nodeResults =
        CallGraphTransitiveClosure.collectNodeResults(
            cg,
            n -> {
              Set<PointerKey> mod = HashSetFactory.make();
              IR ir = n.getIR();
              if (ir != null) {
                for (SSAInstruction s : Iterator2Iterable.make(ir.iterateNormalInstructions())) {
                  mod.addAll(modRef.getMod(n, h, pa, s, null));
                }
              }
              return mod;
            });
  }

  /**
   * Times only the closure of the mod sets collected in {@link #setup()}.
   *
   * <p>Returning the closure prevents JMH and the JIT from eliminating the computation.
   */
  @Benchmark
  public Map<CGNode, OrdinalSet<PointerKey>> closeModSets() {
    return CallGraphTransitiveClosure.transitiveClosure(cg, nodeResults);
  }
}
