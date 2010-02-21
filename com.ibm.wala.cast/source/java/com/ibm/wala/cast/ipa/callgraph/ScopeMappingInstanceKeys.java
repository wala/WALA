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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.loader.AstMethod.LexicalParent;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * An {@link InstanceKeyFactory} that returns {@link ScopeMappingInstanceKey}s as necessary to handle interprocedural lexical
 * scoping
 */
abstract public class ScopeMappingInstanceKeys implements InstanceKeyFactory {

  /**
   * return all {@link LexicalParent}s of methods that can be invoked on base. (Is this right? --MS)
   */
  protected abstract LexicalParent[] getParents(InstanceKey base);

  protected abstract boolean needsScopeMappingKey(InstanceKey base);

  private final PropagationCallGraphBuilder builder;

  private final InstanceKeyFactory basic;

  public class ScopeMappingInstanceKey implements InstanceKey {
    private final InstanceKey base;

    /**
     * the node in which this is allocated
     */
    private final CGNode creator;

    private final ScopeMap map;

    private class ScopeMap extends HashMap<String, CGNode> {

      private static final long serialVersionUID = 3645910671551712906L;

      private void scan(int level, int toDo, LexicalParent parents[], CGNode node, Set<CGNode> parentNodes) {
        if (toDo > 0) {
          int restoreIndex = -1;
          LexicalParent restoreParent = null;

          if (AstTranslator.DEBUG_LEXICAL)
            System.err.println((level + ": searching " + node + " for parents"));

          for (int i = 0; i < parents.length; i++) {

            if (parents[i] == null)
              continue;

            if (AstTranslator.DEBUG_LEXICAL)
              System.err.println((level + ": searching " + parents[i]));

            if (node.getMethod() == parents[i].getMethod()) {
              if (containsKey(parents[i].getName()))
                assert get(parents[i].getName()) == node;
              else {
                put(parents[i].getName(), node);
                if (AstTranslator.DEBUG_LEXICAL)
                  System.err.println((level + ": Adding lexical parent " + parents[i].getName() + " for " + base + " at " + creator
                      + "(toDo is now " + toDo + ")"));
              }

              toDo--;
              restoreIndex = i;
              restoreParent = parents[i];
              parents[i] = null;
            }
          }

          CallGraph CG = builder.getCallGraph();

          // Assertions._assert(CG.getPredNodes(node).hasNext() || toDo == 0);

          for (Iterator PS = CG.getPredNodes(node); PS.hasNext();) {
            CGNode pred = (CGNode) PS.next();
            if (pred != creator && !parentNodes.contains(pred)) {
              parentNodes.add(pred);
              scan(level + 1, toDo, parents, pred, parentNodes);
              parentNodes.remove(pred);
            }
          }

          if (restoreIndex != -1) {
            parents[restoreIndex] = restoreParent;
          }
        }
      }

      private ScopeMap() {
        LexicalParent[] parents = getParents(base);

        if (AstTranslator.DEBUG_LEXICAL)
          System.err.println(("starting search for parents at " + creator));

        HashSet<CGNode> s = HashSetFactory.make(5);
        scan(0, parents.length, parents, creator, s);
      }

      CGNode getDefiningNode(String definer) {
        return (CGNode) get(definer);
      }
    }

    private ScopeMappingInstanceKey(CGNode creator, InstanceKey base) {
      this.creator = creator;
      this.base = base;
      this.map = new ScopeMap();
    }

    public IClass getConcreteType() {
      return base.getConcreteType();
    }

    CGNode getDefiningNode(String definer) {
      return map.getDefiningNode(definer);
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
