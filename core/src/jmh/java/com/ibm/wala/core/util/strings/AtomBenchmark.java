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
 * <p>Run with {@code ./gradlew :core:jmh}. Results are written to {@code core/build/results/jmh/}.
 * Increase the fork count (e.g., by changing the {@code @Fork} annotations or passing {@code -f} on
 * the JMH command line) for more precise numbers.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class AtomBenchmark {

  /** Number of distinct, never-before-interned byte arrays in each miss-benchmark iteration. */
  private static final int MISS_POOL_SIZE = 1 << 14;

  /**
   * A representative mix of names and descriptors, pre-interned exactly once, plus the raw inputs
   * used to create them and the atoms derived from them by slicing, concatenation, and descriptor
   * parsing.
   */
  @State(Scope.Thread)
  public static class AtomFixture {

    private Atom[] atoms;
    private byte[][] byteArrays;
    private String[] strings;
    private ImmutableByteArray[] immutableByteArrays;
    private Atom[] prefixes;
    private Atom[] arrayDescriptors;
    private Atom[] elementDescriptors;
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
      immutableByteArrays = new ImmutableByteArray[descriptors.size()];
      prefixes = new Atom[descriptors.size()];
      for (int i = 0; i < descriptors.size(); i++) {
        strings[i] = descriptors.get(i);
        byteArrays[i] = strings[i].getBytes(StandardCharsets.UTF_8);
        immutableByteArrays[i] = ImmutableByteArray.make(strings[i]);
        atoms[i] = Atom.findOrCreate(byteArrays[i]);
        prefixes[i] = atoms[i].left(Math.min(4, atoms[i].length()));
      }

      final List<Atom> arrays = new ArrayList<>();
      final List<Atom> elements = new ArrayList<>();
      final List<Atom> concatLeft = new ArrayList<>();
      final List<Atom> concatRight = new ArrayList<>();
      for (int i = 0; i < atoms.length; i++) {
        final Atom atom = atoms[i];
        final Atom other = atoms[(i + 1) % atoms.length];
        if (atom.isArrayDescriptor()) {
          arrays.add(atom);
        } else {
          elements.add(atom);
        }
        concatLeft.add(atom);
        concatRight.add(other);
      }
      arrayDescriptors = arrays.toArray(new Atom[0]);
      elementDescriptors = elements.toArray(new Atom[0]);
      leftConcats = concatLeft.toArray(new Atom[0]);
      rightConcats = concatRight.toArray(new Atom[0]);

      // Pre-intern everything the concatenation and descriptor benchmarks derive, so that those
      // benchmarks measure the steady-state "hit" path with a fixed-size dictionary.
      for (int i = 0; i < atoms.length; i++) {
        Atom.concat((byte) '(', immutableByteArrays[i]);
        Atom.concat(atoms[i], atoms[(i + 1) % atoms.length]);
      }
      for (final Atom element : elementDescriptors) {
        element.arrayDescriptorFromElementDescriptor();
      }
      for (final Atom array : arrayDescriptors) {
        array.parseForArrayElementDescriptor();
        array.parseForInnermostArrayElementDescriptor();
        array.parseForArrayDimensionality();
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

    /** Returns the next fixture array descriptor, cycling. */
    public Atom nextArrayDescriptor() {
      return arrayDescriptors[nextIndex() % arrayDescriptors.length];
    }

    /** Returns the next fixture array-element descriptor, cycling. */
    public Atom nextElementDescriptor() {
      return elementDescriptors[nextIndex() % elementDescriptors.length];
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

  /** Looks up an already-interned {@link Atom} from its bytes. */
  @Benchmark
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

  /** Creates the leading half of an already-interned {@link Atom}. */
  @Benchmark
  public Atom left(AtomFixture fixture) {
    final Atom atom = fixture.nextAtom();
    return atom.left(atom.length() / 2);
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

  /** Concatenates two already-interned {@link Atom}s to an already-interned result. */
  @Benchmark
  public Atom concatAtoms(AtomFixture fixture) {
    final int index = fixture.nextIndex();
    return Atom.concat(
        fixture.leftConcats[index % fixture.leftConcats.length],
        fixture.rightConcats[index % fixture.rightConcats.length]);
  }

  /** Prepends a byte to an {@link ImmutableByteArray} to form an already-interned {@link Atom}. */
  @Benchmark
  public Atom concatByteAndImmutableByteArray(AtomFixture fixture) {
    final ImmutableByteArray bytes =
        fixture.immutableByteArrays[fixture.nextIndex() % fixture.immutableByteArrays.length];
    return Atom.concat((byte) '(', bytes);
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
  public boolean contains(AtomFixture fixture) {
    return fixture.nextAtom().contains((byte) 'a');
  }

  /** Searches an {@link Atom} for a byte that is usually absent. */
  @Benchmark
  public int rIndex(AtomFixture fixture) {
    return fixture.nextAtom().rIndex((byte) 'a');
  }

  /** Reads an {@link Atom}'s cached hash code. */
  @Benchmark
  public int atomHashCode(AtomFixture fixture) {
    return fixture.nextAtom().hashCode();
  }

  /** Applies the single-byte descriptor predicates to an {@link Atom}. */
  @Benchmark
  public void descriptorPredicates(AtomFixture fixture, Blackhole blackhole) {
    final Atom atom = fixture.nextAtom();
    blackhole.consume(atom.isClassDescriptor());
    blackhole.consume(atom.isArrayDescriptor());
    blackhole.consume(atom.isMethodDescriptor());
    blackhole.consume(atom.isReservedMemberName());
  }

  /** Strips one {@code [} from an already-interned array descriptor. */
  @Benchmark
  public Atom parseForArrayElementDescriptor(AtomFixture fixture) {
    return fixture.nextArrayDescriptor().parseForArrayElementDescriptor();
  }

  /** Strips all {@code [}s from an already-interned array descriptor. */
  @Benchmark
  public Atom parseForInnermostArrayElementDescriptor(AtomFixture fixture) {
    return fixture.nextArrayDescriptor().parseForInnermostArrayElementDescriptor();
  }

  /** Counts the leading {@code [}s of an already-interned array descriptor. */
  @Benchmark
  public int parseForArrayDimensionality(AtomFixture fixture) {
    return fixture.nextArrayDescriptor().parseForArrayDimensionality();
  }

  /** Turns an already-interned element descriptor into its already-interned array descriptor. */
  @Benchmark
  public Atom arrayDescriptorFromElementDescriptor(AtomFixture fixture) {
    return fixture.nextElementDescriptor().arrayDescriptorFromElementDescriptor();
  }

  /** Interns a fresh pool of genuinely new {@link Atom}s. */
  @Benchmark
  @BenchmarkMode(Mode.SingleShotTime)
  @Warmup(iterations = 3)
  @Measurement(iterations = 3)
  @OperationsPerInvocation(MISS_POOL_SIZE)
  public void findOrCreateMiss(FreshBytes freshBytes, Blackhole blackhole) {
    for (final byte[] bytes : freshBytes.pool) {
      blackhole.consume(Atom.findOrCreate(bytes));
    }
  }
}
