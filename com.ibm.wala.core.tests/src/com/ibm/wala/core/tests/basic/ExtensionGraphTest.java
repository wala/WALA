package com.ibm.wala.core.tests.basic;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.ExtensionGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.graph.traverse.SCCIterator;

public class ExtensionGraphTest {

  private static NumberedGraph<String> makeBaseGraph() {
    NumberedGraph<String> base = SlowSparseNumberedGraph.make();
    
    base.addNode("A");
    base.addNode("B");
    base.addNode("C");
    base.addNode("D");
    base.addNode("E");
    base.addNode("F");
    base.addNode("G");
    base.addNode("H");
    
    base.addEdge("A", "B");
    base.addEdge("B", "C");
    base.addEdge("A", "D");
    base.addEdge("D", "E");
    base.addEdge("A", "F");
    base.addEdge("F", "G");
    base.addEdge("C", "H");
    base.addEdge("E", "H");
    base.addEdge("G", "H");

    return base;
  }
  
  private static void augmentA(NumberedGraph<String> base) {
    base.addEdge("C", "B");
    base.addEdge("E", "D");
    base.addEdge("G", "F");
  }
  
  private static void augmentB(NumberedGraph<String> base) {
    base.addNode("I");
    base.addNode("J");
    
    base.addEdge("I", "J");
    base.addEdge("A", "I");
    base.addEdge("H", "J");
  }
  
  private static void augmentC(NumberedGraph<String> base) {
    base.addEdge("H", "A");
  }
  
  @Test
  public void testAugment() {
    NumberedGraph<String> base = makeBaseGraph();
    Assert.assertEquals("base has 8 SCCs", 8, IteratorUtil.count(new SCCIterator<String>(base)));

    NumberedGraph<String> x = new ExtensionGraph<String>(base);
    augmentA(x);
    Assert.assertEquals("base+A has 5 SCCs", 5, IteratorUtil.count(new SCCIterator<String>(x)));
    Assert.assertEquals("base has 8 SCCs", 8, IteratorUtil.count(new SCCIterator<String>(base)));

    NumberedGraph<String> y = new ExtensionGraph<String>(x);
    augmentB(y);
    Assert.assertEquals("base+A+B has 7 SCCs", 7, IteratorUtil.count(new SCCIterator<String>(y)));
    Assert.assertEquals("base+A has 5 SCCs", 5, IteratorUtil.count(new SCCIterator<String>(x)));
    Assert.assertEquals("base has 8 SCCs", 8, IteratorUtil.count(new SCCIterator<String>(base)));

    NumberedGraph<String> z = new ExtensionGraph<String>(y);
    augmentC(z);
    Assert.assertEquals("base+A+B+C has 3 SCCs", 3, IteratorUtil.count(new SCCIterator<String>(z)));
    Assert.assertEquals("base+A+B has 7 SCCs", 7, IteratorUtil.count(new SCCIterator<String>(y)));
    Assert.assertEquals("base+A has 5 SCCs", 5, IteratorUtil.count(new SCCIterator<String>(x)));
    Assert.assertEquals("base has 8 SCCs", 8, IteratorUtil.count(new SCCIterator<String>(base)));
  }
}
