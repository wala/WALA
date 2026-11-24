package com.ibm.wala.util.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.util.graph.impl.DelegatingNumberedNodeManager;
import com.ibm.wala.util.graph.impl.NodeWithNumber;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class DelegatingNumberedNodeManagerTest {

  @Test
  void emptyIteratorThrowsOnNext() {
    DelegatingNumberedNodeManager<NodeWithNumber> manager = new DelegatingNumberedNodeManager<>();
    Iterator<NodeWithNumber> iterator = manager.iterator();

    assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
    assertThat(iterator).isExhausted();
  }

  @Test
  void iteratorThrowsAfterReturningLastNode() {
    DelegatingNumberedNodeManager<NodeWithNumber> manager = new DelegatingNumberedNodeManager<>();
    NodeWithNumber first = new NodeWithNumber();
    first.setGraphNodeId(2);
    manager.addNode(first);
    NodeWithNumber last = new NodeWithNumber();
    last.setGraphNodeId(5);
    manager.addNode(last);
    Iterator<NodeWithNumber> iterator = manager.iterator();

    assertThat(iterator.next()).isSameAs(first);
    assertThat(iterator.next()).isSameAs(last);
    assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
    assertThat(iterator).isExhausted();
  }
}
