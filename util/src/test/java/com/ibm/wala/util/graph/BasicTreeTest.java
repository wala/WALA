package com.ibm.wala.util.graph;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BasicTreeTest {

  @Test
  void missingChildHasNullValue() {
    BasicTree<String> tree = BasicTree.make("root");

    assertThat(tree.getChildValue(0)).isNull();
  }

  @Test
  void returnsValueOfPresentChild() {
    BasicTree<String> tree = BasicTree.make("root");
    tree.setChild(2, BasicTree.make("child"));

    assertThat(tree.getChildValue(2)).isEqualTo("child");
  }

  @Test
  void unsetChildSlotReturnsNull() {
    BasicTree<String> tree = BasicTree.make("root");
    tree.setChild(2, BasicTree.make("child"));

    assertThat(tree.getChild(1)).isNull();
  }
}
