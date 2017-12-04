/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.modref.JavaScriptModRef;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.core.tests.slicer.SlicerTest;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;

public abstract class TestJavaScriptSlicer extends TestJSCallGraphShape {

  @Test
  public void testSimpleData() throws IOException, WalaException, IllegalArgumentException, CancelException {
    Collection<Statement> result = slice("slice1.js", DataDependenceOptions.FULL, ControlDependenceOptions.NONE); 
       
    for(Statement r : result) {
      System.err.println(r);
    }
    
    Assert.assertEquals(0, SlicerTest.countConditionals(result));

  }

  @Test
  public void testSimpleAll() throws IOException, WalaException, IllegalArgumentException, CancelException {
    Collection<Statement> result = slice("slice1.js", DataDependenceOptions.FULL, ControlDependenceOptions.FULL); 
    
    for(Statement r : result) {
      System.err.println(r);
    }
    
    Assert.assertEquals(2, SlicerTest.countConditionals(result));

  }

  @Test
  public void testSimpleControl() throws IOException, WalaException, IllegalArgumentException, CancelException {
    Collection<Statement> result = slice("slice1.js", DataDependenceOptions.NONE, ControlDependenceOptions.FULL); 
    
    for(Statement r : result) {
      System.err.println(r);
    }
    
    Assert.assertEquals(1, SlicerTest.countConditionals(result));
  }

  private Collection<Statement> slice(String file, DataDependenceOptions data, ControlDependenceOptions ctrl) throws IOException, WalaException, CancelException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", file);
    CallGraph CG = B.makeCallGraph(B.getOptions());
 
    SDG<InstanceKey> sdg = new SDG<>(CG, B.getPointerAnalysis(), new JavaScriptModRef<>(), data, ctrl);

    final Collection<Statement> ss = findTargetStatement(CG);
    Collection<Statement> result = Slicer.computeBackwardSlice(sdg, ss);
    return result;
  }

  private Collection<Statement> findTargetStatement(CallGraph CG) {
    final Collection<Statement> ss = HashSetFactory.make();
    for(CGNode n : getNodes(CG, "suffix:_slice_target_fn")) {
      for(CGNode caller : Iterator2Iterable.make(CG.getPredNodes(n))) {
        for(CallSiteReference site : Iterator2Iterable.make(CG.getPossibleSites(caller, n))) {
          caller.getIR().getCallInstructionIndices(site).foreach(x -> ss.add(new NormalStatement(caller, x)));
        }
      }
    }
    return ss;
  }
}
