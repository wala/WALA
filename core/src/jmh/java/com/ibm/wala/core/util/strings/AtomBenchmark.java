package com.ibm.wala.core.util.strings;

import java.io.UTFDataFormatException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH microbenchmarks for the {@link Atom} class.
 *
 * <p>These benchmarks are intended for detecting performance regressions when the {@link Atom}
 * implementation changes, not for measuring absolute performance. Each {@link Benchmark} method
 * therefore runs against a warm, fixed-size interning dictionary whose contents are identical on
 * every run, so that measurements are stable and repeatable.
 *
 * <p>With two exceptions, every benchmark measures the "hit" path: the {@link Atom} being created
 * or derived is already interned, so the measured work is what real WALA code does in its common
 * case. The two exceptions are {@link #findOrCreateFromFreshByteArray(AtomFixture)}, which copies
 * its input array before interning (as if parsing a freshly read class file), and {@link
 * #findOrCreateMiss}, which interns genuinely new content.
 *
 * <p>A dedicated "miss" benchmark that interns a new {@link Atom} on every invocation is not
 * feasible even with {@link Atom#resetDictionaryForTesting()}: in the default {@linkplain
 * Mode#Throughput throughput mode} a single iteration runs millions of invocations, so interning
 * would grow the dictionary by millions of entries within each iteration, and the per-operation
 * cost would average over a dictionary of ever-increasing size. {@link #findOrCreateMiss} instead
 * interns a bounded fresh pool of content for each measurement iteration, resetting the dictionary
 * first so that every iteration starts from the same empty state.
 *
 * <p>Even so, the per-operation cost is not steady from the very first iterations: in repeated runs
 * the per-operation time only settles to its steady state after roughly eight or more iterations in
 * each fork. {@link #findOrCreateMiss} therefore uses more warmup and measurement iterations than
 * the class default.
 *
 * <p>Run with {@code ./gradlew :core:jmh}. Results are written to {@code core/build/results/jmh/}.
 * These settings trade total run time for precision, so that they can detect relative regressions:
 * each method runs in three forks, with three one-second warmup iterations and three one-second
 * measurement iterations. A single fork is measurably noisier (the per-fork mean is stable, but
 * between-fork variance dominates for several methods), so three forks cut the reported error from
 * up to ~10% down to ~1-2%. A few methods with idiosyncratic noise get their own overrides; see
 * their Javadoc. Pass {@code -f}, {@code -wi}, {@code -i}, and similar JMH command-line flags for
 * one-off adjustments.
 */
@BenchmarkMode(Mode.Throughput)
@Fork(3)
@Measurement(iterations = 3, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
public class AtomBenchmark {

  /** Number of distinct, never-before-interned byte arrays in each miss-benchmark iteration. */
  private static final int MISS_POOL_SIZE = 1 << 14;

  /**
   * A representative mix of names and descriptors, pre-interned exactly once, plus the raw inputs
   * used to create them and the atoms derived from them by slicing and concatenation.
   */
  @State(Scope.Thread)
  public static class AtomFixture {

    private Atom[] atoms;
    private byte[][] byteArrays;
    private String[] strings;
    private Atom[] prefixes;
    private Atom[] leftConcats;
    private Atom[] rightConcats;
    private int next;

    /**
     * Intern the fixture's content and pre-intern all atoms that benchmarks will derive from it.
     */
    @Setup(Level.Trial)
    public void setUp() {
      final List<String> descriptors =
          new ArrayList<>(Arrays.asList("I", "J", "V", "Z", "B", "C", "S", "D", "F"));
      for (int i = 0; i < 128; i++) {
        descriptors.add("field" + i);
        descriptors.add("method" + i);
        descriptors.add("Ljava/lang/Object" + i + ";");
      }
      for (int i = 0; i < 64; i++) {
        descriptors.add("Lcom/ibm/wala/core/util/strings/Class" + i + ";");
        descriptors.add("(I" + "J".repeat(i % 8) + ")V");
      }
      for (int i = 0; i < 32; i++) {
        descriptors.add("[".repeat(i % 8 + 1) + "I");
      }

      atoms = new Atom[descriptors.size()];
      byteArrays = new byte[descriptors.size()][];
      strings = new String[descriptors.size()];
      prefixes = new Atom[descriptors.size()];
      for (int i = 0; i < descriptors.size(); i++) {
        strings[i] = descriptors.get(i);
        byteArrays[i] = strings[i].getBytes(StandardCharsets.UTF_8);
        atoms[i] = Atom.findOrCreate(byteArrays[i]);
        prefixes[i] = Atom.findOrCreate(byteArrays[i], 0, Math.min(4, atoms[i].length()));
      }

      final List<Atom> concatLeft = new ArrayList<>();
      final List<Atom> concatRight = new ArrayList<>();
      for (int i = 0; i < atoms.length; i++) {
        concatLeft.add(atoms[i]);
        concatRight.add(atoms[(i + 1) % atoms.length]);
      }
      leftConcats = concatLeft.toArray(new Atom[0]);
      rightConcats = concatRight.toArray(new Atom[0]);

      // Pre-intern everything the concatenation benchmark derives, so that that benchmark measures
      // the steady-state "hit" path with a fixed-size dictionary.
      for (int i = 0; i < atoms.length; i++) {
        Atom.concat(atoms[i], atoms[(i + 1) % atoms.length]);
      }
    }

    /** Returns the index of the next fixture element, cycling forever without overflow. */
    public int nextIndex() {
      final int index = next;
      next = (next + 1) & Integer.MAX_VALUE;
      return index;
    }

    /** Returns the next fixture atom, cycling. */
    public Atom nextAtom() {
      return atoms[nextIndex() % atoms.length];
    }

    /** Returns the next fixture byte array, cycling. */
    public byte[] nextByteArray() {
      return byteArrays[nextIndex() % byteArrays.length];
    }

    /** Returns the next fixture string, cycling. */
    public String nextString() {
      return strings[nextIndex() % strings.length];
    }
  }

  /**
   * A fresh pool of never-before-interned byte arrays, regenerated with distinct content for each
   * iteration so that {@link #findOrCreateMiss} always interns genuinely new atoms.
   *
   * <p>The interning dictionary is also cleared at the start of every iteration. This reset ensures
   * that {@link #findOrCreateMiss} always interns into an empty dictionary and every iteration
   * measures the same steady-state work. Otherwise, the cost of each insertion would grow as the
   * dictionary fills up over the run.
   */
  @State(Scope.Thread)
  public static class FreshBytes {
    private byte[][] pool;
    private int iteration;

    /**
     * Empties the interning dictionary so that every iteration interns into the same empty state.
     */
    @Setup(Level.Iteration)
    public void resetDictionary() {
      Atom.resetDictionaryForTesting();
    }

    /** Regenerates the pool with content distinct from every previous iteration's. */
    @Setup(Level.Iteration)
    public void regenerate() {
      final int current = iteration++;
      pool = new byte[MISS_POOL_SIZE][];
      for (int i = 0; i < MISS_POOL_SIZE; i++) {
        pool[i] = (current + "-" + i).getBytes(StandardCharsets.UTF_8);
      }
    }
  }

  /**
   * Looks up an already-interned {@link Atom} from its bytes.
   *
   * <p>One more fork than the class default, because this benchmark's mean has drifted more between
   * forks than the other microbenchmarks in repeated runs.
   */
  @Benchmark
  @Fork(4)
  public Atom findOrCreateFromByteArray(AtomFixture fixture) {
    return Atom.findOrCreate(fixture.nextByteArray());
  }

  /** Looks up an already-interned {@link Atom} from a freshly copied byte array. */
  @Benchmark
  public Atom findOrCreateFromFreshByteArray(AtomFixture fixture) {
    final byte[] original = fixture.nextByteArray();
    return Atom.findOrCreate(Arrays.copyOf(original, original.length));
  }

  /** Looks up an already-interned {@link Atom} from a Unicode string. */
  @Benchmark
  public Atom findOrCreateUnicodeAtom(AtomFixture fixture) {
    return Atom.findOrCreateUnicodeAtom(fixture.nextString());
  }

  /** Looks up an already-interned {@link Atom} from an ASCII string. */
  @Benchmark
  public Atom findOrCreateAsciiAtom(AtomFixture fixture) {
    return Atom.findOrCreateAsciiAtom(fixture.nextString());
  }

  /** Looks up an already-interned {@link Atom} from a whole byte-array slice. */
  @Benchmark
  public Atom findOrCreateFromByteArraySlice(AtomFixture fixture) {
    final byte[] bytes = fixture.nextByteArray();
    return Atom.findOrCreate(bytes, 0, bytes.length);
  }

  /** Creates the trailing half of an already-interned {@link Atom}. */
  @Benchmark
  public Atom right(AtomFixture fixture) {
    final Atom atom = fixture.nextAtom();
    return atom.right(atom.length() / 2);
  }

  /** Checks whether an {@link Atom} starts with a fixed prefix. */
  @Benchmark
  public boolean startsWith(AtomFixture fixture) {
    final int index = fixture.nextIndex() % fixture.atoms.length;
    return fixture.atoms[index].startsWith(fixture.prefixes[index]);
  }

  /**
   * Concatenates two already-interned {@link Atom}s to an already-interned result.
   *
   * <p>One more fork than the class default, because this benchmark's mean has drifted more between
   * forks than the other microbenchmarks in repeated runs.
   */
  @Benchmark
  @Fork(4)
  public Atom concatAtoms(AtomFixture fixture) {
    final int index = fixture.nextIndex();
    return Atom.concat(
        fixture.leftConcats[index % fixture.leftConcats.length],
        fixture.rightConcats[index % fixture.rightConcats.length]);
  }

  /** Decodes an {@link Atom}'s bytes to a Unicode string. */
  @Benchmark
  public String toUnicodeString(AtomFixture fixture) throws UTFDataFormatException {
    return fixture.nextAtom().toUnicodeString();
  }

  /** Produces an {@link Atom}'s printable form. */
  @Benchmark
  public String toString(AtomFixture fixture) {
    return fixture.nextAtom().toString();
  }

  /** Copies an {@link Atom}'s bytes into a new array. */
  @Benchmark
  public byte[] getValArray(AtomFixture fixture) {
    return fixture.nextAtom().getValArray();
  }

  /** Reads one byte from an {@link Atom}. */
  @Benchmark
  public byte getVal(AtomFixture fixture) {
    final Atom atom = fixture.nextAtom();
    return atom.getVal(atom.length() / 2);
  }

  /** Searches an {@link Atom} for a byte that is usually absent. */
  @Benchmark
  public int rIndex(AtomFixture fixture) {
    return fixture.nextAtom().rIndex((byte) 'a');
  }

  /**
   * Reads an {@link Atom}'s cached hash code.
   *
   * <p>More measurement iterations than the class default, because this benchmark's throughput
   * periodically dips (for an unidentified reason, possibly an OS or scheduler transient; the hot
   * path allocates nothing, so GC is unlikely) even after warmup, and extra iterations average
   * those dips out.
   */
  @Benchmark
  @Measurement(iterations = 6, time = 1)
  public int atomHashCode(AtomFixture fixture) {
    return fixture.nextAtom().hashCode();
  }

  /** Interns a fresh pool of genuinely new {@link Atom}s. */
  @Benchmark
  @BenchmarkMode(Mode.SingleShotTime)
  @Measurement(iterations = 10)
  @OperationsPerInvocation(MISS_POOL_SIZE)
  @Warmup(iterations = 10)
  public void findOrCreateMiss(FreshBytes freshBytes, Blackhole blackhole) {
    for (final byte[] bytes : freshBytes.pool) {
      blackhole.consume(Atom.findOrCreate(bytes));
    }
  }
}
