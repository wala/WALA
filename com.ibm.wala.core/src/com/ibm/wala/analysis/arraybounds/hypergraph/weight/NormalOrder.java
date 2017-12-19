package com.ibm.wala.analysis.arraybounds.hypergraph.weight;

import java.util.Comparator;

import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight.Type;

/**
 * Defines a normal Order on Weight: unlimited &lt; ... &lt; -1 &lt; 0 &lt; 1 &lt; ... not_set
 * is not comparable
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class NormalOrder implements Comparator<Weight> {

	@Override
	public int compare(Weight o1, Weight o2) {
		int result = 0;

		if (o1.getType() == Type.NOT_SET || o2.getType() == Type.NOT_SET) {
			throw new IllegalArgumentException(
					"Tried to compare weights, which are not set yet.");
		}

		if (o1.getType() == o2.getType()) {
			if (o1.getType() == Type.NUMBER) {
				result = o1.getNumber() - o2.getNumber();
			} else {
				result = 0;
			}
		} else {
			if (o1.getType() == Type.UNLIMITED) {
				result = -1;
			} else if (o2.getType() == Type.UNLIMITED) {
				result = 1;
			} else {
				throw new IllegalArgumentException(
						"Programming error, expected no cases to be left.");
			}
		}

		return result;
	}

}
