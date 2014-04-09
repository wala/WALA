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

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.loader.AstMethod.LexicalParent;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * An {@link InstanceKeyFactory} that returns {@link ScopeMappingInstanceKey}s
 * as necessary to handle interprocedural lexical scoping
 */
abstract public class ScopeMappingInstanceKeys implements InstanceKeyFactory {

  /**
   * return all {@link LexicalParent}s of methods represented by base (a single
   * method for JavaScript, all instance methods in Java).
   */
  protected abstract LexicalParent[] getParents(InstanceKey base);

  /**
   * does base require a scope mapping key? Typically, true if base is allocated
   * in a nested lexical scope
   */
  protected abstract boolean needsScopeMappingKey(InstanceKey base);

  private final PropagationCallGraphBuilder builder;

  private final InstanceKeyFactory basic;

  /**
   * An {@link InstanceKey} carrying information about which {@link CGNode}s
   * represent lexical parents of the allocating {@link CGNode}.
   * 
   * The fact that we discover at most one {@link CGNode} per lexical parent
   * relies on the following property: in a call graph, the contexts used for a
   * nested function can be finer than those used for the containing function,
   * but _not_ coarser. This ensures that there is at most one CGNode
   * corresponding to a lexical parent (e.g., we don't get two clones of
   * function f1() invoking a single CGNode representing nested function f2())
   * 
   * Note that it is possible to not find a {@link CGNode} corresponding to some
   * lexical parent; this occurs when a deeply nested function is returned
   * before being invoked, so some lexical parent is no longer on the call stack
   * when the function is allocated. See test case nested.js.
   */
  public class ScopeMappingInstanceKey implements InstanceKey {
    /**
     * the underlying instance key
     */
    private final InstanceKey base;

    /**
     * the node in which base is allocated
     */
    private final CGNode creator;

    /**
     * mapping from lexical parent names to the corresponding CGNodes
     */
    private final HashMap<String, CGNode> scopeMap;

    /**
     * compute the {@link CGNode} correspond to each specified
     * {@link LexicalParent} of {@link #base}, populating {@link #scopeMap}
     * 
     */
    private void computeLexicalParentCGNodes() {
      if (AstTranslator.DEBUG_LEXICAL)
        System.err.println(("starting search for parents at " + creator));
      final LexicalParent[] parents = getParents(base);
      Iterator<CGNode> preds = DFS.iterateDiscoverTime(GraphInverter.invert(builder.getCallGraph()), creator);
      int toDo = parents.length;
      while (preds.hasNext()) {
        CGNode pred = preds.next();
        for (int i = 0; i < parents.length; i++) {
          if (parents[i] != null) {
            if (pred.getMethod() == parents[i].getMethod()) {
              if (scopeMap.containsKey(parents[i].getName()))
                assert scopeMap.get(parents[i].getName()) == pred;
              else {
                toDo--;
                scopeMap.put(parents[i].getName(), pred);
                if (AstTranslator.DEBUG_LEXICAL)
                  System.err.println(("Adding lexical parent " + parents[i].getName() + " for " + base + " at " + creator
                      + "(toDo is now " + toDo + ")"));
              }
            }
          }
        }
      }
    }

    private ScopeMappingInstanceKey(CGNode creator, InstanceKey base) {
      this.creator = creator;
      this.base = base;
      this.scopeMap = HashMapFactory.make();
      computeLexicalParentCGNodes();
    }

    public IClass getConcreteType() {
      return base.getConcreteType();
    }

    /**
     * get the CGNode representing the lexical parent of {@link #creator} with name definer
     * @param definer
     * @return
     */
    CGNode getDefiningNode(String definer) {
      return scopeMap.get(definer);
    }

    public int hashCode() {
      return base.hashCode() * creator.hashCode();
    }

    public boolean equals(Object o) {
      return (o instanceof ScopeMappingInstanceKey) && ((ScopeMappingInstanceKey) o).base.equals(base)
          && ((ScopeMappingInstanceKey) o).creator.equals(creator);
    }

    public String toString() {
      return "SMIK:" + base + "@" + creator;
    }
  }

  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    InstanceKey base = basic.getInstanceKeyForAllocation(node, allocation);
    if (base != null && needsScopeMappingKey(base)) {
      return new ScopeMappingInstanceKey(node, base);
    } else {
      return base;
    }
  }

  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
    return basic.getInstanceKeyForMultiNewArray(node, allocation, dim);
  }

  public InstanceKey getInstanceKeyForConstant(TypeReference type, Object S) {
    return basic.getInstanceKeyForConstant(type, S);
  }

  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr, TypeReference type) {
    return basic.getInstanceKeyForPEI(node, instr, type);
  }

  public InstanceKey getInstanceKeyForClassObject(TypeReference type) {
    return basic.getInstanceKeyForClassObject(type);
  }

  public ScopeMappingInstanceKeys(PropagationCallGraphBuilder builder, InstanceKeyFactory basic) {
    this.basic = basic;
    this.builder = builder;
  }
}
