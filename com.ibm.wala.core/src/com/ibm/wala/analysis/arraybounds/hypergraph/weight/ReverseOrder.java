package com.ibm.wala.analysis.arraybounds.hypergraph.weight;

import java.util.Comparator;

import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight.Type;

/**
 * Defines a reverse Order on Weight: ... &gt; 1 &gt; 0 &gt; -1 &gt; ... &gt; unlimited not_set
 * is not comparable
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class ReverseOrder implements Comparator<Weight> {

	private final NormalOrder normalOrder;

	public ReverseOrder() {
		this.normalOrder = new NormalOrder();
	}

	@Override
	public int compare(Weight o1, Weight o2) {
		int result;
		if (o1.getType() == Type.UNLIMITED) {
			result = -1;
		} else if (o2.getType() == Type.UNLIMITED) {
			result = 1;
		} else {
			result = -this.normalOrder.compare(o1, o2);
		}

		return result;
	}

}
