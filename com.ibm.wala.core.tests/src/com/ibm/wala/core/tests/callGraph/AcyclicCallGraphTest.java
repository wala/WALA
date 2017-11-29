package com.ibm.wala.core.tests.callGraph;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntPair;

public class AcyclicCallGraphTest extends WalaTestCase {

  @Test public void testNList() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA,
        CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha,
        "Lrecurse/NList");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
  
    CallGraph cg = CallGraphTestUtil.buildZeroCFA(options, new AnalysisCacheImpl(), cha, scope, false);

    IBinaryNaturalRelation backEdges = Acyclic.computeBackEdges(cg, cg.getFakeRootNode());
    
    Assert.assertTrue("NList should have cycles", backEdges.iterator().hasNext()); 
    
    Map<CGNode, Set<CGNode>> cgBackEdges = HashMapFactory.make();
    for (IntPair p : backEdges) {
      CGNode src = cg.getNode(p.getX());
      if (!cgBackEdges.containsKey(src)) {
        cgBackEdges.put(src, HashSetFactory.<CGNode>make());
      }
      cgBackEdges.get(src).add(cg.getNode(p.getY()));
    }
    
    PrunedCallGraph pcg = new PrunedCallGraph(cg, Iterator2Collection.toSet(cg.iterator()), cgBackEdges);
    
    Assert.assertTrue("cycles should be gone", !Acyclic.computeBackEdges(pcg, pcg.getFakeRootNode()).iterator().hasNext());
  }

}
