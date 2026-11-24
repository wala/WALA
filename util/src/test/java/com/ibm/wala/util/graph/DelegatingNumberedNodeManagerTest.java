package com.ibm.wala.util.graph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    assertThrows(NoSuchElementException.class, iterator::next);
    assertFalse(iterator.hasNext());
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

    assertSame(first, iterator.next());
    assertSame(last, iterator.next());
    assertThrows(NoSuchElementException.class, iterator::next);
    assertFalse(iterator.hasNext());
  }
}
