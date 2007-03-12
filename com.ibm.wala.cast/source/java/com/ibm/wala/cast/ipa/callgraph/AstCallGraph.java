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

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.ir.cfg.AstInducedCFG;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.Function;
import com.ibm.wala.util.warnings.WarningSet;

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

    private boolean hasCallback(Function<Object,Object> callback) {
      return callbacks != null && callbacks.contains(callback);
    }

    private boolean hasAllCallbacks(Set<Function<Object,Object>> callbacks) {
      return callbacks != null && callbacks.containsAll(callbacks);
    }

    public void addCallback(Function<Object,Object> callback) {
      if (! hasCallback(callback)) {
	if (callbacks == null) {
	  callbacks = new HashSet<Function<Object,Object>>(1);
	}

	callbacks.add(callback);

	for(Iterator ps = getPredNodes(this); ps.hasNext(); ) {
	  ((AstCGNode)ps.next()).addCallback(callback);
	}
      }
    }

    public void addAllCallbacks(Set<Function<Object,Object>> callback) {
      if (! hasAllCallbacks(callbacks)) {
	if (callbacks == null) {
	  callbacks = new HashSet<Function<Object,Object>>(1);
	}

	callbacks.addAll(callbacks);

	for(Iterator ps = getPredNodes(this); ps.hasNext(); ) {
	  ((AstCGNode)ps.next()).addAllCallbacks(callbacks);
	}
      }
    }

    public boolean addTarget(CallSiteReference site, CGNode node) {
      if (super.addTarget(site, node)) {
	if (((AstCGNode)node).callbacks != null) {
	  ((AstCGNode)node).fireCallbacks();
	  addAllCallbacks(((AstCGNode)node).callbacks);
	}
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
