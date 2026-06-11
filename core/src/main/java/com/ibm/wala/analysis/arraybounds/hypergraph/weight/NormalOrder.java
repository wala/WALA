package com.ibm.wala.analysis.arraybounds.hypergraph.weight;

import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight.Type;
import java.util.Comparator;

/**
 * Defines a normal Order on Weight: unlimited &lt; ... &lt; -1 &lt; 0 &lt; 1 &lt; ... not_set is
 * not comparable
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
public class NormalOrder implements Comparator<Weight> {

  @Override
  public int compare(Weight o1, Weight o2) {
    final int result;

    if (o1.type() == Type.NOT_SET || o2.type() == Type.NOT_SET) {
      throw new IllegalArgumentException("Tried to compare weights, which are not set yet.");
    }

    if (o1.type() == o2.type()) {
      if (o1.type() == Type.NUMBER) {
        result = o1.number() - o2.number();
      } else {
        result = 0;
      }
    } else {
      if (o1.type() == Type.UNLIMITED) {
        result = -1;
      } else if (o2.type() == Type.UNLIMITED) {
        result = 1;
      } else {
        throw new IllegalArgumentException("Programming error, expected no cases to be left.");
      }
    }

    return result;
  }
}
