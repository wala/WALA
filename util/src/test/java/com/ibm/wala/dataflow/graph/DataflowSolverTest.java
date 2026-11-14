package com.ibm.wala.dataflow.graph;

import static com.ibm.wala.util.intset.IntSetAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DataflowSolver}'s variable lookup preconditions and short-circuit optimizations.
 *
 * <p>The propagation tests combine generating transfers with identity transfers to exercise
 * variable sharing across node inputs, node outputs, and edges.
 */
class DataflowSolverTest {

  @Test
  void nodeTransfersPropagateThroughIdentityNodes() throws CancelException {
    BitVectorSolver<String> solver = makeSolver(true, false, BitVectorUnion.instance());

    solver.solve(null);

    assertThat(solver.getOut("c").getValue()).containsExactly(0);
  }

  @Test
  void edgeTransfersPropagateThroughIdentityEdges() throws CancelException {
    BitVectorSolver<String> solver = makeSolver(true, true, BitVectorUnion.instance());

    solver.solve(null);

    assertThat(solver.getOut("c").getValue()).containsExactly(0, 1);
  }

  @Test
  void edgeOnlyTransfersPreserveNontrivialUnaryMeets() throws CancelException {
    BitVectorSolver<String> solver = makeSolver(false, true, new UnionWithConstant());

    solver.solve(null);

    assertThat(solver.getIn("c").getValue()).containsExactly(1, 2);
  }

  @Test
  void getInRejectsUnknownNodes() throws CancelException {
    BitVectorSolver<String> solver = makeSolver(true, false, BitVectorUnion.instance());
    solver.solve(null);

    assertThatThrownBy(() -> solver.getIn("unknown")).isInstanceOf(NullPointerException.class);
  }

  @Test
  void getOutRejectsUnknownNodes() throws CancelException {
    BitVectorSolver<String> solver = makeSolver(true, false, BitVectorUnion.instance());
    solver.solve(null);

    assertThatThrownBy(() -> solver.getOut("unknown")).isInstanceOf(NullPointerException.class);
  }

  @Test
  void getInRequiresInitializedVariables() {
    BitVectorSolver<String> solver = makeSolver(true, false, BitVectorUnion.instance());

    assertThatThrownBy(() -> solver.getIn("a")).isInstanceOf(NullPointerException.class);
  }

  @Test
  void getOutRequiresInitializedVariables() {
    BitVectorSolver<String> solver = makeSolver(true, false, BitVectorUnion.instance());

    assertThatThrownBy(() -> solver.getOut("a")).isInstanceOf(NullPointerException.class);
  }

  @Test
  void getOutRequiresNodeTransfers() throws CancelException {
    BitVectorSolver<String> solver = makeSolver(false, true, BitVectorUnion.instance());
    solver.solve(null);

    assertThatThrownBy(() -> solver.getOut("a")).isInstanceOf(NullPointerException.class);
  }

  /**
   * Creates a solver over {@code a -> b -> c} with an additional edge {@code a -> c}.
   *
   * <p>When enabled, the node transfer at {@code a} adds bit 0, and the edge transfer from {@code
   * a} to {@code b} adds bit 1. All other enabled transfer functions are identities.
   *
   * @param nodeTransfers whether to enable node transfer functions
   * @param edgeTransfers whether to enable edge transfer functions
   * @param meet the operator that combines predecessor values
   * @return a solver whose variables have not yet been initialized
   */
  private static BitVectorSolver<String> makeSolver(
      boolean nodeTransfers, boolean edgeTransfers, AbstractMeetOperator<BitVectorVariable> meet) {
    Graph<String> graph = SlowSparseNumberedGraph.make();
    graph.addNode("a");
    graph.addNode("b");
    graph.addNode("c");
    graph.addEdge("a", "b");
    graph.addEdge("a", "c");
    graph.addEdge("b", "c");
    ITransferFunctionProvider<String, BitVectorVariable> functions =
        new ITransferFunctionProvider<>() {
          @Override
          public UnaryOperator<BitVectorVariable> getNodeTransferFunction(String node) {
            return node.equals("a") ? new BitVectorUnionConstant(0) : BitVectorIdentity.instance();
          }

          @Override
          public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(String src, String dst) {
            return src.equals("a") && dst.equals("b")
                ? new BitVectorUnionConstant(1)
                : BitVectorIdentity.instance();
          }

          @Override
          public boolean hasNodeTransferFunctions() {
            return nodeTransfers;
          }

          @Override
          public boolean hasEdgeTransferFunctions() {
            return edgeTransfers;
          }

          @Override
          public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
            return meet;
          }
        };
    return new BitVectorSolver<>(new BasicFramework<>(graph, functions));
  }

  /** A union meet that adds bit 2 even when there is only one predecessor. */
  private static final class UnionWithConstant extends AbstractMeetOperator<BitVectorVariable> {
    @Override
    public boolean isUnaryNoOp() {
      return false;
    }

    @Override
    public byte evaluate(BitVectorVariable lhs, BitVectorVariable[] rhs) {
      byte changed = BitVectorUnion.instance().evaluate(lhs, rhs);
      if (!lhs.get(2)) {
        lhs.set(2);
        return CHANGED;
      }
      return changed;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof UnionWithConstant;
    }

    @Override
    public int hashCode() {
      return 31;
    }

    @Override
    public String toString() {
      return "UNION with 2";
    }
  }
}
