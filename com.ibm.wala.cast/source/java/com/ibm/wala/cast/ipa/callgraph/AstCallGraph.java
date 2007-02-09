/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.cast.ir.cfg.*;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cfg.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.*;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.warnings.WarningSet;

import java.util.*;

public class AstCallGraph extends ExplicitCallGraph {
  private final WarningSet warnings;

  public AstCallGraph(ClassHierarchy cha, AnalysisOptions options, WarningSet warnings) {
    super(cha, options);
    this.warnings = warnings;
  }

  public class AstFakeRoot extends FakeRootMethod {

    public AstFakeRoot(ClassHierarchy cha, AnalysisOptions options) {
      super(cha, options);
    }

    public InducedCFG makeControlFlowGraph() {
      return new AstInducedCFG(getStatements(warnings), this, Everywhere.EVERYWHERE);
    }

    public AstLexicalRead addGlobalRead(String name) {
      AstLexicalRead s = new AstLexicalRead(nextLocal++, null, name);
      statements.add(s);
      return s;
    }
  }

  public abstract class ScriptFakeRoot extends AstFakeRoot {

    public ScriptFakeRoot(ClassHierarchy cha, AnalysisOptions options) {
      super(cha, options);
    }

    public abstract SSAAbstractInvokeInstruction addDirectCall(int functionVn, int[] argVns, CallSiteReference callSite);

  }

  class AstCGNode extends ExplicitNode {
    private Set<Function<Object,Object>> callbacks;

    private AstCGNode(IMethod method, Context context) {
      super(method, context);
    }

    private void fireCallbacks() {
      if (callbacks != null) {
        boolean done = false;
        while (!done) {
          try {
            for (Iterator<Function<Object,Object>> x = callbacks.iterator(); x.hasNext();) {
              x.next().apply(null);
            }
          } catch (ConcurrentModificationException e) {
            done = false;
            continue;
          }
          done = true;
        }
      }
    }

    public void addCallback(Function<Object,Object> callback) {
      if (callbacks == null)
        callbacks = new HashSet<Function<Object,Object>>(1);
      callbacks.add(callback);
    }

    private void fireCallbacksTransitive() {
      for (Iterator<CGNode> nodes = DFS.iterateFinishTime(AstCallGraph.this, new NonNullSingletonIterator<CGNode>(this)); nodes
          .hasNext();) {
        ((AstCGNode) nodes.next()).fireCallbacks();
      }
    }

    public boolean addTarget(CallSiteReference site, CGNode node) {
      if (super.addTarget(site, node)) {
        fireCallbacksTransitive();
        return true;
      } else {
        return false;
      }
    }
  }

  protected ExplicitNode makeNode(IMethod method, Context context) {
    return new AstCGNode(method, context);
  }

  protected CGNode makeFakeRootNode() {
    return findOrCreateNode(new AstFakeRoot(cha, options), Everywhere.EVERYWHERE);
  }

}
