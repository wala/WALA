package com.ibm.wala.analysis.arraybounds;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.arraybounds.hypergraph.HyperNode;
import com.ibm.wala.analysis.arraybounds.hypergraph.algorithms.ShortestPath;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.NormalOrder;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.ReverseOrder;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.util.ssa.InstructionByIIndexMap;

/**
 * The array out of bounds analysis uses the inequality graph as described in
 * [1]. And a shortest path computation as suggested ibid. as possible solver
 * for the inequality graph.
 *
 * [1] Bodík, Rastislav, Rajiv Gupta, and Vivek Sarkar.
 * "ABCD: eliminating array bounds checks on demand." ACM SIGPLAN Notices. Vol.
 * 35. No. 5. ACM, 2000.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class ArrayOutOfBoundsAnalysis {
  public enum UnnecessaryCheck {
    NONE, UPPER, LOWER, BOTH;

    public UnnecessaryCheck union(UnnecessaryCheck other) {
      final Set<UnnecessaryCheck> set = new HashSet<>();
      set.add(this);
      set.add(other);
      set.remove(NONE);
      if (set.contains(BOTH) || (set.contains(UPPER) && set.contains(LOWER))) {
        return BOTH;
      } else if (set.size() == 0) {
        return NONE;
      } else if (set.size() == 1) {
        return set.iterator().next();
      } else {
        throw new RuntimeException("Case that should not happen, this method is implemented wrong.");
      }
    }
  }

  private ArrayBoundsGraph lowerBoundGraph;
  private ArrayBoundsGraph upperBoundGraph;

  /**
   * List of variables, that are used for array access and if they are
   * neccessary
   */
  private final Map<SSAArrayReferenceInstruction, UnnecessaryCheck> boundsCheckUnnecessary;

  /**
   * Create and perform the array out of bounds analysis.
   * 
   * Make sure, the given IR was created with pi nodes for each variable, that
   * is part of a branch instruction! Otherwise the results will be poor.
   * 
   * @param ir
   */
  public ArrayOutOfBoundsAnalysis(IR ir) {
    this.boundsCheckUnnecessary = new InstructionByIIndexMap<>();
    this.buildInequalityGraphs(ir);

    this.computeLowerBound();
    this.computeUpperBounds();

    this.lowerBoundGraph = null;
    this.upperBoundGraph = null;
  }

  private void addUnnecessaryCheck(SSAArrayReferenceInstruction instruction, UnnecessaryCheck checkToAdd) {
    final UnnecessaryCheck oldCheck = this.boundsCheckUnnecessary.get(instruction);
    final UnnecessaryCheck newCheck = oldCheck.union(checkToAdd);
    this.boundsCheckUnnecessary.put(instruction, newCheck);
  }

  private void buildInequalityGraphs(IR ir) {
    ArrayBoundsGraphBuilder builder = new ArrayBoundsGraphBuilder(ir);
    this.lowerBoundGraph = builder.getLowerBoundGraph();
    this.upperBoundGraph = builder.getUpperBoundGraph();

    for (final SSAArrayReferenceInstruction instruction : builder.getArrayReferenceInstructions()) {
      this.boundsCheckUnnecessary.put(instruction, UnnecessaryCheck.NONE);
    }
    builder = null;
  }

  /**
   * compute lower bound
   */
  private void computeLowerBound() {
    final HyperNode<Integer> zero = this.lowerBoundGraph.getNodes().get(ArrayBoundsGraph.ZERO);
    ShortestPath.compute(this.lowerBoundGraph, zero, new NormalOrder());

    for (final SSAArrayReferenceInstruction instruction : this.boundsCheckUnnecessary.keySet()) {
      
      Weight weight = this.lowerBoundGraph.getVariableWeight(instruction.getIndex());
      
      if (weight.getType() == Weight.Type.NUMBER && weight.getNumber() >= 0) {
        this.addUnnecessaryCheck(instruction, UnnecessaryCheck.LOWER);
      }
    }
  }

  /**
   * compute upper bound for each array
   */
  private void computeUpperBounds() {
    final Map<Integer, Integer> arrayLengths = this.upperBoundGraph.getArrayLength();
    for (final Integer array : arrayLengths.keySet()) {
      final HyperNode<Integer> arrayNode = this.upperBoundGraph.getNodes().get(arrayLengths.get(array));

      ShortestPath.compute(this.upperBoundGraph, arrayNode, new ReverseOrder());

      for (final SSAArrayReferenceInstruction instruction : this.boundsCheckUnnecessary.keySet()) {
        if (instruction.getArrayRef() == array) {
          Weight weight = this.upperBoundGraph.getVariableWeight(instruction.getIndex());

          if (weight.getType() == Weight.Type.NUMBER && weight.getNumber() <= -1) {
            this.addUnnecessaryCheck(instruction, UnnecessaryCheck.UPPER);
          }
        }
      }
    }
  }

  /**
   * @return for each array reference instruction (load or store), if both,
   *         lower bound, upper bound or no check is unnecessary.
   */
  public Map<SSAArrayReferenceInstruction, UnnecessaryCheck> getBoundsCheckNecessary() {
    return this.boundsCheckUnnecessary;
  }
}
