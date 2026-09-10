package com.ibm.wala.benchmarks.callgraph;

import com.ibm.wala.core.tests.callGraph.CallGraphTest;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
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
 * JMH macrobenchmarks that perform large-scale, end-to-end WALA call-graph analyses.
 *
 * <p>Unlike the microbenchmarks, which measure tightly-scoped operations in isolation, each
 * macrobenchmark here runs a complete analysis task just as a real WALA user or a unit test would.
 *
 * <p>This class contains two kinds of benchmarks:
 *
 * <ul>
 *   <li>{@link #testHelloAllEntrypoints()} — a faithful reproduction of {@link
 *       CallGraphTest#testHelloAllEntrypoints} including all test-harness verification. This is the
 *       existing end-to-end probe.
 *   <li>Per-phase analysis-only benchmarks ({@link #buildRTA()}, {@link #buildZeroCFA()}, etc.)
 *       that time <em>only</em> the pointer-analysis construction for each call-graph algorithm,
 *       with no verification overhead. The scope, class hierarchy, entrypoints, analysis options,
 *       and cache are built once in {@link #setup()}, outside the timed region.
 * </ul>
 *
 * <p>Because these tasks take seconds rather than microseconds, they use the {@link
 * Mode#SingleShotTime single-shot time mode}, in which each iteration is a single invocation. Run
 * with {@code ./gradlew :core:jmh}.
 *
 * <p>The benchmark is stable to roughly one percent across forks, but with only a single fork and a
 * handful of iterations JMH reports a wildly inflated error: its printed margin is {@code t(n-1,
 * 99.9%) x} the standard error, and with {@code n = 3} that t-multiplier is about 32. Each fork is
 * therefore treated as one independent sample, and six forks (with six measured invocations each)
 * give a printed error that is small enough to track relative changes in runtime.
 */
@BenchmarkMode(Mode.SingleShotTime)
@Fork(6)
@Measurement(iterations = 6)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3)
public class CallGraphBenchmark {

  private AnalysisScope scope;
  private ClassHierarchy cha;
  private AnalysisOptions options;
  private AnalysisCacheImpl cache;

  @Setup(Level.Trial)
  public void setup() throws ClassHierarchyException, IllegalArgumentException, IOException {
    scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.HELLO, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = new AllApplicationEntrypoints(scope, cha);
    options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    cache = new AnalysisCacheImpl();
  }

  /**
   * Builds call graphs from all application entrypoints of the {@code hello} test subject.
   *
   * <p>We intentionally reuse {@link CallGraphTest#testHelloAllEntrypoints} verbatim, as a
   * large-scale benchmark workload. Error Prone's {@code JUnitMethodInvoked} check dislikes this,
   * but the benchmark is exactly a stand-in for the JUnit runner.
   */
  @Benchmark
  @SuppressWarnings("JUnitMethodInvoked")
  public void testHelloAllEntrypoints()
      throws CancelException, ClassHierarchyException, IllegalArgumentException, IOException {
    new CallGraphTest().testHelloAllEntrypoints();
  }

  /**
   * Times only the RTA call-graph construction, with no test-harness verification.
   *
   * <p>The preceding pipeline work (scope, CHA, options, cache) is done once in {@link #setup()},
   * before any timing begins. Returning the built {@link CallGraph} prevents dead-code elimination.
   */
  @Benchmark
  public CallGraph buildRTA() throws IllegalArgumentException, CancelException {
    return CallGraphTestUtil.buildRTA(options, cache, cha);
  }

  /** Times only the 0-CFA call-graph construction, with no test-harness verification. */
  @Benchmark
  public CallGraph buildZeroCFA() throws IllegalArgumentException, CancelException {
    return CallGraphTestUtil.buildZeroCFA(options, cache, cha, false);
  }

  /** Times only the 0-1-CFA call-graph construction, with no test-harness verification. */
  @Benchmark
  public CallGraph buildZeroOneCFA() throws IllegalArgumentException, CancelException {
    return CallGraphTestUtil.buildZeroOneCFA(options, cache, cha, false);
  }

  /** Times only the 0-Container-CFA call-graph construction, with no test-harness verification. */
  @Benchmark
  public CallGraph buildZeroContainerCFA() throws IllegalArgumentException, CancelException {
    return CallGraphTestUtil.buildZeroContainerCFA(options, cache, cha);
  }

  /**
   * Times only the 0-1-Container-CFA call-graph construction, with no test-harness verification.
   */
  @Benchmark
  public CallGraph buildZeroOneContainerCFA() throws IllegalArgumentException, CancelException {
    return CallGraphTestUtil.buildZeroOneContainerCFA(options, cache, cha);
  }

  /**
   * Times all five call-graph phases in sequence, sharing one cache and JIT regime as in the real
   * pipeline, but with no test-harness verification.
   *
   * <p>This is the lighter-weight counterpart to {@link #testHelloAllEntrypoints()}: it preserves
   * the ensemble behavior (phases share one cache, exactly as a user would run them) while dropping
   * the ~30% verification overhead. Returning the built call graphs prevents dead-code elimination.
   */
  @Benchmark
  public CallGraphTest.AllApplicationCallGraphs buildAllCallGraphs()
      throws IllegalArgumentException, CancelException {
    return CallGraphTest.buildAllCallGraphs(options, cache, cha, false);
  }
}
