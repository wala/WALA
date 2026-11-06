package com.ibm.wala.core.tests.callGraph;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphTransitiveClosure;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.GenReach;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.graph.traverse.SCCIterator;
import com.ibm.wala.util.intset.OrdinalSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link CallGraphTransitiveClosure#transitiveClosure} matches both a general
 * fixed-point closure and plain reachability on call graphs with cycles.
 */
public class CallGraphTransitiveClosureTest extends WalaTestCase {

  /**
   * About how many nodes of a graph have their closure checked against plain reachability, which
   * takes a depth-first search per node.
   */
  private static final int REACHABILITY_SAMPLES = 100;

  /**
   * Computes the closure with the general bit-vector fixed-point solver, over the inverted call
   * graph, as {@link CallGraphTransitiveClosure#transitiveClosure} did before it condensed strongly
   * connected components.
   *
   * @param cg the call graph
   * @param nodeResults analysis result for each individual node
   * @return a map from each node to the analysis result for the node and its transitive callees
   * @throws CancelException if the solver is canceled
   */
  private static <T> Map<CGNode, Collection<T>> fixedPointClosure(
      CallGraph cg, Map<CGNode, Collection<T>> nodeResults) throws CancelException {
    GenReach<CGNode, T> gr = new GenReach<>(GraphInverter.invert(cg), nodeResults);
    BitVectorSolver<CGNode> solver = new BitVectorSolver<>(gr);
    solver.solve(null);
    Map<CGNode, Collection<T>> result = HashMapFactory.make();
    for (CGNode n : cg) {
      result.put(
          n,
          OrdinalSet.toCollection(
              new OrdinalSet<>(solver.getOut(n).getValue(), gr.getLatticeValues())));
    }
    return result;
  }

  /**
   * Converts each node's closure to a plain set, so closures can be compared by their elements.
   *
   * @param closure a map from each node to its closure
   * @return a map from each node to the elements of its closure
   */
  private static <T> Map<CGNode, Collection<T>> toSets(Map<CGNode, OrdinalSet<T>> closure) {
    Map<CGNode, Collection<T>> result = HashMapFactory.make();
    closure.forEach((n, s) -> result.put(n, OrdinalSet.toCollection(s)));
    return result;
  }

  /**
   * Gives each even-numbered node itself as its result and leaves odd-numbered nodes without a
   * result, so a node's closure is exactly the even-numbered nodes it reaches.
   *
   * @param cg the call graph
   * @return the result of each even-numbered node
   */
  private static Map<CGNode, Collection<CGNode>> evenNodes(CallGraph cg) {
    Map<CGNode, Collection<CGNode>> result = HashMapFactory.make();
    for (CGNode n : cg) {
      if (cg.getNumber(n) % 2 == 0) {
        result.put(n, Set.of(n));
      }
    }
    return result;
  }

  /**
   * Asserts that {@code cg} has a strongly connected component of several nodes that calls,
   * directly or transitively, a different component of several nodes, so the closure must handle
   * both cycles and components that depend on other components.
   *
   * @param cg the call graph
   */
  private static void assertHasDependentCycles(CallGraph cg) {
    Set<Set<CGNode>> cycles = HashSetFactory.make();
    new SCCIterator<>(cg)
        .forEachRemaining(
            c -> {
              if (c.size() > 1) {
                cycles.add(c);
              }
            });
    boolean dependent = false;
    for (Set<CGNode> c : cycles) {
      Set<CGNode> reachable = DFS.getReachableNodes(cg, c);
      for (Set<CGNode> other : cycles) {
        if (other != c && reachable.containsAll(other)) {
          dependent = true;
        }
      }
    }
    assertThat(dependent).isTrue();
  }

  /**
   * Asserts that the closure of {@link #evenNodes} matches the fixed-point closure at every node,
   * and the even-numbered nodes reachable from the node at about {@link #REACHABILITY_SAMPLES}
   * evenly spaced nodes.
   *
   * @param cg the call graph
   * @throws CancelException if the solver is canceled
   */
  private static void assertClosureOfEvenNodes(CallGraph cg) throws CancelException {
    Map<CGNode, Collection<CGNode>> nodeResults = evenNodes(cg);
    Map<CGNode, Collection<CGNode>> closure =
        toSets(CallGraphTransitiveClosure.transitiveClosure(cg, nodeResults));
    assertThat(closure).isEqualTo(fixedPointClosure(cg, nodeResults));
    int stride = Math.max(1, cg.getNumberOfNodes() / REACHABILITY_SAMPLES);
    for (CGNode n : cg) {
      if (cg.getNumber(n) % stride != 0) {
        continue;
      }
      Set<CGNode> reachableEven = HashSetFactory.make();
      for (CGNode m : DFS.getReachableNodes(cg, Set.of(n))) {
        if (cg.getNumber(m) % 2 == 0) {
          reachableEven.add(m);
        }
      }
      assertThat(closure.get(n)).isEqualTo(reachableEven);
    }
  }

  /**
   * Builds options for all application entrypoints of the {@code hello} test subject.
   *
   * @return the analysis scope, class hierarchy, and options
   * @throws ClassHierarchyException if the class hierarchy cannot be built
   * @throws IOException if the analysis scope cannot be read
   */
  private static HelloAnalysis makeHelloAnalysis() throws ClassHierarchyException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.HELLO, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = new AllApplicationEntrypoints(scope, cha);
    return new HelloAnalysis(cha, CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints));
  }

  /**
   * The class hierarchy and options for the {@code hello} test subject.
   *
   * @param cha the class hierarchy
   * @param options the analysis options
   */
  private record HelloAnalysis(ClassHierarchy cha, AnalysisOptions options) {}

  /** Tests the closure on the 0-CFA call graph of a recursive list, a small graph with cycles. */
  @Test
  public void testRecursiveList()
      throws CancelException, ClassHierarchyException, IllegalArgumentException, IOException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(cha, "Lrecurse/NList");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, false);
    assertClosureOfEvenNodes(cg);
  }

  /**
   * Tests the closure on the RTA call graph of every application entrypoint of {@code hello}, a
   * large graph whose cycles depend on other cycles.
   */
  @Test
  public void testHelloRTA()
      throws CancelException, ClassHierarchyException, IllegalArgumentException, IOException {
    HelloAnalysis hello = makeHelloAnalysis();
    CallGraph cg =
        CallGraphTestUtil.buildRTA(hello.options(), new AnalysisCacheImpl(), hello.cha());
    assertHasDependentCycles(cg);
    assertClosureOfEvenNodes(cg);
  }

  /**
   * Tests the closure of mod sets on the 0-1-CFA call graph of every application entrypoint of
   * {@code hello}, comparing it with the fixed-point closure of the same per-node mod sets.
   */
  @Test
  public void testHelloModSets()
      throws CancelException, ClassHierarchyException, IllegalArgumentException, IOException {
    HelloAnalysis hello = makeHelloAnalysis();
    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroOneCFABuilder(
            JavaLanguage.get(), hello.options(), new AnalysisCacheImpl(), hello.cha());
    CallGraph cg = builder.makeCallGraph(hello.options(), null);
    PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();

    ModRef<InstanceKey> modRef = ModRef.make();
    ExtendedHeapModel h = modRef.makeHeapModel(pa);
    Map<CGNode, Collection<PointerKey>> nodeResults =
        CallGraphTransitiveClosure.collectNodeResults(
            cg,
            n -> {
              Collection<PointerKey> mod = HashSetFactory.make();
              IR ir = n.getIR();
              if (ir != null) {
                for (SSAInstruction s : Iterator2Iterable.make(ir.iterateNormalInstructions())) {
                  mod.addAll(modRef.getMod(n, h, pa, s, null));
                }
              }
              return mod;
            });

    assertThat(toSets(CallGraphTransitiveClosure.transitiveClosure(cg, nodeResults)))
        .isEqualTo(fixedPointClosure(cg, nodeResults));
  }
}
