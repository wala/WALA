/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
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
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.wala.cast.ipa.lexical.TransitiveLexicalAccesses;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;

public abstract class TestTransitiveLexicalAccesses {

  @Test
  public void testSimpleLexical() throws IOException, IllegalArgumentException, CancelException {
    JSCFABuilder b = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "simple-lexical.js");
    CallGraph CG = b.makeCallGraph(b.getOptions());    
    TransitiveLexicalAccesses lexAccesses = TransitiveLexicalAccesses.make(CG, b.getPointerAnalysis());
    Map<CGNode, OrdinalSet<Pair<CGNode, String>>> result = lexAccesses.computeLexVarsRead();
    for (CGNode n : result.keySet()) {
      if (n.toString().contains("Node: <Code body of function Ltests/simple-lexical.js/outer/inner>")) {
        // function "inner" reads exactly x and z 
        OrdinalSet<Pair<CGNode, String>> readVars = result.get(n);
        Assert.assertEquals(2, readVars.size());
        Assert.assertEquals("[[Node: <Code body of function Ltests/simple-lexical.js/outer> Context: Everywhere,x], [Node: <Code body of function Ltests/simple-lexical.js/outer> Context: Everywhere,z]]", readVars.toString());
      }
      if (n.toString().contains("Node: <Code body of function Ltests/simple-lexical.js/outer/inner2>")) {
        // function "inner3" reads exactly innerName, inner3, and x and z via callees
        OrdinalSet<Pair<CGNode, String>> readVars = result.get(n);
        Assert.assertEquals(4, readVars.size());
        Assert.assertEquals("[[Node: <Code body of function Ltests/simple-lexical.js/outer> Context: Everywhere,inner3], [Node: <Code body of function Ltests/simple-lexical.js/outer> Context: Everywhere,innerName], [Node: <Code body of function Ltests/simple-lexical.js/outer> Context: Everywhere,x], [Node: <Code body of function Ltests/simple-lexical.js/outer> Context: Everywhere,z]]", readVars.toString());
      }
    }
  }

}
