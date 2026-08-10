package com.ibm.wala.benchmarks.dataflow;

import com.ibm.wala.examples.analysis.dataflow.DataflowTest;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
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
 * JMH macrobenchmarks for the interprocedural reaching-definitions analyses of {@link
 * DataflowTest#testContextSensitive} and {@link DataflowTest#testContextInsensitive}.
 *
 * <p>{@link #analyzeTestContextSensitive()} times only the {@link
 * com.ibm.wala.examples.analysis.dataflow.ContextSensitiveReachingDefs} construction and {@code
 * analyze()} call for the {@code dataflow.StaticDataflow} test subject, while {@link
 * #analyzeTestContextInsensitive()} times the equivalent context-insensitive work: the {@link
 * com.ibm.wala.ipa.cfg.ExplodedInterproceduralCFG} construction, the {@link
 * com.ibm.wala.examples.analysis.dataflow.ContextInsensitiveReachingDefs} construction, and the
 * {@code analyze()} call. These are the parts of the two {@code DataflowTest} tests this benchmark
 * cares about. All of the preceding analysis-pipeline work — analysis-scope construction,
 * class-hierarchy construction, and call-graph construction (including pointer analysis) — is done
 * once, in {@link #setup()}, before any timing begins, and the same call graphs are reused across
 * every iteration.
 *
 * <p>Each measured invocation is one complete interprocedural reaching-definitions analysis, taking
 * on the order of a quarter of a millisecond for the {@code dataflow.StaticDataflow} test subject
 * (measured means of roughly {@code 0.25} and {@code 0.23} ms for the context-insensitive and
 * context-sensitive analyses). Because a single invocation is a natural unit of work, this
 * benchmark uses {@link Mode#SingleShotTime single-shot time mode}, in which each iteration is
 * exactly one invocation, measured in {@link TimeUnit#MILLISECONDS milliseconds}.
 *
 * <p>These are sub-millisecond analyses, so a single invocation is noisy (allocation and GC jitter
 * are on the same scale as the work itself). The {@code @Warmup} of 60 iterations covers the JIT
 * ramp, which extends well past 30 iterations, and the {@code @Measurement} of 20 iterations across
 * 4 forks averages the noise down to a per-run error of roughly {@code 5-9%}. Repeated runs agree
 * with each other within that noise: e.g. the context-insensitive mean reproduced at {@code 0.255}
 * and {@code 0.250} ms and the context-sensitive mean at {@code 0.236} and {@code 0.224} ms across
 * two consecutive runs.
 *
 * <p>Run with {@code ./gradlew :core:jmh}.
 */
@BenchmarkMode(Mode.SingleShotTime)
@Fork(4)
@Measurement(iterations = 20)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 60)
public class DataflowBenchmark {

  /**
   * The pre-analysis state for {@link #analyzeTestContextSensitive()}, built by {@link #setup()}.
   */
  private DataflowTest.TestContextSensitiveAnalysis contextSensitiveAnalysis;

  /**
   * The pre-analysis state for {@link #analyzeTestContextInsensitive()}, built by {@link #setup()}.
   */
  private DataflowTest.TestContextInsensitiveAnalysis contextInsensitiveAnalysis;

  @Setup(Level.Trial)
  public void setup()
      throws CancelException, ClassHierarchyException, IllegalArgumentException, IOException {
    contextSensitiveAnalysis = DataflowTest.prepareTestContextSensitive();
    contextInsensitiveAnalysis = DataflowTest.prepareTestContextInsensitive();
  }

  /**
   * Times only the context-sensitive reaching-definitions analysis that {@link
   * DataflowTest#testContextSensitive} performs, using the same call graph that {@code
   * testContextSensitive} uses.
   *
   * <p>The preceding pipeline work is done once, in {@link #setup()}, before any timing begins.
   * Returning the analysis result prevents JMH and the JIT from eliminating the computation.
   */
  @Benchmark
  public DataflowTest.TestContextSensitiveResult analyzeTestContextSensitive() {
    return DataflowTest.computeTestContextSensitive(contextSensitiveAnalysis);
  }

  /**
   * Times only the context-insensitive reaching-definitions analysis that {@link
   * DataflowTest#testContextInsensitive} performs, using the same call graph and class hierarchy
   * that {@code testContextInsensitive} uses.
   *
   * <p>The preceding pipeline work is done once, in {@link #setup()}, before any timing begins.
   * Returning the analysis result prevents JMH and the JIT from eliminating the computation.
   */
  @Benchmark
  public DataflowTest.TestContextInsensitiveResult analyzeTestContextInsensitive() {
    return DataflowTest.computeTestContextInsensitive(contextInsensitiveAnalysis);
  }
}
