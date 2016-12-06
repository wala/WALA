package com.ibm.wala.analysis.arraybounds.hypergraph.weight.edgeweights;

import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight;

/**
 * The weight of an edge can produce a new value for the tail nodes given the
 * head nodes.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
public interface EdgeWeight {
	public Weight newValue(Weight weight);
}
