package com.ibm.wala.benchmarks.slicer;

import com.ibm.wala.core.tests.slicer.SlicerTest;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.Collection;
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
 * JMH macrobenchmark for the {@link Slicer#computeBackwardSlice} call in {@link
 * SlicerTest#testList}.
 *
 * <p>{@link #computeBackwardSliceInTestList()} times only that {@code computeBackwardSlice} call
 * for the {@code slice.TestList} test subject, which is the part of {@code SlicerTest.testList}
 * this benchmark cares about. All of the preceding analysis-pipeline work — analysis-scope
 * construction, class-hierarchy construction, call-graph construction (including pointer analysis),
 * and finding the {@code get} call in the {@code main} method — is done once, in {@link #setup()},
 * before any timing begins, and the same call graph, pointer analysis, and slice root are reused
 * across every iteration.
 *
 * <p>Each measured invocation is one complete backward slice, taking roughly 30ms. Because a single
 * invocation is a natural unit of work, this benchmark uses {@link Mode#SingleShotTime single-shot
 * time mode}, in which each iteration is exactly one invocation, measured in {@link
 * TimeUnit#MILLISECONDS milliseconds}.
 *
 * <p>The slice computation runs enough code that the JIT needs many invocations to reach steady
 * state: per-fork times trend downward for dozens of invocations, and measured means fall from
 * roughly 40ms after five warmup invocations to a stable 28.6ms only after roughly sixty. The
 * {@code @Warmup(iterations = 60)} setting absorbs that entire ramp, so the
 * {@code @Measurement(iterations = 10)} measured invocations in each fork are steady and
 * independent. Together with {@code @Fork(4)} — four independent JVMs, so that no single JVM's JIT
 * decisions dominate the reported error — this benchmark is reproducible to within about 1.5%,
 * tight enough to track relative changes in runtime.
 *
 * <p>Run with {@code ./gradlew :core:jmh}.
 */
@BenchmarkMode(Mode.SingleShotTime)
@Fork(4)
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 60)
public class SlicerBenchmark {

  /** The pre-slice analysis state, built once per fork by {@link #setup()}. */
  private SlicerTest.TestListAnalysis analysis;

  @Setup(Level.Trial)
  public void setup()
      throws CancelException, ClassHierarchyException, IllegalArgumentException, IOException {
    analysis = SlicerTest.prepareTestList();
  }

  /**
   * Times only the {@link Slicer#computeBackwardSlice} call that {@link SlicerTest#testList}
   * performs, using the same call graph, pointer analysis, and slice root that {@code testList}
   * uses.
   *
   * <p>The preceding pipeline work is done once, in {@link #setup()}, before any timing begins.
   * Returning the computed slice prevents JMH and the JIT from eliminating the computation.
   */
  @Benchmark
  public Collection<Statement> computeBackwardSliceInTestList()
      throws CancelException, IllegalArgumentException {
    return SlicerTest.computeTestListSlice(analysis);
  }
}
