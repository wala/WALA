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
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.ir.cfg.AstInducedCFG;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.functions.Function;

public class AstCallGraph extends ExplicitCallGraph {
  public AstCallGraph(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
    super(cha, options, cache);
  }

  public static class AstFakeRoot extends AbstractRootMethod {

    public AstFakeRoot(MethodReference rootMethod, IClass declaringClass, IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
      super(rootMethod, declaringClass, cha, options, cache);
    }

    public AstFakeRoot(MethodReference rootMethod, IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
      super(rootMethod, cha, options, cache);
    }

    public InducedCFG makeControlFlowGraph(SSAInstruction[] statements) {
      return new AstInducedCFG(statements, this, Everywhere.EVERYWHERE);
    }

    public AstLexicalRead addGlobalRead(TypeReference type, String name) {
      AstLexicalRead s = new AstLexicalRead(nextLocal++, null, name);
      statements.add(s);
      return s;
    }
  }

  public static abstract class ScriptFakeRoot extends AstFakeRoot {

    public ScriptFakeRoot(MethodReference rootMethod, IClass declaringClass, IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
      super(rootMethod, declaringClass, cha, options, cache);
    }

    public ScriptFakeRoot(MethodReference rootMethod, IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
      super(rootMethod, cha, options, cache);
    }

    public abstract SSAAbstractInvokeInstruction addDirectCall(int functionVn, int[] argVns, CallSiteReference callSite);

  }

  public class AstCGNode extends ExplicitNode {
    private Set<Function<Object, Object>> callbacks;

    private boolean lexicalScopingChanges = false;
    
    private IR cachedIR;
    
    private DefUse cachedDU;
    
    private AstCGNode(IMethod method, Context context) {
      super(method, context);
    }

    private void fireCallbacks() {
      if (callbacks != null) {
        boolean done = false;
        while (!done) {
          try {
            for (Iterator<Function<Object, Object>> x = callbacks.iterator(); x.hasNext();) {
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

    private boolean hasCallback(Function<Object, Object> callback) {
      return callbacks != null && callbacks.contains(callback);
    }

    private boolean hasAllCallbacks(Set<Function<Object, Object>> callbacks) {
      return callbacks != null && callbacks.containsAll(callbacks);
    }

    public void addCallback(Function<Object, Object> callback) {
      if (!hasCallback(callback)) {
        if (callbacks == null) {
          callbacks = HashSetFactory.make(1);
        }

        callbacks.add(callback);

        for (Iterator ps = getCallGraph().getPredNodes(this); ps.hasNext();) {
          ((AstCGNode) ps.next()).addCallback(callback);
        }
      }
    }

    public void addAllCallbacks(Set<Function<Object, Object>> callback) {
      if (!hasAllCallbacks(callbacks)) {
        if (callbacks == null) {
          callbacks = HashSetFactory.make(1);
        }

        callbacks.addAll(callback);

        for (Iterator ps = getCallGraph().getPredNodes(this); ps.hasNext();) {
          ((AstCGNode) ps.next()).addAllCallbacks(callback);
        }
      }
    }

    public void setLexicallyMutatedIR(IR ir) {
      lexicalScopingChanges = true;
      cachedIR = ir;
      cachedDU = null;
    }
    
    public void clearMutatedCache(CallSiteReference cs) {
      targets.remove(cs.getProgramCounter());
    }
    
    public IR getLexicallyMutatedIR() {
      if (lexicalScopingChanges) {
        return cachedIR;
      } else {
        return null;
      }
    }
    
    public DefUse getLexicallyMutatedDU() {
      if (lexicalScopingChanges) {
        if (cachedDU == null) {
          cachedDU = new DefUse(cachedIR);
        }
        return cachedDU;
      } else {
        return null;
      }
    }

    public boolean addTarget(CallSiteReference site, CGNode node) {
      if (super.addTarget(site, node)) {
        if (((AstCGNode) node).callbacks != null) {
          ((AstCGNode) node).fireCallbacks();
          addAllCallbacks(((AstCGNode) node).callbacks);
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

  protected CGNode makeFakeRootNode() throws CancelException {
    return findOrCreateNode(new AstFakeRoot(FakeRootMethod.rootMethod, cha, options, getAnalysisCache()), Everywhere.EVERYWHERE);
  }

}
