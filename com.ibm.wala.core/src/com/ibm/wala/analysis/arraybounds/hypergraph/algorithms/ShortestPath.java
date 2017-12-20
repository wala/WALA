package com.ibm.wala.analysis.arraybounds.hypergraph.algorithms;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.analysis.arraybounds.hypergraph.DirectedHyperEdge;
import com.ibm.wala.analysis.arraybounds.hypergraph.DirectedHyperGraph;
import com.ibm.wala.analysis.arraybounds.hypergraph.HyperNode;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight.Type;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.edgeweights.EdgeWeight;

/**
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 * @param <T>
 *          NodeValueType
 * @see ShortestPath#compute(DirectedHyperGraph, HyperNode, Comparator)
 */
public class ShortestPath<T> {
  /**
   * Computes all shortest paths from source. The result is stored in
   * {@link HyperNode#getWeight()}.
   *
   * This is using a variation of Bellman-Ford for hyper graphs.
   *
   * @param graph
   * @param source
   * @param comparator
   *          defines order on weights.
   */
  public static <NodeValueType> void compute(DirectedHyperGraph<NodeValueType> graph, HyperNode<NodeValueType> source,
      Comparator<Weight> comparator) {
    graph.reset();
    source.setWeight(Weight.ZERO);
    new ShortestPath<>(graph, comparator);
  }

  private Set<DirectedHyperEdge<T>> updatedEdges;
  private final Comparator<Weight> comparator;
  private final DirectedHyperGraph<T> graph;
  private boolean setUnlimitedOnChange = false;

  private boolean hasNegativeCycle = false;

  /**
   * @param graph
   *          Source nodes for shortest path computation should be set to 0,
   *          other nodes should be set to {@link Weight#NOT_SET}.
   * @param comparator
   *          defines order on weights.
   */
  private ShortestPath(DirectedHyperGraph<T> graph, Comparator<Weight> comparator) {
    this.comparator = comparator;
    this.graph = graph;

    this.computeShortestPaths();
    this.hasNegativeCycle = (this.updatedEdges.size() > 0);
    if (this.hasNegativeCycle) {
      // trigger, that changing values are set to infty in writeChanges:
      this.setUnlimitedOnChange = true;

      // we need to propagate negative cycle to all connected nodes
      this.computeShortestPaths();
    }
  }

  private void computeShortestPaths() {
    this.updatedEdges = this.graph.getEdges();
    final int nodeCount = this.graph.getNodes().size();
    for (int i = 0; i < nodeCount - 1; i++) {
      this.updateAllEdges();
      if (this.updatedEdges.size() == 0) {
        break;
      }
    }
  }

  /**
   * @param weight
   * @param otherWeight
   * @return weight &gt; otherWeight
   */
  private boolean greaterThen(Weight weight, Weight otherWeight) {
    return otherWeight.getType() == Type.NOT_SET || this.comparator.compare(weight, otherWeight) > 0;
  }

  /**
   * @param weight
   * @param otherWeight
   * @return weight &lt; otherWeight
   */
  private boolean lessThen(Weight weight, Weight otherWeight) {
    return otherWeight.getType() == Type.NOT_SET || this.comparator.compare(weight, otherWeight) < 0;
  }

  /**
   * Maximum of source weights, modified by the value of the edge. Note that
   * every weight is larger than {@link Weight#NOT_SET} for max computation.
   * This allows distances to propagate, even if not all nodes are connected to
   * the source of the shortest path computation. Otherwise (source,
   * other)->(sink) would not have a path from source to sink.
   *
   * @param edge
   * @return max{edgeValue.newValue(sourceWeight) | sourceWeight \in
   *         edge.getSources()}
   */
  private Weight maxOfSources(final DirectedHyperEdge<T> edge) {
    final EdgeWeight edgeValue = edge.getWeight();
    Weight newWeight = Weight.NOT_SET;
    for (final HyperNode<T> node : edge.getSource()) {

      final Weight nodeWeight = node.getWeight();
      if (nodeWeight.getType() != Type.NOT_SET) {

        final Weight temp = edgeValue.newValue(nodeWeight);
        if (this.greaterThen(temp, newWeight)) {
          newWeight = temp;
        }
      } else {
        newWeight = Weight.NOT_SET;
        break;
      }
    }
    return newWeight;
  }

  /**
   * We do not need to iterate all edges, but edges of which the source weight
   * was changed, other edges will not lead to a change of the destination
   * weight. For correct updating of the destination weight, we need to consider
   * all incoming edges. (The minimum of in edges is computed per round, not
   * global - see
   * {@link ShortestPath#updateDestinationsWithMin(HashSet, DirectedHyperEdge, Weight)}
   * )
   *
   * @return A set of edges, that may lead to changes of weights.
   */
  private HashSet<DirectedHyperEdge<T>> selectEdgesToIterate() {
    final HashSet<DirectedHyperEdge<T>> edgesToIterate = new HashSet<>();
    for (final DirectedHyperEdge<T> edge : this.updatedEdges) {
      for (final HyperNode<T> node : edge.getDestination()) {
        edgesToIterate.addAll(node.getInEdges());
      }
    }
    return edgesToIterate;
  }

  private void updateAllEdges() {

    for (final DirectedHyperEdge<T> edge : this.selectEdgesToIterate()) {
      final Weight maxOfSources = this.maxOfSources(edge);

      if (maxOfSources.getType() != Type.NOT_SET) {
        this.updateDestinationsWithMin(edge, maxOfSources);
      }
    }

    this.writeChanges();
  }

  /**
   * Updates Nodes with the minimum of all incoming edges. The minimum is
   * computed over the minimum of all edges that were processed in this round (
   * {@link ShortestPath#selectEdgesToIterate()}).
   *
   * This is necessary for the feature described in
   * {@link ShortestPath#maxOfSources(DirectedHyperEdge)} to work properly: The
   * result of different rounds is not always monotonous, p.a.:
   *
   * <pre>
   * (n1, n2)->(n3)
   * Round 1: n1 = unset, n2 = -3 -&gt; n3 = max(unset,-3) = -3
   * Round 2: n1 = 1, n2 = -3 -&gt; n3 = max(1,-3) = 1
   * </pre>
   *
   * Would we compute the minimum of n3 over all rounds, it would be -3, but 1
   * is correct.
   *
   * Note: that every weight is smaller than {@link Weight#NOT_SET} for min
   * computation. This allows distances to propagate, even if not all nodes are
   * connected to the source of the shortest path computation. Otherwise
   * (source)->(sink), (other)->(sink), would not have a path from source to
   * sink.
   *
   * @param edge
   * @param newWeight
   */
  private void updateDestinationsWithMin(final DirectedHyperEdge<T> edge, Weight newWeight) {
    if (!newWeight.equals(Weight.NOT_SET)) {
      for (final HyperNode<T> node : edge.getDestination()) {
        if (this.lessThen(newWeight, node.getNewWeight())) {
          node.setNewWeight(newWeight);
        }
      }
    }
  }

  /**
   * This method is necessary, as the min is updated per round. (See
   * {@link ShortestPath#updateDestinationsWithMin(DirectedHyperEdge, Weight)} )
   */
  private void writeChanges() {
    final HashSet<DirectedHyperEdge<T>> newUpdatedEdges = new HashSet<>();

    for (final HyperNode<T> node : this.graph.getNodes().values()) {
      final Weight oldWeight = node.getWeight();
      final Weight newWeight = node.getNewWeight();
      if (!newWeight.equals(Weight.NOT_SET) && !oldWeight.equals(newWeight)) {
        // node weight has changed, so out edges have to be updated next
        // round:
        newUpdatedEdges.addAll(node.getOutEdges());

        if (this.setUnlimitedOnChange) {
          node.setWeight(Weight.UNLIMITED);
        } else {
          node.setWeight(node.getNewWeight());
        }
      }
      node.setNewWeight(Weight.NOT_SET);
    }

    this.updatedEdges = newUpdatedEdges;
  }
}
