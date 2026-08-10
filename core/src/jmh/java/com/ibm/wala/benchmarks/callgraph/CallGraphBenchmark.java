package com.ibm.wala.benchmarks.callgraph;

import com.ibm.wala.core.tests.callGraph.CallGraphTest;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

/**
 * JMH macrobenchmarks that perform large-scale, end-to-end WALA analyses.
 *
 * <p>Unlike the microbenchmarks, which measure tightly-scoped operations in isolation, each
 * macrobenchmark here runs a complete analysis task just as a real WALA user or a unit test would.
 * The first such task, {@link #testHelloAllEntrypoints()}, is exactly the work of {@link
 * CallGraphTest#testHelloAllEntrypoints}.
 *
 * <p>Because these tasks take seconds rather than microseconds, they use the {@link
 * Mode#SingleShotTime single-shot time mode}, in which each iteration is a single invocation. Run
 * with {@code ./gradlew :core:jmh}.
 *
 * <p>The benchmark is stable to roughly one percent across forks, but with only a single fork and a
 * handful of iterations JMH reports a wildly inflated error: its printed margin is {@code t(n-1,
 * 99.9%) x} the standard error, and with {@code n = 3} that t-multiplier is about 32. Each fork is
 * therefore treated as one independent sample, and four forks (with two measured invocations each)
 * give a printed error that is small enough to track relative changes in runtime.
 */
@BenchmarkMode(Mode.SingleShotTime)
@Fork(4)
@Measurement(iterations = 2)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1)
public class CallGraphBenchmark {

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
}
