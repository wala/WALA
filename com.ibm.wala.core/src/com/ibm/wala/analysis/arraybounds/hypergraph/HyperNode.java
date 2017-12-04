package com.ibm.wala.analysis.arraybounds.hypergraph;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight;

/**
 * A HyperNode is a node of a {@link DirectedHyperGraph}.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 * @param <T>
 */
public class HyperNode<T> {
	private Weight weight;
	private Weight newWeight;

	/**
	 * Set of edges, which have this node as source, can be set automatically
	 * with {@link DirectedHyperGraph#updateNodeEdges()}
	 */
	private Set<DirectedHyperEdge<T>> outEdges;
	/**
	 * Set of edges, which have this node as source, can be set automatically
	 * with {@link DirectedHyperGraph#updateNodeEdges()}
	 */
	private Set<DirectedHyperEdge<T>> inEdges;

	private T value;

	public HyperNode(T value) {
		this.value = value;
		this.outEdges = new HashSet<>();
		this.weight = Weight.NOT_SET;
		this.newWeight = Weight.NOT_SET;
	}

	public Set<DirectedHyperEdge<T>> getInEdges() {
		return this.inEdges;
	}

	public Weight getNewWeight() {
		return this.newWeight;
	}

	public Set<DirectedHyperEdge<T>> getOutEdges() {
		return this.outEdges;
	}

	public T getValue() {
		return this.value;
	}

	public Weight getWeight() {
		return this.weight;
	}

	public void setInEdges(Set<DirectedHyperEdge<T>> inEdges) {
		this.inEdges = inEdges;
	}

	public void setNewWeight(Weight newWeight) {
		this.newWeight = newWeight;
	}

	public void setOutEdges(Set<DirectedHyperEdge<T>> outEdges) {
		this.outEdges = outEdges;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public void setWeight(Weight weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		if (this.weight == Weight.NOT_SET) {
			return this.value.toString();
		} else {
			return this.value.toString() + ": " + this.weight;
		}
	}
}
