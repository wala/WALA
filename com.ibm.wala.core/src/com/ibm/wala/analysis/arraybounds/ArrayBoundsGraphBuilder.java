package com.ibm.wala.analysis.arraybounds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.analysis.arraybounds.hypergraph.DirectedHyperEdge;
import com.ibm.wala.analysis.arraybounds.hypergraph.HyperNode;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.Operator;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;

/**
 * @see ArrayBoundsGraph
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
public class ArrayBoundsGraphBuilder {
	private final IR ir;
	/** Variables, which were already explored. */
	private final HashSet<Integer> foundVariables;
	private final DefUse defUse;

	private final ArrayBoundsGraph lowerBoundGraph;

	private final ArrayBoundsGraph upperBoundGraph;
	private final Set<SSAArrayReferenceInstruction> arrayReferenceInstructions;
	private final IBinaryOpInstruction.Operator SUB = IBinaryOpInstruction.Operator.SUB;

	private final IBinaryOpInstruction.Operator ADD = IBinaryOpInstruction.Operator.ADD;

	public ArrayBoundsGraphBuilder(IR ir) {
		this.ir = ir;

		this.foundVariables = new HashSet<>();
		this.defUse = new DefUse(ir);

		this.arrayReferenceInstructions = new HashSet<>();
		this.lowerBoundGraph = new ArrayBoundsGraph();
		this.upperBoundGraph = new ArrayBoundsGraph();

		this.findArrayAccess();
		this.exploreIr();
		this.addConstructionLength();

    this.lowerBoundGraph.updateNodeEdges();
    this.upperBoundGraph.updateNodeEdges();   
		
		this.lowerBoundGraph.postProcessConstants();
		this.upperBoundGraph.postProcessConstants();
		
    this.lowerBoundGraph.updateNodeEdges();
    this.upperBoundGraph.updateNodeEdges();   		
		
		bundleDeadEnds(this.lowerBoundGraph);
		bundleDeadEnds(this.upperBoundGraph);

		collapseNonPhiEdges(this.lowerBoundGraph);
		collapseNonPhiEdges(this.upperBoundGraph);

		this.lowerBoundGraph.updateNodeEdges();
		this.upperBoundGraph.updateNodeEdges();
	}

	private void addConstructionLength() {

		for (final Integer array : this.lowerBoundGraph.getArrayLength()
				.keySet()) {
			final Integer tmp = array;

			final SSAInstruction instruction = this.defUse.getDef(array);
			if (instruction != null) {
				instruction.visit(new Visitor() {
					@Override
					public void visitNew(SSANewInstruction instruction) {
					  //We only support arrays with dimension 1
						if (instruction.getNumberOfUses() == 1) {
  						final int constructionLength = instruction.getUse(0);
  						Integer arraysNode = ArrayBoundsGraphBuilder.this.lowerBoundGraph
  								.getArrayLength().get(tmp);
  						ArrayBoundsGraphBuilder.this.lowerBoundGraph.addEdge(
  								arraysNode, constructionLength);
  						arraysNode = ArrayBoundsGraphBuilder.this.upperBoundGraph
  								.getArrayLength().get(tmp);
  						ArrayBoundsGraphBuilder.this.upperBoundGraph.addEdge(
  								arraysNode, constructionLength);
  
  						ArrayBoundsGraphBuilder.this
  						.addPossibleConstant(constructionLength);
						}
					}
				});
			}
		}

	}

	/**
	 * Case 1: piRestrictor restricts the pi variable for upper/ lower bounds graph
	 * Given this code below, we want to create a hyper edge
	 * {piParent, piRestrictor} --&gt; {piVar}.
	 *
	 * If is op in {&lt;, &gt;} we now, that the distance from piRestrictor to piVar
	 * is +-1 as ( a &lt; b ) &lt;==&gt; ( a &lt;= b - 1), same with "&lt;".
	 * To be more precise we introduce a helper node and add
	 * {piRestrictor} -- (-)1 --&gt; {helper}
	 * {piParent, helper} --&gt; {piVar}
	 *
	 * Case 2: no restriction is given by the branch (i.e. the operator is not equal)
	 * {piParent} --&gt; {piVar}
	 *
	 * <code>if (piParent op piRestrictor) {piVar = piParent}</code>
	 *
	 * @param piVar
	 * @param piParent
	 * @param piRestrictor
	 * @param op
	 */
	private void addPiStructure(Integer piVar, Integer piParent,
			Integer piRestrictor, Operator op) {

		Integer helper;
		switch (op) {
		case EQ:
			this.upperBoundGraph.addPi(piVar, piParent, piRestrictor);
			this.lowerBoundGraph.addPi(piVar, piParent, piRestrictor);
			break;
		case NE:
			this.upperBoundGraph.addEdge(piParent, piVar);
			this.lowerBoundGraph.addEdge(piParent, piVar);
			break;
		case LE: // piVar <= piRestrictor
			this.upperBoundGraph.addPi(piVar, piParent, piRestrictor);

			this.lowerBoundGraph.addEdge(piParent, piVar);
			break;
		case GE: // piVar >= piRestrictor
			this.lowerBoundGraph.addPi(piVar, piParent, piRestrictor);

			this.upperBoundGraph.addEdge(piParent, piVar);
			break;
		case LT: // piVar < piRestrictor
			helper = this.upperBoundGraph.generateNewVar();
			this.upperBoundGraph.addAdditionEdge(piRestrictor, helper, -1);
			this.upperBoundGraph.addPi(piVar, piParent, helper);

			this.lowerBoundGraph.addEdge(piParent, piVar);
			break;
		case GT: // piVar > piRestrictor
			helper = this.lowerBoundGraph.generateNewVar();
			this.lowerBoundGraph.addAdditionEdge(piRestrictor, helper, 1);
			this.lowerBoundGraph.addPi(piVar, piParent, helper);

			this.upperBoundGraph.addEdge(piParent, piVar);
			break;
    default:
      throw new UnsupportedOperationException(String.format("unexpected operator %s", op));
		}

	}

	private void addPossibleConstant(int handle) {
		if (this.ir.getSymbolTable().isIntegerConstant(handle)) {
			final int value = this.ir.getSymbolTable().getIntValue(handle);
			this.lowerBoundGraph.addConstant(handle, value);
			this.upperBoundGraph.addConstant(handle, value);
		}
	}

	/**
	 * Connect all lose ends to the infinity node. See the description of
	 * {@link ArrayBoundsGraph} for why this is necessary.
	 *
	 * @param graph
	 */
	private static void bundleDeadEnds(ArrayBoundsGraph graph) {
		final Set<HyperNode<Integer>> nodes = new HashSet<>();
		nodes.addAll(graph.getNodes().values());

		for (final DirectedHyperEdge<Integer> edge : graph.getEdges()) {
			for (final HyperNode<Integer> node : edge.getDestination()) {
				nodes.remove(node);
			}
		}

		for (final HyperNode<Integer> node : nodes) {
			graph.markAsDeadEnd(node.getValue());
		}
	}

	/**
	 * To make construction of the hyper-graph more easy, we always add single
	 * edges and fuse them into one hyper-edge. Where necessary (Everywhere but
	 * incoming edges of phi nodes.)
	 *
	 * @param graph
	 */
	private static void collapseNonPhiEdges(ArrayBoundsGraph graph) {
		final Map<HyperNode<Integer>, DirectedHyperEdge<Integer>> inEdges = new HashMap<>();
		final Set<DirectedHyperEdge<Integer>> edges = new HashSet<>();
		edges.addAll(graph.getEdges());
		for (final DirectedHyperEdge<Integer> edge : edges) {
			assert edge.getDestination().size() == 1;

			final HyperNode<Integer> node = edge.getDestination().iterator()
					.next();
			if (!graph.getPhis().contains(node.getValue())) {
				if (inEdges.containsKey(node)) {
					final DirectedHyperEdge<Integer> inEdge = inEdges.get(node);
					assert inEdge.getWeight().equals(edge.getWeight());
					for (final HyperNode<Integer> src : edge.getSource()) {
						inEdge.getSource().add(src);
					}
					graph.getEdges().remove(edge);
				} else {
					inEdges.put(node, edge);
				}
			}
		}
	}

	/**
	 * Discovers predecessors and adds them to the graph.
	 *
	 * @param todo
	 * @param handle
	 */
	private void discoverPredecessors(final Stack<Integer> todo, int handle) {
		final SSAInstruction def = this.defUse.getDef(handle);
		if (def == null) {
			this.addPossibleConstant(handle);
		} else {
			def.visit(new Visitor() {
				@Override
				public void visitArrayLength(
						SSAArrayLengthInstruction instruction) {
					ArrayBoundsGraphBuilder.this.lowerBoundGraph
					.markAsArrayLength(instruction.getArrayRef(),
							instruction.getDef());
					ArrayBoundsGraphBuilder.this.upperBoundGraph
					.markAsArrayLength(instruction.getArrayRef(),
							instruction.getDef());
				}

				@Override
				public void visitBinaryOp(SSABinaryOpInstruction instruction) {
					if (instruction.getOperator() == ArrayBoundsGraphBuilder.this.SUB
							|| instruction.getOperator() == ArrayBoundsGraphBuilder.this.ADD) {
						final BinaryOpWithConstant op = BinaryOpWithConstant
								.create(instruction,
										ArrayBoundsGraphBuilder.this.ir);

						if (op != null) {
							todo.push(op.getOtherVar());
							int value = op.getConstantValue();
							if (instruction.getOperator() == ArrayBoundsGraphBuilder.this.SUB) {
								value = -value;
							}
							ArrayBoundsGraphBuilder.this.lowerBoundGraph
							.addAdditionEdge(op.getOtherVar(),
									instruction.getDef(), value);
							ArrayBoundsGraphBuilder.this.upperBoundGraph
							.addAdditionEdge(op.getOtherVar(),
									instruction.getDef(), value);

						}
					}
				}

				@Override
				public void visitPhi(SSAPhiInstruction instruction) {				  
				  int phi = instruction.getDef();
          ArrayBoundsGraphBuilder.this.lowerBoundGraph.addPhi(phi);
          ArrayBoundsGraphBuilder.this.upperBoundGraph.addPhi(phi);

          for (int i = 0; i < instruction.getNumberOfUses(); i++) {            
            int use = instruction.getUse(i);          
            todo.push(use);

            ArrayBoundsGraphBuilder.this.lowerBoundGraph.addEdge(use, phi);
            ArrayBoundsGraphBuilder.this.upperBoundGraph.addEdge(use, phi);
          }

				}

				@Override
				public void visitPi(SSAPiInstruction instruction) {
					final SSAConditionalBranchInstruction branch = (SSAConditionalBranchInstruction) instruction
							.getCause();
					assert branch.getNumberOfUses() == 2;

					final Integer piVar = instruction.getDef();
					final Integer piParent = instruction.getUse(0);

					final ConditionNormalizer cnd = new ConditionNormalizer(
							branch, piParent, ArrayBoundsGraphBuilder.this
							.isBranchTaken(instruction, branch));

					final Integer piRestrictor = cnd.getRhs();

					todo.push(piParent);
					todo.push(piRestrictor);

					ArrayBoundsGraphBuilder.this.addPiStructure(piVar,
							piParent, piRestrictor, cnd.getOp());
				}
			});
		}
	}

	private void exploreIr() {
		final Set<Integer> variablesUsedAsIndex = new HashSet<>();
		for (final Set<Integer> variables : this.lowerBoundGraph
				.getArrayAccess().values()) {
			variablesUsedAsIndex.addAll(variables);
		}

		for (final Integer variable : variablesUsedAsIndex) {
			this.startDFS(variable);
		}
	}

	private void findArrayAccess() {
		this.ir.visitNormalInstructions(new Visitor() {
			@Override
			public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
				ArrayBoundsGraphBuilder.this.lowerBoundGraph.markAsArrayAccess(
						instruction.getArrayRef(), instruction.getIndex());
				ArrayBoundsGraphBuilder.this.upperBoundGraph.markAsArrayAccess(
						instruction.getArrayRef(), instruction.getIndex());

				ArrayBoundsGraphBuilder.this.arrayReferenceInstructions
				.add(instruction);
			}

			@Override
			public void visitArrayStore(SSAArrayStoreInstruction instruction) {
				ArrayBoundsGraphBuilder.this.lowerBoundGraph.markAsArrayAccess(
						instruction.getArrayRef(), instruction.getIndex());
				ArrayBoundsGraphBuilder.this.upperBoundGraph.markAsArrayAccess(
						instruction.getArrayRef(), instruction.getIndex());

				ArrayBoundsGraphBuilder.this.arrayReferenceInstructions
				.add(instruction);
			}
		});

	}

	public Set<SSAArrayReferenceInstruction> getArrayReferenceInstructions() {
		return this.arrayReferenceInstructions;
	}

	public ArrayBoundsGraph getLowerBoundGraph() {
		return this.lowerBoundGraph;
	}

	public ArrayBoundsGraph getUpperBoundGraph() {
		return this.upperBoundGraph;
	}

	private boolean isBranchTaken(SSAPiInstruction pi,
			SSAConditionalBranchInstruction cnd) {
		final BasicBlock branchTargetBlock = this.ir.getControlFlowGraph()
				.getBlockForInstruction(cnd.getTarget());

		return branchTargetBlock.getNumber() == pi.getSuccessor();
	}

	/**
	 * Explore the DefUse-Chain with depth-first-search to add constraints to
	 * the given variable.
	 */
	private void startDFS(int index) {
		final Stack<Integer> todo = new Stack<>();
		todo.push(index);

		while (!todo.isEmpty()) {
			final int next = todo.pop();
			if (!this.foundVariables.contains(next)) {
				this.foundVariables.add(next);
				this.lowerBoundGraph.addNode(next);
				this.upperBoundGraph.addNode(next);

				this.discoverPredecessors(todo, next);
			}
		}

	}

}
