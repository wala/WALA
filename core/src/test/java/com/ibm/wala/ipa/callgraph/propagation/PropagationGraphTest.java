package com.ibm.wala.ipa.callgraph.propagation;

import static com.ibm.wala.util.collections.IteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import org.junit.jupiter.api.Test;

class PropagationGraphTest {
  private final PropagationGraph graph = new PropagationGraph();
  private final PointsToSetVariable source = new PointsToSetVariable(new Key("source"));
  private final PointsToSetVariable target = new PointsToSetVariable(new Key("target"));

  @Test
  void emptyStatementIteratorThrowsOnExhaustion() {
    assertExhausted(graph.getStatements());
  }

  @Test
  void statementIteratorThrowsAfterLastImplicitEquation() {
    AssignEquation equation = addAssignment();
    Iterator<?> iterator = graph.getStatements();

    assertThat(iterator.next()).isEqualTo(equation);
    assertExhausted(iterator);
  }

  @Test
  void removedImplicitEquationLeavesExhaustedIterator() {
    AssignEquation equation = addAssignment();
    graph.removeStatement(equation);

    assertExhausted(graph.getStatements());
  }

  @Test
  void emptyVariableSuccessorIteratorThrowsOnExhaustion() {
    addAssignment();

    assertExhausted(graph.getFlowGraphIncludingImplicitConstraints().getSuccNodes(target));
  }

  @Test
  void variableSuccessorIteratorThrowsAfterLastVariable() {
    addAssignment();
    Iterator<PointsToSetVariable> iterator =
        graph.getFlowGraphIncludingImplicitConstraints().getSuccNodes(source);

    assertThat(iterator.next()).isSameAs(target);
    assertExhausted(iterator);
  }

  @Test
  void emptyFilteredPredecessorIteratorThrowsOnExhaustion() {
    addAssignment();

    assertExhausted(graph.getAssignmentGraph().getPredNodes(source));
  }

  @Test
  void filteredPredecessorIteratorThrowsAfterLastVariable() {
    addAssignment();
    Iterator<PointsToSetVariable> iterator = graph.getAssignmentGraph().getPredNodes(target);

    assertThat(iterator.next()).isSameAs(source);
    assertExhausted(iterator);
  }

  @Test
  void emptyFilteredSuccessorIteratorThrowsOnExhaustion() {
    addAssignment();

    assertExhausted(graph.getAssignmentGraph().getSuccNodes(target));
  }

  @Test
  void filteredSuccessorIteratorThrowsAfterLastVariable() {
    addAssignment();
    Iterator<PointsToSetVariable> iterator = graph.getAssignmentGraph().getSuccNodes(source);

    assertThat(iterator.next()).isSameAs(target);
    assertExhausted(iterator);
  }

  private AssignEquation addAssignment() {
    AssignEquation equation = new AssignEquation(target, source);
    graph.addStatement(equation);
    return equation;
  }

  private record Key(String name) implements PointerKey {}
}
