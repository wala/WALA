package com.ibm.wala.util.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.impl.BasicGraph;
import java.util.Iterator;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class GraphIntegrityTest {

  private record Node(int id) {
    @Override
    public int hashCode() {
      return 0;
    }
  }

  private static final List<Node> NODES = List.of(new Node(0), new Node(1), new Node(2));

  /** The two masks independently describe edges from source * 3 + target. */
  private static class IndependentAdjacencyGraph extends BasicGraph<Node> {
    private final int successors;
    private int predecessors;

    IndependentAdjacencyGraph(int successors, int predecessors) {
      this.successors = successors;
      this.predecessors = predecessors;
      NODES.forEach(this::addNode);
    }

    @Override
    public Iterator<Node> getSuccNodes(@Nullable Node n) {
      return NODES.stream()
          .filter(target -> (successors & (1 << (n.id() * 3 + target.id()))) != 0)
          .iterator();
    }

    @Override
    public Iterator<Node> getPredNodes(@Nullable Node n) {
      return NODES.stream()
          .filter(source -> (predecessors & (1 << (source.id() * 3 + n.id()))) != 0)
          .iterator();
    }

    @Override
    public int getSuccNodeCount(Node n) {
      return IteratorUtil.count(getSuccNodes(n));
    }

    @Override
    public int getPredNodeCount(Node n) {
      return IteratorUtil.count(getPredNodes(n));
    }

    @Override
    public boolean hasEdge(@Nullable Node src, @Nullable Node dst) {
      // An inconsistent graph's membership query cannot establish inverse adjacency.
      return true;
    }
  }

  private static boolean checkSucceeds(Graph<?> graph) {
    try {
      GraphIntegrity.check(graph);
      return true;
    } catch (UnsoundGraphException e) {
      return false;
    }
  }

  @Test
  public void acceptsExactlyMatchingAdjacencyRelationsOnThreeNodes() {
    // All 2^18 pairs, including self-loops, cycles, disconnected nodes, and hash collisions.
    // Equality of the independently supplied relations is the oracle, not another graph checker.
    assertSoftly(softly -> {
      for (int successors = 0; successors < 512; successors++) {
        for (int predecessors = 0; predecessors < 512; predecessors++) {
          softly
              .assertThat(checkSucceeds(new IndependentAdjacencyGraph(successors, predecessors)))
              .as("successor relation 0x%x, predecessor relation 0x%x", successors, predecessors)
              .isEqualTo(successors == predecessors);
        }
      }
    });
  }

  @Test
  public void rejectsPredecessorWithoutMatchingSuccessor() {
    assertThatThrownBy(() -> GraphIntegrity.check(new IndependentAdjacencyGraph(0, 2)))
        .isInstanceOf(UnsoundGraphException.class)
        .hasMessageContaining("is a predecessor")
        .hasMessageContaining("inverse doesn't hold");
  }

  @Test
  public void rejectsSuccessorWithoutMatchingPredecessor() {
    assertThatThrownBy(() -> GraphIntegrity.check(new IndependentAdjacencyGraph(2, 0)))
        .isInstanceOf(UnsoundGraphException.class)
        .hasMessageContaining("is a successor")
        .hasMessageContaining("inverse doesn't hold");
  }

  @Test
  public void rejectsPredecessorOutsideGraph() {
    var graph =
        new IndependentAdjacencyGraph(64, 64) {
          @Override
          public boolean containsNode(@Nullable Node n) {
            return !NODES.get(2).equals(n);
          }
        };
    assertThatThrownBy(() -> GraphIntegrity.check(graph))
        .isInstanceOf(UnsoundGraphException.class)
        .hasMessageContaining("is a predecessor")
        .hasMessageContaining("not contained in the graph");
  }

  @Test
  public void rejectsSuccessorOutsideGraph() {
    var graph =
        new IndependentAdjacencyGraph(4, 4) {
          @Override
          public boolean containsNode(@Nullable Node n) {
            return !NODES.get(2).equals(n);
          }
        };
    assertThatThrownBy(() -> GraphIntegrity.check(graph))
        .isInstanceOf(UnsoundGraphException.class)
        .hasMessageContaining("is a successor")
        .hasMessageContaining("not contained in the graph");
  }

  @Test
  public void rejectsListedNodeOutsideGraph() {
    var graph =
        new IndependentAdjacencyGraph(0, 0) {
          @Override
          public boolean containsNode(@Nullable Node n) {
            return false;
          }
        };
    assertThatThrownBy(() -> GraphIntegrity.check(graph))
        .isInstanceOf(UnsoundGraphException.class)
        .hasMessageContaining("is not contained");
  }

  @Test
  public void rejectsIncorrectSuccessorCount() {
    var graph =
        new IndependentAdjacencyGraph(2, 2) {
          @Override
          public int getSuccNodeCount(Node n) {
            return super.getSuccNodeCount(n) + 1;
          }
        };
    assertThatThrownBy(() -> GraphIntegrity.check(graph))
        .isInstanceOf(UnsoundGraphException.class)
        .hasMessageContaining("getSuccNodeCount");
  }

  @Test
  public void rejectsIncorrectPredecessorCount() {
    var graph =
        new IndependentAdjacencyGraph(2, 2) {
          @Override
          public int getPredNodeCount(Node n) {
            return super.getPredNodeCount(n) + 1;
          }
        };
    assertThatThrownBy(() -> GraphIntegrity.check(graph))
        .isInstanceOf(UnsoundGraphException.class)
        .hasMessageContaining("getPredNodeCount");
  }

  @Test
  public void rechecksAdjacencyAfterGraphChanges() throws UnsoundGraphException {
    var graph = new IndependentAdjacencyGraph(2, 2);
    GraphIntegrity.check(graph);
    graph.predecessors = 0;
    assertThatThrownBy(() -> GraphIntegrity.check(graph))
        .isInstanceOf(UnsoundGraphException.class)
        .hasMessageContaining("inverse doesn't hold");
  }

  @Test
  public void acceptsEmptyGraph() {
    assertThatCode(() -> GraphIntegrity.check(new BasicGraph<>())).doesNotThrowAnyException();
  }

  @Test
  public void checksDenseGraphWithoutRepeatedlyScanningInverseAdjacency()
      throws UnsoundGraphException {
    class CountingGraph extends BasicGraph<Integer> {
      private int visitedEdges;

      private Iterator<Integer> counting(Iterator<Integer> nodes) {
        return new Iterator<>() {
          @Override
          public boolean hasNext() {
            return nodes.hasNext();
          }

          @Override
          public Integer next() {
            visitedEdges++;
            return nodes.next();
          }
        };
      }

      @Override
      public Iterator<Integer> getPredNodes(@Nullable Integer n) {
        return counting(super.getPredNodes(n));
      }

      @Override
      public Iterator<Integer> getSuccNodes(@Nullable Integer n) {
        return counting(super.getSuccNodes(n));
      }
    }
    var graph = new CountingGraph();
    int nodeCount = 64;
    for (int n = 0; n < nodeCount; n++) {
      graph.addNode(n);
    }
    for (int source = 0; source < nodeCount; source++) {
      for (int target = 0; target < nodeCount; target++) {
        graph.addEdge(source, target);
      }
    }

    GraphIntegrity.check(graph);

    // Allow a generous constant number of full edge traversals, including the other checks.
    assertThat(graph.visitedEdges).isLessThan(16 * nodeCount * nodeCount);
  }
}
