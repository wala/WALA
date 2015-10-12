package com.ibm.wala.analysis.arraybounds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.analysis.arraybounds.hypergraph.DirectedHyperEdge;
import com.ibm.wala.analysis.arraybounds.hypergraph.DirectedHyperGraph;
import com.ibm.wala.analysis.arraybounds.hypergraph.HyperNode;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight.Type;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.edgeweights.AdditiveEdgeWeight;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.edgeweights.EdgeWeight;

/**
 * Some thoughts about implementation details, not mentioned in [1]:
 *
 * As it is written The paper describes, that the distance is equal to the
 * shortest hyper path. But what if we don't know anything about a variable
 * (i.e. it is returned by a method)? There will be no path at all, the distance
 * should be unlimited.
 *
 * Initializing all nodes with -infinity instead of infinity, seems to work at
 * first glance, as we also have hyper edges with more than one source, which
 * cause the maximum to be propagated instead of minimum. However, this will not
 * work, as loops will not get updated properly.
 *
 * We need to make sure, that only nodes, which are not connected to the source
 * of shortest path computation are set to infinity. To do so, it is enough to
 * set nodes, which don't have a predecessor to infinity. (Nodes in cycles will
 * always have an ancestor, which is not part of the cycle. So all nodes are
 * either connected to the source, or a node with no predecessor.)
 *
 * In this implementation this is done, by adding an infinity node and connect
 * all lose ends to it (see
 * {@link ArrayBoundsGraphBuilder#bundleDeadEnds(ArrayBoundsGraph)}). Note, that
 * array length and the zero node are dead ends, if they are not the source of a
 * shortest path computation. To prevent changing the inequality graph,
 * depending on which source is used, a kind of trap door construct is used (See
 * {@link ArrayBoundsGraph#createSourceVar(Integer)}).
 *
 * There are some variations, but these are minor changes to improve results:
 * <ul>
 * <li>handling of constants (see
 * {@link ArrayBoundsGraph#addConstant(Integer, Integer)})
 * <li>pi nodes (see {@link ArrayBoundsGraph#addPhi(Integer, Integer, Integer)})
 * <li>array length nodes (see {@link ArrayBoundsGraph#arrayLength})
 * </ul>
 *
 * [1] Bodík, Rastislav, Rajiv Gupta, and Vivek Sarkar.
 * "ABCD: eliminating array bounds checks on demand." ACM SIGPLAN Notices. Vol.
 * 35. No. 5. ACM, 2000.
 * 
 * @author Stephan Gocht <stephan@gobro.de>
 */
public class ArrayBoundsGraph extends DirectedHyperGraph<Integer> {
	/**
	 * We need a ssa variable representing zero. So we just use an integer,
	 * which is never produced by ssa generation
	 */
	public final static Integer ZERO = -1;
	/**
	 * We need a ssa variable representing unlimited (values we don't know
	 * anything about). So we just use an integer, which is never produced by
	 * ssa generation
	 */
	public final static Integer UNLIMITED = -2;

	/**
	 * Maps each array variable to a set of variables, which are used as Index
	 * for accessing that array
	 */
	private final HashMap<Integer, Set<Integer>> arrayAccess;

	/**
	 * Maps each array variable to a node which is parent to all variables, that
	 * contain the array length
	 */
	private final HashMap<Integer, Integer> arrayLength;

	private final HashSet<Integer> phis;

	/**
	 * For simplicity we introduce extra variables, for arrayLength, to have a
	 * unique node representing the array length, even if the length is accessed
	 * more than once in the code.
	 *
	 * Start with -3 so it is unequal to other constants
	 */
	private Integer arrayCounter = -3;

	public ArrayBoundsGraph() {
		this.arrayAccess = new HashMap<>();
		this.arrayLength = new HashMap<>();
		this.phis = new HashSet<>();
		this.addNode(UNLIMITED);
		this.phis.add(UNLIMITED);
		this.createSourceVar(ZERO);
	}

	public void addAdditionEdge(Integer src, Integer dst, Integer value) {
		this.addNode(src);
		final HyperNode<Integer> srcNode = this.getNodes().get(src);

		this.addNode(dst);
		final HyperNode<Integer> dstNode = this.getNodes().get(dst);

		Weight weight;
		if (value == 0) {
			weight = Weight.ZERO;
		} else {
			weight = new Weight(Type.NUMBER, value);
		}

		final EdgeWeight edgeWeight = new AdditiveEdgeWeight(weight);

		final DirectedHyperEdge<Integer> edge = new DirectedHyperEdge<Integer>();
		edge.getDestination().add(dstNode);
		edge.getSource().add(srcNode);
		edge.setWeight(edgeWeight);

		this.getEdges().add(edge);
	}

	public void addArray(Integer array) {
		this.getArrayNode(array);
	}

	/**
	 * Add variable as constant with value value.
	 *
	 * This will create the following construct: [zero] -(value)-> [h1] -0- >
	 * [variable] -(-value)-> [h2] -0-> [zero].
	 *
	 * The bidirectional linking, allows things like
	 *
	 * <pre>
	 * int[] a = new int[2]();
	 * a[0] = 1;
	 * </pre>
	 *
	 * to work properly. h1, h2 are helper nodes: [zero] and [variable] may have
	 * other predecessors, this will cause their in edges to be merged to a
	 * single hyper edge with weight zero. The helper nodes are inserted to keep
	 * the proper distance from [zero].
	 *
	 * @param variable
	 * @param value
	 */
	public void addConstant(Integer variable, Integer value) {
		final Integer helper1 = this.generateNewVar();
		final Integer helper2 = this.generateNewVar();

		this.addAdditionEdge(ZERO, helper1, value);
		this.addEdge(helper1, variable);
		this.addAdditionEdge(variable, helper2, -value);
		this.addEdge(helper2, ZERO);
	}

	public void addEdge(Integer src, Integer dst) {
		this.addAdditionEdge(src, dst, 0);
	}

	public HyperNode<Integer> addNode(Integer value) {
		HyperNode<Integer> result;
		if (!this.getNodes().keySet().contains(value)) {
			result = new HyperNode<Integer>(value);
			this.getNodes().put(value, result);
		} else {
			result = this.getNodes().get(value);
		}
		return result;
	}

	public void addPhi(Integer dst, Integer src1, Integer src2) {
		this.phis.add(dst);
		this.addEdge(src1, dst);
		this.addEdge(src2, dst);
	}

	public void addPi(Integer dst, Integer src1, Integer src2) {
		this.addEdge(src1, dst);
		this.addEdge(src2, dst);
	}

	/**
	 * Adds var as source var. A source var is a variable, which can be used as
	 * source for shortest path computation.
	 *
	 * This will create the following construct: [unlimited] -> [var] -> [var]
	 * -(unlimited)-> [unlimited]
	 *
	 * This is a trap door construct: if [var] is not set to 0 it will get the
	 * value unlimited, if [var] is set to 0 it will stay 0.
	 *
	 * @param var
	 */
	public void createSourceVar(Integer var) {
		this.addNode(var);

		final HyperNode<Integer> arrayNode = this.getNodes().get(var);
		final HyperNode<Integer> unlimitedNode = this.getNodes().get(UNLIMITED);

		final DirectedHyperEdge<Integer> edge = new DirectedHyperEdge<>();
		edge.setWeight(new AdditiveEdgeWeight(Weight.UNLIMITED));
		edge.getSource().add(arrayNode);
		edge.getDestination().add(unlimitedNode);
		this.getEdges().add(edge);

		this.addEdge(UNLIMITED, var);
		this.addEdge(var, var);
	}

	public Integer generateNewVar() {
		final int result = this.arrayCounter;
		this.arrayCounter += -1;
		return result;
	}

	public HashMap<Integer, Set<Integer>> getArrayAccess() {
		return this.arrayAccess;
	}

	public HashMap<Integer, Integer> getArrayLength() {
		return this.arrayLength;
	}

	public Integer getArrayNode(Integer array) {
		Integer arrayVar;
		if (!this.arrayLength.containsKey(array)) {
			arrayVar = this.generateNewVar();
			this.arrayLength.put(array, arrayVar);
			this.createSourceVar(arrayVar);
		} else {
			arrayVar = this.arrayLength.get(array);
		}
		return arrayVar;
	}

	public HashSet<Integer> getPhis() {
		return this.phis;
	}

	public void markAsArrayAccess(Integer array, Integer index) {
		Set<Integer> indices;
		if (!this.arrayAccess.containsKey(array)) {
			indices = new HashSet<Integer>();
			this.arrayAccess.put(array, indices);
		} else {
			indices = this.arrayAccess.get(array);
		}
		indices.add(index);
		this.addArray(array);
	}

	/**
	 * Mark variable as length for array.
	 *
	 * @param array
	 * @param variable
	 */
	public void markAsArrayLength(Integer array, Integer variable) {
		this.addEdge(this.getArrayNode(array), variable);
	}

	public void markAsDeadEnd(Integer variable) {
		this.addEdge(UNLIMITED, variable);
	}
}
