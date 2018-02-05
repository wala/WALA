/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.examples.analysis.dataflow;

import java.io.IOException;

import org.junit.Assert;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.io.FileProvider;

public class InitializerTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    
    AnalysisScope scope = null;
    try {
      scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA,
          (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"), InitializerTest.class.getClassLoader());
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    IClassHierarchy cha = null;
    
    try {
      cha = ClassHierarchyFactory.make(scope);
    } catch (ClassHierarchyException e) {
      e.printStackTrace();
    }
    
    
    
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, "LstaticInit/TestStaticInit");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);
    CallGraph cg = null;
    try {
      cg = builder.makeCallGraph(options, null);
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (CallGraphBuilderCancelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    System.out.println("Start");
    
    StaticInitializer reachingDefs = new StaticInitializer(cg);
    TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, IClass> result = reachingDefs.analyze();
    ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph = reachingDefs.getSupergraph();
    for (BasicBlockInContext<IExplodedBasicBlock> bb : supergraph) {
      if (bb.getNode().toString().contains("doNothing")) {
//        System.out.println("Do!");
        IExplodedBasicBlock delegate = bb.getDelegate();
        if (delegate.getNumber() == 4) {
          IntSet solution = result.getResult(bb);
          IntIterator intIterator = solution.intIterator();
          while (intIterator.hasNext()) {
            int next = intIterator.next();
            System.out.println(reachingDefs.getDomain().getMappedObject(next));
          }
          Assert.assertEquals(3, solution.size());
        }
      }
    }
    
    System.out.println("End");
    
  }
    


}
