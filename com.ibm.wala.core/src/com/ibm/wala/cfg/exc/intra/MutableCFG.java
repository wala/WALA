package com.ibm.wala.cfg.exc.intra;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;

/**
 * A modifiable control flow graph.
 * 
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class MutableCFG<X, T extends IBasicBlock<X>> extends SparseNumberedGraph<T> {

	private MutableCFG() {
	}
	
	public static <I, T extends IBasicBlock<I>> MutableCFG<I, T> copyFrom(ControlFlowGraph<I, T> cfg) {
		MutableCFG<I, T> mutable = new MutableCFG<I, T>();
		
		for (T node : cfg) {
			mutable.addNode(node);
		}
		
		for (T node : cfg) {
			for (T succ : cfg.getNormalSuccessors(node)) {
				mutable.addEdge(node, succ);
			}

			for (T succ : cfg.getExceptionalSuccessors(node)) {
				mutable.addEdge(node, succ);
			}
		}
		
		return mutable;
	}
	
}