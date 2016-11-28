package com.ibm.wala.analysis.arraybounds.hypergraph.weight.edgeweights;

import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.edgeweights.AdditiveEdgeWeight;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.edgeweights.EdgeWeight;

/**
 * EdgeWeight that adds a specific value.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class AdditiveEdgeWeight implements EdgeWeight {
	private final Weight value;

	public AdditiveEdgeWeight(Weight value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final AdditiveEdgeWeight other = (AdditiveEdgeWeight) obj;
		if (this.value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!this.value.equals(other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}

	@Override
	public Weight newValue(Weight weight) {
		return weight.add(this.value);
	}

	@Override
	public String toString() {
		return this.value.toString();
	}
}
