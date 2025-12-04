package com.ibm.wala.util.collections;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

/** JHM benchmark tests for {@link MapUtil}. */
public class MapUtilBenchmark {

  /**
   * The size of {@link Map}s to be manipulated by benchmark methods.
   *
   * <p>These {@link Map}s should be large enough to accommodate realistic operations. However, they
   * should not be so large that benchmarks will be dominated by the time spent creating and copying
   * {@link Map}s.
   */
  private static final int MAP_SIZE = 10_000;

  private static final int[] EXTANT_KEYS = IntStream.range(0, MAP_SIZE).toArray();

  private static final int[] NOVEL_KEYS = IntStream.range(MAP_SIZE, MAP_SIZE * 2).toArray();

  private abstract static class ModifiableMapState<V> {

    @Nullable private @Unmodifiable Map<Integer, V> originalMap;

    @Nullable Map<Integer, V> map;

    protected abstract V computeValue(int key);

    /**
     * Prepares {@link #map} and {@link #originalMap} as {@link Map}s with keys from 0 through (but
     * not including) {@link #MAP_SIZE}.
     *
     * <p>{@link #map} is modifiable and visible to benchmarks, while {@link #originalMap} is
     * unmodifiable and hidden from benchmarks. Both maps have identical contents at the start of
     * each benchmark iteration. If a benchmark invocation modifies {@link #map}, then it should
     * also revert that modification, so that both maps are back to being identical when the
     * benchmark invocation ends.
     */
    @MustBeInvokedByOverriders
    protected void setup() {
      originalMap =
          IntStream.range(0, MAP_SIZE)
              .boxed()
              .collect(Collectors.toUnmodifiableMap(key -> key, this::computeValue));
      map = new HashMap<>(originalMap);
    }

    /** Validates that {@link #map} is still equivalent to {@link #originalMap}. */
    @MustBeInvokedByOverriders
    protected void tearDown() {
      assertThat(map).containsExactlyInAnyOrderEntriesOf(originalMap);
    }
  }

  /** A {@link Map} for use with {@link MapUtil#findOrCreateSet(Map, Object)}. */
  @State(Scope.Thread)
  public static class ToListState extends ModifiableMapState<List<Integer>> {

    @Override
    protected List<Integer> computeValue(int key) {
      return List.of(key);
    }

    @Setup
    public void setup() {
      super.setup();
    }

    @TearDown
    public void tearDown() {
      super.tearDown();
    }
  }

  /** A {@link Map} for use with {@link MapUtil#findOrCreateMap(Map, Object)}. */
  @State(Scope.Thread)
  public static class ToMapState extends ModifiableMapState<Map<Integer, Integer>> {

    @Override
    protected Map<Integer, Integer> computeValue(int key) {
      return Map.of(key, key);
    }

    @Setup
    public void setup() {
      super.setup();
    }

    @TearDown
    public void tearDown() {
      super.tearDown();
    }
  }

  /** A {@link Map} for use with {@link MapUtil#findOrCreateSet(Map, Object)}. */
  @State(Scope.Thread)
  public static class ToSetState extends ModifiableMapState<Set<Integer>> {

    @Override
    protected Set<Integer> computeValue(int key) {
      return Set.of(key);
    }

    @Setup
    public void setup() {
      super.setup();
    }

    @TearDown
    public void tearDown() {
      super.tearDown();
    }
  }

  /**
   * A {@link Map} for use with {@link MapUtil#findOrCreateValue(Map, Object, Factory)}.
   *
   * <p>Contains no duplicate values.
   */
  @State(Scope.Thread)
  public static class ToValueState extends ModifiableMapState<Integer> {

    @Override
    protected Integer computeValue(int key) {
      return key;
    }

    @Setup
    public void setup() {
      super.setup();
    }

    @TearDown
    public void tearDown() {
      super.tearDown();
    }
  }

  /**
   * A {@link Map} for use with {@link MapUtil#groupKeysByValue(Map)}.
   *
   * <p>Contains four keys that map to each distinct value.
   */
  @State(Scope.Thread)
  public static class ToQuarterValueState extends ModifiableMapState<Integer> {

    @Override
    protected Integer computeValue(int key) {
      return key / 4;
    }

    @Setup
    public void setup() {
      super.setup();
    }

    @TearDown
    public void tearDown() {
      super.tearDown();
    }
  }

  /** Uses {@link MapUtil#findOrCreateList(Map, Object)} with many extant keys. */
  @Benchmark
  public void findOrCreateList_extant(final Blackhole blackhole, final ToListState state) {
    final var map = requireNonNull(state.map);
    for (int key : EXTANT_KEYS) {
      blackhole.consume(MapUtil.findOrCreateList(map, key));
    }
  }

  /** Uses {@link MapUtil#findOrCreateList(Map, Object)} with many novel keys. */
  @Benchmark
  public void findOrCreateList_novel(final Blackhole blackhole, final ToListState state) {
    final var map = requireNonNull(state.map);
    for (int key : NOVEL_KEYS) {
      blackhole.consume(MapUtil.findOrCreateList(map, key));
      state.map.remove(key);
    }
  }

  /** Uses {@link MapUtil#findOrCreateMap(Map, Object)} with many extant keys. */
  @Benchmark
  public void findOrCreateMap_extant(final Blackhole blackhole, final ToMapState state) {
    final var map = requireNonNull(state.map);
    for (int key : EXTANT_KEYS) {
      blackhole.consume(MapUtil.findOrCreateMap(map, key));
    }
  }

  /** Uses {@link MapUtil#findOrCreateMap(Map, Object)} with many novel keys. */
  @Benchmark
  public void findOrCreateMap_novel(final Blackhole blackhole, final ToMapState state) {
    final var map = requireNonNull(state.map);
    for (int key : NOVEL_KEYS) {
      blackhole.consume(MapUtil.findOrCreateMap(map, key));
      state.map.remove(key);
    }
  }

  /** Uses {@link MapUtil#findOrCreateSet(Map, Object)} with many extant keys. */
  @Benchmark
  public void findOrCreateSet_extant(final Blackhole blackhole, final ToSetState state) {
    final var map = requireNonNull(state.map);
    for (int key : EXTANT_KEYS) {
      blackhole.consume(MapUtil.findOrCreateSet(map, key));
    }
  }

  /** Uses {@link MapUtil#findOrCreateSet(Map, Object)} with many novel keys. */
  @Benchmark
  public void findOrCreateSet_novel(final Blackhole blackhole, final ToSetState state) {
    final var map = requireNonNull(state.map);
    for (int key : NOVEL_KEYS) {
      blackhole.consume(MapUtil.findOrCreateSet(map, key));
      state.map.remove(key);
    }
  }

  /** Uses {@link MapUtil#findOrCreateValue(Map, Object, Factory)} with many extant keys. */
  @Benchmark
  public void findOrCreateValue_extant(final Blackhole blackhole, final ToValueState state) {
    final var map = requireNonNull(state.map);
    for (int key : EXTANT_KEYS) {
      blackhole.consume(MapUtil.findOrCreateValue(map, key, () -> key));
    }
  }

  /** Uses {@link MapUtil#findOrCreateValue(Map, Object, Factory)} with many novel keys. */
  @Benchmark
  public void findOrCreateValue_novel(final Blackhole blackhole, final ToValueState state) {
    final var map = requireNonNull(state.map);
    for (int key : NOVEL_KEYS) {
      blackhole.consume(MapUtil.findOrCreateValue(map, key, () -> key));
      state.map.remove(key);
    }
  }

  /** Calls {@link MapUtil#inverseMap(Map)}. */
  @Benchmark
  public Object inverseMap(final ToSetState state) {
    return MapUtil.inverseMap(requireNonNull(state.map));
  }

  /** Calls {@link MapUtil#invertOneToOneMap(Map)}. */
  @Benchmark
  public Object invertOneToOneMap(ToValueState state) {
    return MapUtil.invertOneToOneMap(requireNonNull(state.map));
  }

  /** Calls {@link MapUtil#groupKeysByValue(Map)}. */
  @Benchmark
  public Object groupKeysByValue(ToQuarterValueState state) {
    return MapUtil.groupKeysByValue(requireNonNull(state.map));
  }
}
