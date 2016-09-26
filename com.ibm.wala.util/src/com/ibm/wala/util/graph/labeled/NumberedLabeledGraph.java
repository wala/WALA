package com.ibm.wala.util.graph.labeled;

import com.ibm.wala.util.graph.NumberedGraph;

public interface NumberedLabeledGraph<T, I> extends NumberedGraph<T>, NumberedLabeledEdgeManager<T,I> {

}
