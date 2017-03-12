package com.ibm.wala.analysis.arraybounds.hypergraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight;

/**
 * Implementation of a directed hyper graph. In a hyper graph an edge can have
 * more than one head and more than one tail.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 * @param <T>
 */
public class DirectedHyperGraph<T> {
	private final Map<T, HyperNode<T>> nodes;
	private final Set<DirectedHyperEdge<T>> edges;

	public DirectedHyperGraph() {
		this.nodes = new HashMap<>();
		this.edges = new HashSet<>();
	}

	public Set<DirectedHyperEdge<T>> getEdges() {
		return this.edges;
	}

	public Map<T, HyperNode<T>> getNodes() {
		return this.nodes;
	}

	/**
	 * Resets the weight of all nodes.
	 */
	public void reset() {
		for (final HyperNode<T> node : this.getNodes().values()) {
			node.setWeight(Weight.NOT_SET);
			node.setNewWeight(Weight.NOT_SET);
		}
	}

	@Override
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		for (final DirectedHyperEdge<T> edge : this.getEdges()) {
			buffer.append(edge.getSource());
			buffer.append(" -- ");
			buffer.append(edge.getWeight());
			buffer.append(" --> ");
			buffer.append(edge.getDestination());
			buffer.append("\n");
		}
		return buffer.toString();
	}

	/**
	 * The outdEdges of a node may not have been set on construction. Use this
	 * method to set them based on the edges of this HyperGraph.
	 */
	public void updateNodeEdges() {
		for (final HyperNode<T> node : this.getNodes().values()) {
			node.setOutEdges(new HashSet<DirectedHyperEdge<T>>());
			node.setInEdges(new HashSet<DirectedHyperEdge<T>>());
		}

		for (final DirectedHyperEdge<T> edge : this.edges) {
			for (final HyperNode<T> node : edge.getSource()) {
				node.getOutEdges().add(edge);
			}
			for (final HyperNode<T> node : edge.getDestination()) {
				node.getInEdges().add(edge);
			}
		}
	}
}
