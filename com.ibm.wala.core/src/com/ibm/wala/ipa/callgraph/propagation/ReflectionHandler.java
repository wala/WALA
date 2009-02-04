/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalReturnCallee;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;

/**
 * a helper class which can modify a {@link PropagationCallGraphBuilder} to deal with reflective factory methods.
 * 
 * @author sfink
 */
public class ReflectionHandler {
  private final static boolean VERBOSE = false;

  private final PropagationCallGraphBuilder builder;

  public ReflectionHandler(PropagationCallGraphBuilder builder) {
    this.builder = builder;
  }

  /**
   * update the pointer analysis solver based on flow of reflective factory results to checkcasts
   * 
   * @return true if anything has changed
   * @throws CancelException
   * @throws IllegalArgumentException
   */
  protected boolean updateForReflection() throws IllegalArgumentException, CancelException {

    if (builder.getOptions().getReflectionOptions().isIgnoreFlowToCasts()) {
      return false;
    }
    Collection<Statement> returnStatements = computeFactoryReturnStatements();
    Set<CGNode> changedNodes = HashSetFactory.make();
    for (Statement st : returnStatements) {
      if (VERBOSE) {
        System.err.println("Slice " + st);
      }
      Collection<Statement> slice = Slicer.computeForwardSlice(st, builder.callGraph, null, DataDependenceOptions.REFLECTION,
          ControlDependenceOptions.NONE);
      if (VERBOSE) {
        for (Statement x : slice) {
          System.err.println(" " + x);
        }
      }
      Filter f = new Filter() {
        public boolean accepts(Object o) {
          Statement s = (Statement) o;
          if (s.getKind() == Kind.NORMAL) {
            return ((NormalStatement) s).getInstruction() instanceof SSACheckCastInstruction;
          } else {
            return false;
          }
        }
      };
      Collection<Statement> casts = Iterator2Collection.toCollection(new FilterIterator<Statement>(slice.iterator(), f));
      changedNodes.addAll(modifyFactoryInterpreter(st, casts, builder.getContextInterpreter(), builder.getClassHierarchy()));
    }
    for (Iterator<CGNode> it = changedNodes.iterator(); it.hasNext();) {
      builder.addConstraintsFromChangedNode(it.next());
    }
    return changedNodes.size() > 0;

  }

  private Collection<Statement> computeFactoryReturnStatements() {
    // todo: clean up logic with inheritance, delegation.
    HashSet<Statement> result = HashSetFactory.make();
    for (Iterator it = builder.getCallGraph().iterator(); it.hasNext();) {
      CGNode n = (CGNode) it.next();
      if (n.getMethod() instanceof SyntheticMethod) {
        SyntheticMethod m = (SyntheticMethod) n.getMethod();
        if (m.isFactoryMethod()) {
          result.add(new NormalReturnCallee(n));
        }
      }
    }
    return result;
  }

  /**
   * modify the contextInterpreter to account for new interpretations of factory methods.
   * 
   * @return set of nodes whose interpretation has changed.
   */
  private Set<CGNode> modifyFactoryInterpreter(Statement returnStatement, Collection<Statement> casts,
      RTAContextInterpreter contextInterpreter, IClassHierarchy cha) {
    HashSet<CGNode> result = HashSetFactory.make();

    for (Statement st : casts) {
      SSACheckCastInstruction c = (SSACheckCastInstruction) ((NormalStatement) st).getInstruction();
      TypeReference type = c.getDeclaredResultType();
      IClass klass = cha.lookupClass(type);
      if (klass != null) {
        if (contextInterpreter.recordFactoryType(returnStatement.getNode(), klass)) {
          result.add(returnStatement.getNode());
        }
      }
    }
    return result;
  }
}
