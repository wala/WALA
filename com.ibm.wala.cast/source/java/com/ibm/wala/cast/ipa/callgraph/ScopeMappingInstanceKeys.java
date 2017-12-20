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

import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Pair;

/**
 * An {@link InstanceKeyFactory} that returns {@link ScopeMappingInstanceKey}s
 * as necessary to handle interprocedural lexical scoping (specifically, to
 * handle closure creation when a function escapes its allocating scope)
 */
abstract public class ScopeMappingInstanceKeys implements InstanceKeyFactory {

  /**
   * does base require a scope mapping key? Typically, true if base is allocated
   * in a nested lexical scope, to handle the case of base being a function that
   * performs closure accesses
   */
  protected abstract boolean needsScopeMappingKey(InstanceKey base);

  protected final PropagationCallGraphBuilder builder;

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

    public ScopeMappingInstanceKey(CGNode creator, InstanceKey base) {
      this.creator = creator;
      this.base = base;
    }

    @Override
    public IClass getConcreteType() {
      return base.getConcreteType();
    }

    /**
     * get the CGNode representing the lexical parent of {@link #creator} with
     * name definer
     * 
     */
    public Iterator<CGNode> getFunargNodes(Pair<String, String> name) {
      Collection<CGNode> constructorCallers = getConstructorCallers(this, name);
      assert constructorCallers != null && !constructorCallers.isEmpty() : "no callers for constructor";
      Iterator<CGNode> result = EmptyIterator.instance();
      for (CGNode callerOfConstructor : constructorCallers) {
        if (callerOfConstructor.getMethod().getReference().getDeclaringClass().getName().toString().equals(name.snd)) {
          result = new CompoundIterator<>(result, new NonNullSingletonIterator<>(callerOfConstructor));
        } else {
          PointerKey funcKey = builder.getPointerKeyForLocal(callerOfConstructor, 1);
          for (InstanceKey funcPtr : builder.getPointerAnalysis().getPointsToSet(funcKey)) {
            if (funcPtr instanceof ScopeMappingInstanceKey) {
              result = new CompoundIterator<>(result, ((ScopeMappingInstanceKey) funcPtr).getFunargNodes(name));
            }
          }
        }
      }
      return result;
    }


    @Override
    public int hashCode() {
      return base.hashCode() * creator.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof ScopeMappingInstanceKey) && ((ScopeMappingInstanceKey) o).base.equals(base)
          && ((ScopeMappingInstanceKey) o).creator.equals(creator);
    }

    @Override
    public String toString() {
      return "SMIK:" + base + "@creator:" + creator;
    }

    public InstanceKey getBase() {
      return base;
    }
    
    public CGNode getCreator() {
      return creator;
    }

    @Override
    public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
      return new FilterIterator<>(
          base.getCreationSites(CG),
          o -> o.fst.equals(creator));
    }
  }

  @Override
  public InstanceKey getInstanceKeyForAllocation(CGNode creatorNode, NewSiteReference allocationSite) {
    InstanceKey base = basic.getInstanceKeyForAllocation(creatorNode, allocationSite);
    if (base != null && needsScopeMappingKey(base)) {
      return new ScopeMappingInstanceKey(creatorNode, base);
    } else {
      return base;
    }
  }

  /**
   * get the CGNodes corresponding to the method that invoked the constructor for smik
   */
  protected abstract Collection<CGNode> getConstructorCallers(ScopeMappingInstanceKey smik, Pair<String, String> name);
  
  @Override
  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
    return basic.getInstanceKeyForMultiNewArray(node, allocation, dim);
  }

  @Override
  public InstanceKey getInstanceKeyForConstant(TypeReference type, Object S) {
    return basic.getInstanceKeyForConstant(type, S);
  }

  @Override
  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr, TypeReference type) {
    return basic.getInstanceKeyForPEI(node, instr, type);
  }

  @Override
  public InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType) {
    return basic.getInstanceKeyForMetadataObject(obj, objType);
  }

  public ScopeMappingInstanceKeys(PropagationCallGraphBuilder builder, InstanceKeyFactory basic) {
    this.basic = basic;
    this.builder = builder;
  }
}
